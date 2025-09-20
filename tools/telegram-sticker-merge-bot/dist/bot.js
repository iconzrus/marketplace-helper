import "dotenv/config";
import { Bot, session, InlineKeyboard } from "grammy";
import { MAX_STICKERS_PER_SET } from "./types.js";
import { paginate, parseSelection } from "./utils/selection.js";
import { downloadFile, toLite, uploadSticker } from "./utils/stickers.js";
function initial() {
    return {
        stage: "idle",
        sourceInputs: [],
        sourceSets: [],
        selectionMode: "ranges",
        selectionQuery: "",
        chosen: [],
        customEmojiIds: [],
        emojiCollectMode: "items",
    };
}
const token = process.env.BOT_TOKEN;
if (!token) {
    console.error("BOT_TOKEN is not set in environment");
    process.exit(1);
}
const bot = new Bot(token);
bot.use(session({
    initial,
}));
let cachedBotUsername = null;
async function getBotUsername(api) {
    if (cachedBotUsername)
        return cachedBotUsername;
    const me = await api.getMe();
    cachedBotUsername = me.username ?? "unknown_bot";
    return cachedBotUsername;
}
function sanitizeShortNameInput(input) {
    const trimmed = input.trim();
    const cleaned = trimmed.replace(/[^A-Za-z0-9_]/g, "_");
    return cleaned.slice(0, 64);
}
async function generateShortNameForChunk(api, userInput, chunkIndex) {
    const username = (await getBotUsername(api)).toLowerCase();
    const suffix = `_by_${username}`;
    let base = sanitizeShortNameInput(userInput);
    // remove existing suffix if present (case-insensitive)
    if (base.toLowerCase().endsWith(suffix)) {
        base = base.slice(0, base.length - suffix.length);
    }
    // add numeric index before suffix when needed
    const withIndex = chunkIndex === 0 ? base : `${base}_${chunkIndex + 1}`;
    // ensure total length <= 64
    const maxBaseLen = 64 - suffix.length;
    const truncated = withIndex.slice(0, Math.max(0, maxBaseLen));
    return `${truncated}${suffix}`;
}
bot.command("start", async (ctx) => {
    ctx.session = initial();
    await ctx.reply([
        "Привет! Пришли наборы стикеров, например:",
        "- shortname: cats_pack_2024",
        "- ссылка: t.me/addstickers/cats_pack_2024",
        "Или просто вышли по одному стикеру из нужных наборов — я сам определю имя набора.",
        "Если нужно собрать набор ИЗ кастомных эмодзи: присылай сообщения с эмодзи и затем /emoji_done (или начни с /emoji).",
        "Когда закончишь — пришли /done.",
        "Подсказка: суффикс с именем бота к shortname добавлю автоматически, писать его не нужно.",
        "Нужно собрать ВСЕ эмодзи из наборов, откуда ты прислал эмодзи? Используй /emoji_full. Только присланные — /emoji_items.",
    ].join("\n"));
    ctx.session.stage = "awaiting_sets";
});
bot.command("cancel", async (ctx) => {
    ctx.session = initial();
    await ctx.reply("Сброшено. Используй /start чтобы начать заново.");
});
bot.on("message:text", async (ctx, next) => {
    const text = ctx.message.text.trim();
    if (text.startsWith("/") && text !== "/done") {
        return await next();
    }
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
            await ctx.reply("Это не похоже на имя/ссылку набора. Пришли shortname (например cats_pack_2024), ссылку вида t.me/addstickers/<name> или сам стикер из набора.");
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
        const collect = (entities) => {
            if (!entities)
                return [];
            return entities
                .filter((e) => e.type === "custom_emoji" && e.custom_emoji_id)
                .map((e) => e.custom_emoji_id);
        };
        const ids = [
            ...collect(ctx.message.entities),
            ...collect(ctx.message.caption_entities),
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
            await ctx.reply("Теперь задай короткое имя (shortname). Я добавлю нужный суффикс автоматически.");
            return;
        }
        ctx.session.desiredShortName = text;
        if ((ctx.session.customEmojiIds?.length ?? 0) > 0) {
            await createCustomEmojiSets(ctx);
        }
        else {
            await createSetsAndFill(ctx);
        }
        return;
    }
});
bot.on("message:sticker", async (ctx) => {
    if (ctx.session.stage !== "awaiting_sets")
        return;
    const st = ctx.message.sticker;
    const setName = st.set_name || st.sticker_set_name;
    if (!setName) {
        await ctx.reply("Этот стикер не содержит публичного имени набора. Пришли ссылку t.me/addstickers/<name> или shortname.");
        return;
    }
    ctx.session.sourceInputs.push(setName);
    await ctx.reply(`Добавлено: ${setName}. Ещё? /done чтобы продолжить.`);
});
bot.callbackQuery(/mode:(ranges|emojis)/, async (ctx) => {
    const mode = ctx.match[1];
    ctx.session.selectionMode = mode;
    await ctx.answerCallbackQuery();
    await ctx.editMessageText(mode === "ranges"
        ? "Режим выбора: номера и диапазоны (пример: 1-5,7,10-12). Теперь пришли выбор."
        : "Режим выбора: эмодзи (пример: :😀,😂). Теперь пришли выбор.");
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
    ctx.session.emojiCollectMode = "items";
    await ctx.reply("Режим эмодзи (только присланные): пришли сообщения с нужными кастомными эмодзи, затем /emoji_done. Для полного набора используй /emoji_full.");
});
bot.command("emoji_full", async (ctx) => {
    ctx.session = initial();
    ctx.session.stage = "awaiting_custom_emoji";
    ctx.session.customEmojiIds = [];
    ctx.session.emojiCollectMode = "full_sets";
    await ctx.reply("Режим эмодзи (полные наборы): пришли сообщения с эмодзи, затем /emoji_done. Будут добавлены ВСЕ эмодзи из их наборов.");
});
bot.command("emoji_items", async (ctx) => {
    ctx.session = initial();
    ctx.session.stage = "awaiting_custom_emoji";
    ctx.session.customEmojiIds = [];
    ctx.session.emojiCollectMode = "items";
    await ctx.reply("Режим эмодзи (только присланные): пришли сообщения с нужными кастомными эмодзи, затем /emoji_done.");
});
bot.command("emoji_done", async (ctx) => {
    if (ctx.session.stage !== "awaiting_custom_emoji") {
        await ctx.reply("Сначала запусти /emoji или /emoji_full и пришли эмодзи.");
        return;
    }
    const ids = Array.from(new Set(ctx.session.customEmojiIds ?? []));
    if (ids.length === 0) {
        await ctx.reply("Пока нет эмодзи. Пришли текст с кастомными эмоджи и повтори /emoji_done.");
        return;
    }
    await ctx.reply(`Собрано эмодзи: ${ids.length}. Введи заголовок нового набора.`);
    ctx.session.stage = "confirm_create";
});
async function loadSourceSets(api, ctx) {
    const inputs = ctx.session.sourceInputs;
    const loaded = [];
    for (const inp of inputs) {
        const name = inp;
        try {
            const set = await api.getStickerSet(name);
            const stickers = set.stickers.map((s, i) => toLite(s, i));
            loaded.push({ name: set.name, title: set.title, stickers });
        }
        catch (e) {
            await ctx.reply(`Не удалось получить набор ${name}: ${e.description ?? e.message}. Убедись, что это корректный shortname или отправь стикер из набора.`);
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
async function renderSetsSummary(ctx, page) {
    const perPage = 20;
    const flat = [];
    for (const set of ctx.session.sourceSets) {
        set.stickers.forEach((s, i) => {
            flat.push({ label: `${set.name} #${i + 1} ${s.emoji ?? ""}`.trim() });
        });
    }
    const { items, pages } = paginate(flat, page, perPage);
    const text = items.map((i, idx) => `${(page - 1) * perPage + idx + 1}. ${i.label}`).join("\n");
    const kb = new InlineKeyboard();
    if (page > 1)
        kb.text("◀️", `page:${page - 1}`);
    if (page < pages)
        kb.text("▶️", `page:${page + 1}`);
    kb.row().text("Выбор по номерам", "mode:ranges").text("Выбор по эмодзи", "mode:emojis");
    await ctx.reply(`Всего стикеров: ${flat.length}. Страница ${page}/${pages}.\n${text || "(пусто)"}`, { reply_markup: kb });
}
function normalizeSetName(input) {
    const trimmed = input.trim();
    const urlMatch = trimmed.match(/(?:https?:\/\/)?t\.me\/addstickers\/([A-Za-z0-9_]+)/i);
    if (urlMatch)
        return urlMatch[1];
    if (/^[A-Za-z0-9_]{3,}$/.test(trimmed))
        return trimmed;
    return null;
}
async function createSetsAndFill(ctx) {
    ctx.session.stage = "creating";
    const userId = ctx.from.id;
    const title = ctx.session.desiredTitle;
    const baseShortInput = ctx.session.desiredShortName;
    const refs = ctx.session.chosen;
    const byFormat = new Map();
    for (const ref of refs) {
        const set = ctx.session.sourceSets.find((s) => s.name === ref.sourceSetName);
        const st = set.stickers[ref.indexInSource];
        const list = byFormat.get(st.format) ?? [];
        list.push({ setName: set.name, index: ref.indexInSource });
        byFormat.set(st.format, list);
    }
    const results = [];
    for (const [format, list] of byFormat.entries()) {
        for (let chunkIndex = 0; chunkIndex * MAX_STICKERS_PER_SET < list.length; chunkIndex++) {
            const chunk = list.slice(chunkIndex * MAX_STICKERS_PER_SET, (chunkIndex + 1) * MAX_STICKERS_PER_SET);
            const short = await generateShortNameForChunk(ctx.api, baseShortInput, chunkIndex);
            const setTitle = chunkIndex === 0 ? title : `${title} (${chunkIndex + 1})`;
            const summary = { shortName: short, title: setTitle, format, total: chunk.length, added: 0, skipped: [] };
            const first = chunk[0];
            let created = false;
            try {
                const firstSet = ctx.session.sourceSets.find((s) => s.name === first.setName);
                const firstSticker = firstSet.stickers[first.index];
                const firstBuf = await downloadFile(ctx.api, firstSticker.fileId);
                const firstUploaded = await uploadSticker(ctx.api, userId, firstBuf, format);
                const inputSticker = { emoji_list: [firstSticker.emoji ?? ""], sticker: firstUploaded };
                const sticker_format = format === "static" ? "static" : format === "animated" ? "animated" : "video";
                await ctx.api.createNewStickerSet(userId, short, setTitle, [inputSticker], { sticker_format });
                summary.added += 1;
                created = true;
            }
            catch (e) {
                await ctx.reply(`Не удалось создать набор ${setTitle} (${short}): ${e.description ?? e.message}`);
            }
            if (!created) {
                results.push(summary);
                continue;
            }
            for (const item of chunk.slice(1)) {
                try {
                    const set = ctx.session.sourceSets.find((s) => s.name === item.setName);
                    const st = set.stickers[item.index];
                    const file = await downloadFile(ctx.api, st.fileId);
                    const uploadedId = await uploadSticker(ctx.api, userId, file, format);
                    const inputSticker = { emoji_list: [st.emoji ?? ""], sticker: uploadedId };
                    await ctx.api.addStickerToSet(userId, short, inputSticker);
                    summary.added += 1;
                }
                catch (e) {
                    summary.skipped.push({ reason: e.description ?? e.message ?? "error", index: item.index });
                }
            }
            results.push(summary);
        }
    }
    const lines = [];
    for (const r of results) {
        const url = `t.me/addstickers/${r.shortName}`;
        const skipped = r.skipped.length ? `, пропущено ${r.skipped.length}` : "";
        lines.push(`${r.title} [${r.format}] — добавлено ${r.added}/${r.total}${skipped}\n${url}`);
    }
    if (lines.length === 0) {
        await ctx.reply("Ничего не создано.");
    }
    else {
        await ctx.reply(lines.join("\n\n"));
    }
    ctx.session = initial();
}
async function createCustomEmojiSets(ctx) {
    ctx.session.stage = "creating";
    const userId = ctx.from.id;
    const title = ctx.session.desiredTitle;
    const baseShortInput = ctx.session.desiredShortName;
    const ids = Array.from(new Set(ctx.session.customEmojiIds ?? []));
    const stickersResp = await ctx.api.getCustomEmojiStickers(ids);
    const stickers = stickersResp ?? [];
    let expanded = stickers;
    if (ctx.session.emojiCollectMode === "full_sets") {
        const setNames = Array.from(new Set(expanded.map((s) => s.set_name || s.sticker_set_name).filter(Boolean)));
        const full = [];
        for (const name of setNames) {
            try {
                const set = await ctx.api.getStickerSet(name);
                full.push(...set.stickers);
            }
            catch { }
        }
        expanded = full.length ? full : expanded;
    }
    const items = expanded.map((s) => ({
        fileId: s.file_id,
        emoji: s.emoji ?? "",
        format: s.is_animated ? "animated" : s.is_video ? "video" : "static",
    }));
    const seen = new Set();
    const deduped = [];
    for (const it of items) {
        if (!seen.has(it.fileId)) {
            seen.add(it.fileId);
            deduped.push(it);
        }
    }
    const byFormat = new Map();
    for (const it of deduped) {
        const list = byFormat.get(it.format) ?? [];
        list.push(it);
        byFormat.set(it.format, list);
    }
    const results = [];
    for (const [format, list] of byFormat.entries()) {
        for (let chunkIndex = 0; chunkIndex * MAX_STICKERS_PER_SET < list.length; chunkIndex++) {
            const chunk = list.slice(chunkIndex * MAX_STICKERS_PER_SET, (chunkIndex + 1) * MAX_STICKERS_PER_SET);
            const short = await generateShortNameForChunk(ctx.api, baseShortInput, chunkIndex);
            const setTitle = chunkIndex === 0 ? title : `${title} (${chunkIndex + 1})`;
            const first = chunk[0];
            const sticker_format = format === "static" ? "static" : format === "animated" ? "animated" : "video";
            try {
                const firstBuf = await downloadFile(ctx.api, first.fileId);
                const firstUploaded = await uploadSticker(ctx.api, userId, firstBuf, format);
                const firstInput = { emoji_list: [first.emoji ?? ""], sticker: firstUploaded };
                await ctx.api.createNewStickerSet(userId, short, setTitle, [firstInput], { sticker_format, sticker_type: "custom_emoji" });
            }
            catch (e) {
                await ctx.reply(`Не удалось создать набор ${setTitle}: ${e.description ?? e.message}`);
                continue;
            }
            let added = 1;
            let skipped = 0;
            for (const it of chunk.slice(1)) {
                try {
                    const buf = await downloadFile(ctx.api, it.fileId);
                    const uploaded = await uploadSticker(ctx.api, userId, buf, format);
                    const input = { emoji_list: [it.emoji ?? ""], sticker: uploaded };
                    await ctx.api.addStickerToSet(userId, short, input);
                    added += 1;
                }
                catch (e) {
                    skipped += 1;
                }
            }
            results.push(`Создано: ${setTitle} (${short}) — добавлено ${added}/${chunk.length}${skipped ? ", пропущено " + skipped : ""}\nt.me/addstickers/${short}`);
        }
    }
    if (results.length === 0) {
        await ctx.reply("Ничего не создано.");
    }
    else {
        await ctx.reply(results.join("\n\n"));
    }
    ctx.session = initial();
}
if (process.env.NODE_ENV !== "test") {
    bot.start();
    console.log("Sticker merge bot started");
}
