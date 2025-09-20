import "dotenv/config";
import { Bot, Context, session, SessionFlavor, InlineKeyboard } from "grammy";
import { Api } from "grammy";
import { CreateResultSummary, MAX_STICKERS_PER_SET, SessionData, StickerFormat } from "./types.js";
import { paginate, parseSelection } from "./utils/selection.js";
import { downloadFile, toLite, uploadSticker } from "./utils/stickers.js";

type MyContext = Context & SessionFlavor<SessionData>;

function initial(): SessionData {
  return {
    stage: "idle",
    sourceInputs: [],
    sourceSets: [],
    selectionMode: "ranges",
    selectionQuery: "",
    chosen: [],
    customEmojiIds: [],
  };
}

const token = process.env.BOT_TOKEN;
if (!token) {
  console.error("BOT_TOKEN is not set in environment");
  process.exit(1);
}

const bot = new Bot<MyContext>(token);
bot.use(
  session({
    initial,
  })
);

bot.command("start", async (ctx) => {
  ctx.session = initial();
  await ctx.reply(
    [
      "Привет! Пришли наборы стикеров, например:",
      "- shortname: cats_pack_2024",
      "- ссылка: t.me/addstickers/cats_pack_2024",
      "Или просто вышли по одному стикеру из нужных наборов — я сам определю имя набора.",
      "Если нужно собрать набор ИЗ кастомных эмодзи: присылай сообщения с эмодзи и затем /emoji_done (или начни с /emoji).",
      "Когда закончишь — пришли /done.",
    ].join("\n")
  );
  ctx.session.stage = "awaiting_sets";
});

bot.command("cancel", async (ctx) => {
  ctx.session = initial();
  await ctx.reply("Сброшено. Используй /start чтобы начать заново.");
});

bot.on("message:text", async (ctx) => {
  const text = ctx.message.text.trim();
  if (ctx.session.stage === "awaiting_sets") {
    if (text === "/done" || text === "") {
      if (ctx.session.sourceInputs.length === 0) {
        await ctx.reply("Нужно указать хотя бы один набор.");
        return;
      }
      await loadSourceSets(ctx.api, ctx);
      return;
    }
    const normalized = normalizeSetName(text);
    if (!normalized) {
      await ctx.reply(
        "Это не похоже на имя/ссылку набора. Пришли shortname (например cats_pack_2024), ссылку вида t.me/addstickers/<name> или сам стикер из набора."
      );
      return;
    }
    ctx.session.sourceInputs.push(normalized);
    await ctx.reply(`Добавлено: ${normalized}. Ещё? /done чтобы продолжить.`);
    return;
  }

  if (ctx.session.stage === "awaiting_selection") {
    ctx.session.selectionQuery = text;
    const parsed = parseSelection(text, ctx.session.sourceSets, ctx.session.selectionMode);
    if (parsed.errors.length) {
      await ctx.reply(`Ошибки:\n- ${parsed.errors.join("\n- ")}`);
      return;
    }
    ctx.session.chosen = parsed.chosen;
    const total = parsed.chosen.length;
    await ctx.reply(`Выбрано ${total}. Введи заголовок нового набора.`);
    ctx.session.stage = "confirm_create";
    return;
  }

  if (ctx.session.stage === "awaiting_custom_emoji") {
    const collect = (entities?: any[]) => {
      if (!entities) return [] as string[];
      return entities
        .filter((e) => e.type === "custom_emoji" && e.custom_emoji_id)
        .map((e) => e.custom_emoji_id as string);
    };
    const ids = [
      ...collect((ctx.message as any).entities),
      ...collect((ctx.message as any).caption_entities),
    ];
    if (ids.length === 0) {
      await ctx.reply("Не нашёл кастомных эмодзи в сообщении. Пришли текст с нужными эмодзи или /emoji_done.");
      return;
    }
    const before = ctx.session.customEmojiIds?.length ?? 0;
    ctx.session.customEmojiIds = Array.from(new Set([...(ctx.session.customEmojiIds ?? []), ...ids]));
    const after = ctx.session.customEmojiIds.length;
    await ctx.reply(`Добавлено эмодзи: +${after - before}. Всего: ${after}. Присылай ещё или /emoji_done.`);
    return;
  }

  if (ctx.session.stage === "confirm_create") {
    if (!ctx.session.desiredTitle) {
      ctx.session.desiredTitle = text;
      await ctx.reply("Теперь задай короткое имя (shortname), латиница/нижнее_подчёркивание.");
      return;
    }
    ctx.session.desiredShortName = text;
    if ((ctx.session.customEmojiIds?.length ?? 0) > 0) {
      await createCustomEmojiSets(ctx);
    } else {
      await createSetsAndFill(ctx);
    }
    return;
  }
});

// Allow users to send a sticker from a set to auto-detect set name
bot.on("message:sticker", async (ctx) => {
  if (ctx.session.stage !== "awaiting_sets") return;
  const st: any = ctx.message.sticker as any;
  const setName: string | undefined = st.set_name || st.sticker_set_name;
  if (!setName) {
    await ctx.reply(
      "Этот стикер не содержит публичного имени набора. Пришли ссылку t.me/addstickers/<name> или shortname."
    );
    return;
  }
  ctx.session.sourceInputs.push(setName);
  await ctx.reply(`Добавлено: ${setName}. Ещё? /done чтобы продолжить.`);
});

bot.callbackQuery(/mode:(ranges|emojis)/, async (ctx) => {
  const mode = ctx.match[1] as "ranges" | "emojis";
  ctx.session.selectionMode = mode;
  await ctx.answerCallbackQuery();
  await ctx.editMessageText(
    mode === "ranges"
      ? "Режим выбора: номера и диапазоны (пример: 1-5,7,10-12). Теперь пришли выбор."
      : "Режим выбора: эмодзи (пример: :😀,😂). Теперь пришли выбор."
  );
  ctx.session.stage = "awaiting_selection";
});

bot.callbackQuery(/page:(\d+)/, async (ctx) => {
  const page = Number(ctx.match[1]);
  await renderSetsSummary(ctx, page);
  await ctx.answerCallbackQuery();
});

bot.command("emoji", async (ctx) => {
  ctx.session = initial();
  ctx.session.stage = "awaiting_custom_emoji";
  ctx.session.customEmojiIds = [];
  await ctx.reply("Режим эмодзи: пришли сообщения с нужными кастомными эмодзи, затем /emoji_done.");
});

bot.command("emoji_done", async (ctx) => {
  if (ctx.session.stage !== "awaiting_custom_emoji") {
    await ctx.reply("Сначала запусти /emoji и пришли эмодзи.");
    return;
  }
  const ids = Array.from(new Set(ctx.session.customEmojiIds ?? []));
  if (ids.length === 0) {
    await ctx.reply("Пока нет эмодзи. Пришли текст с кастомными эмодзи и повтори /emoji_done.");
    return;
  }
  await ctx.reply(`Собрано эмодзи: ${ids.length}. Введи заголовок нового набора.`);
  ctx.session.stage = "confirm_create";
});

async function loadSourceSets(api: Api, ctx: MyContext) {
  const inputs = ctx.session.sourceInputs;
  const loaded = [] as SessionData["sourceSets"];
  for (const inp of inputs) {
    // inputs already contain validated shortnames or set names from sticker messages
    const name = inp;
    try {
      const set = await api.getStickerSet(name);
      const stickers = set.stickers.map((s, i) => toLite(s, i));
      loaded.push({ name: set.name, title: set.title, stickers });
    } catch (e: any) {
      await ctx.reply(
        `Не удалось получить набор ${name}: ${e.description ?? e.message}. Убедись, что это корректный shortname или отправь стикер из набора.`
      );
    }
  }
  if (loaded.length === 0) {
    await ctx.reply("Не удалось получить ни одного набора. /start чтобы попробовать снова.");
    ctx.session = initial();
    return;
  }
  ctx.session.sourceSets = loaded;
  ctx.session.stage = "review_sets";
  await renderSetsSummary(ctx, 1);
}

async function renderSetsSummary(ctx: MyContext, page: number) {
  const perPage = 20;
  const flat: Array<{ label: string }> = [];
  for (const set of ctx.session.sourceSets) {
    set.stickers.forEach((s, i) => {
      flat.push({ label: `${set.name} #${i + 1} ${s.emoji ?? ""}`.trim() });
    });
  }
  const { items, pages } = paginate(flat, page, perPage);
  const text = items.map((i, idx) => `${(page - 1) * perPage + idx + 1}. ${i.label}`).join("\n");
  const kb = new InlineKeyboard();
  if (page > 1) kb.text("◀️", `page:${page - 1}`);
  if (page < pages) kb.text("▶️", `page:${page + 1}`);
  kb.row().text("Выбор по номерам", "mode:ranges").text("Выбор по эмодзи", "mode:emojis");
  await ctx.reply(
    `Всего стикеров: ${flat.length}. Страница ${page}/${pages}.\n${text || "(пусто)"}`,
    { reply_markup: kb }
  );
}

function normalizeSetName(input: string): string | null {
  // Accept full https://t.me/addstickers/<name> or t.me/addstickers/<name> or just <name>
  const trimmed = input.trim();
  const urlMatch = trimmed.match(/(?:https?:\/\/)?t\.me\/addstickers\/([A-Za-z0-9_]+)/i);
  if (urlMatch) return urlMatch[1];
  // Plain shortname must be alnum or underscore and at least 3 chars
  if (/^[A-Za-z0-9_]{3,}$/.test(trimmed)) return trimmed;
  return null;
}

async function createSetsAndFill(ctx: MyContext) {
  ctx.session.stage = "creating";
  const userId = ctx.from!.id;
  const title = ctx.session.desiredTitle!;
  const baseShort = ctx.session.desiredShortName!;

  // Group chosen stickers by format
  const refs = ctx.session.chosen;
  const byFormat = new Map<StickerFormat, Array<{ setName: string; index: number }>>();
  for (const ref of refs) {
    const set = ctx.session.sourceSets.find((s) => s.name === ref.sourceSetName)!;
    const st = set.stickers[ref.indexInSource];
    const list = byFormat.get(st.format) ?? [];
    list.push({ setName: set.name, index: ref.indexInSource });
    byFormat.set(st.format, list);
  }

  const results: CreateResultSummary["createdSets"] = [];

  for (const [format, list] of byFormat.entries()) {
    // split by MAX_STICKERS_PER_SET
    for (let chunkIndex = 0; chunkIndex * MAX_STICKERS_PER_SET < list.length; chunkIndex++) {
      const chunk = list.slice(
        chunkIndex * MAX_STICKERS_PER_SET,
        (chunkIndex + 1) * MAX_STICKERS_PER_SET
      );
      const short = chunkIndex === 0 ? baseShort : `${baseShort}_${chunkIndex + 1}`;
      const setTitle = chunkIndex === 0 ? title : `${title} (${chunkIndex + 1})`;

      const summary = { shortName: short, title: setTitle, format, total: chunk.length, added: 0, skipped: [] as Array<{reason: string; index: number}> };

      // Create set with first sticker
      const first = chunk[0];
      let created = false;
      try {
        const firstSet = ctx.session.sourceSets.find((s) => s.name === first.setName)!;
        const firstSticker = firstSet.stickers[first.index];
        const firstBuf = await downloadFile(ctx.api, firstSticker.fileId);
        const firstUploaded = await uploadSticker(ctx.api, userId, firstBuf, format);
        await createSetWithFirst(ctx.api, userId, setTitle, short, firstUploaded, firstSticker.emoji ?? "", format);
        summary.added += 1;
        created = true;
      } catch (e: any) {
        await ctx.reply(
          `Не удалось создать набор ${setTitle} (${short}): ${e.description ?? e.message}`
        );
      }

      if (!created) {
        results.push(summary);
        continue;
      }

      // Add the rest
      for (const item of chunk.slice(1)) {
        try {
          const set = ctx.session.sourceSets.find((s) => s.name === item.setName)!;
          const st = set.stickers[item.index];
          const file = await downloadFile(ctx.api, st.fileId);
          const uploadedId = await uploadSticker(ctx.api, userId, file, format);
          await addStickerByFormat(ctx.api, userId, short, uploadedId, st.emoji ?? "", format);
          summary.added += 1;
        } catch (e: any) {
          summary.skipped.push({ reason: e.description ?? e.message ?? "error", index: item.index });
        }
      }
      results.push(summary);
    }
  }

  // Prepare final message(s)
  const lines: string[] = [];
  for (const r of results) {
    const url = `t.me/addstickers/${r.shortName}`;
    const skipped = r.skipped.length ? `, пропущено ${r.skipped.length}` : "";
    lines.push(`${r.title} [${r.format}] — добавлено ${r.added}/${r.total}${skipped}\n${url}`);
  }
  if (lines.length === 0) {
    await ctx.reply("Ничего не создано.");
  } else {
    await ctx.reply(lines.join("\n\n"));
  }
  ctx.session = initial();
}

async function createSetWithFirst(
  api: Api,
  userId: number,
  title: string,
  shortName: string,
  uploadedFileId: string,
  emoji: string,
  format: StickerFormat
) {
  const sticker_format =
    format === "static" ? "static" : format === "animated" ? "animated" : "video";
  const inputSticker: any = { emoji_list: [emoji] };
  if (format === "static") inputSticker.png_sticker = uploadedFileId;
  else if (format === "animated") inputSticker.tgs_sticker = uploadedFileId;
  else inputSticker.webm_sticker = uploadedFileId;
  // Newer Bot API expects stickers array; grammy supports both forms via extra
  await api.createNewStickerSet(userId, shortName, title, {
    sticker_format,
    stickers: [inputSticker],
  } as any);
}

async function addStickerByFormat(
  api: Api,
  userId: number,
  shortName: string,
  fileId: string,
  emoji: string,
  format: StickerFormat
) {
  const inputSticker: any = { emoji_list: [emoji] };
  if (format === "static") inputSticker.png_sticker = fileId;
  else if (format === "animated") inputSticker.tgs_sticker = fileId;
  else inputSticker.webm_sticker = fileId;
  await api.addStickerToSet(userId, shortName, inputSticker);
}

async function createCustomEmojiSets(ctx: MyContext) {
  // Create custom emoji set(s) from collected customEmojiIds
  ctx.session.stage = "creating";
  const userId = ctx.from!.id;
  const title = ctx.session.desiredTitle!;
  const baseShort = ctx.session.desiredShortName!;
  const ids = Array.from(new Set(ctx.session.customEmojiIds ?? []));

  // Fetch Sticker objects for custom emoji ids
  const stickersResp = await ctx.api.getCustomEmojiStickers(ids);
  const stickers = stickersResp ?? [];

  // Telegram requires sticker_type = custom_emoji and sticker_format must match source
  // We'll split by format and by MAX_STICKERS_PER_SET
  type Item = { fileId: string; emoji?: string; format: StickerFormat };
  const items: Item[] = stickers.map((s) => ({
    fileId: s.file_id,
    emoji: s.emoji ?? "",
    format: s.is_animated ? "animated" : s.is_video ? "video" : "static",
  }));

  const byFormat = new Map<StickerFormat, Item[]>();
  for (const it of items) {
    const list = byFormat.get(it.format) ?? [];
    list.push(it);
    byFormat.set(it.format, list);
  }

  const results: string[] = [];
  for (const [format, list] of byFormat.entries()) {
    for (let chunkIndex = 0; chunkIndex * MAX_STICKERS_PER_SET < list.length; chunkIndex++) {
      const chunk = list.slice(
        chunkIndex * MAX_STICKERS_PER_SET,
        (chunkIndex + 1) * MAX_STICKERS_PER_SET
      );
      const short = chunkIndex === 0 ? baseShort : `${baseShort}_${chunkIndex + 1}`;
      const setTitle = chunkIndex === 0 ? title : `${title} (${chunkIndex + 1})`;

      // Create set with first sticker (sticker_type=custom_emoji)
      const first = chunk[0];
      const sticker_format = format === "static" ? "static" : format === "animated" ? "animated" : "video";
      const firstInput: any = { emoji_list: [first.emoji ?? "" ] };
      if (format === "static") firstInput.png_sticker = first.fileId;
      else if (format === "animated") firstInput.tgs_sticker = first.fileId;
      else firstInput.webm_sticker = first.fileId;

      try {
        await ctx.api.createNewStickerSet(userId, short, setTitle, {
          sticker_format,
          sticker_type: "custom_emoji",
          stickers: [firstInput],
        } as any);
      } catch (e: any) {
        await ctx.reply(`Не удалось создать набор ${setTitle}: ${e.description ?? e.message}`);
        continue;
      }

      // Add the rest
      let added = 1;
      for (const it of chunk.slice(1)) {
        const input: any = { emoji_list: [it.emoji ?? ""] };
        if (format === "static") input.png_sticker = it.fileId;
        else if (format === "animated") input.tgs_sticker = it.fileId;
        else input.webm_sticker = it.fileId;
        try {
          await ctx.api.addStickerToSet(userId, short, input);
          added += 1;
        } catch (e: any) {
          // continue, but record error
        }
      }
      results.push(`Создано: ${setTitle} (${short}) — добавлено ${added}/${chunk.length}\nt.me/addstickers/${short}`);
    }
  }

  if (results.length === 0) {
    await ctx.reply("Ничего не создано.");
  } else {
    await ctx.reply(results.join("\n\n"));
  }
  ctx.session = initial();
}

if (process.env.NODE_ENV !== "test") {
  bot.start();
  // eslint-disable-next-line no-console
  console.log("Sticker merge bot started");
}


