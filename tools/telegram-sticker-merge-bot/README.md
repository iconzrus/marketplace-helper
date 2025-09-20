# Telegram Sticker Merge Bot

Node.js + TypeScript bot using grammy. Creates a new sticker set from selected stickers of multiple existing sets.

## Setup

1. Create environment variable for the bot token (or a `.env` file) with:

```
BOT_TOKEN=123456:ABC...
```

2. Install deps and build:

```
npm i
npm run build
npm start
```

Or run in dev:

```
npm run dev
```

## Быстрый запуск (с вашим BOT_TOKEN)

Внимание: Токен ниже вшит в команды. Хранить его в репозитории/логе не рекомендуется. При необходимости замените токен и удалите его из истории.

Windows PowerShell:

```
cd tools/telegram-sticker-merge-bot; npm i; npm run build; $env:BOT_TOKEN="8212481437:AAHmj4o5-A4TGEFRmaCDLS9TUBNNLfZvLlo"; npm start
```

Linux/macOS:

```
cd tools/telegram-sticker-merge-bot && npm i && npm run build && BOT_TOKEN="8212481437:AAHmj4o5-A4TGEFRmaCDLS9TUBNNLfZvLlo" npm start
```

## Usage flow

- /start — send set names or links (one per line), then /done
- Review paginated list, choose selection mode:
  - Ranges: e.g. `1-5,7,10-12`
  - Emojis: e.g. `:😀,😂`
- Provide title and shortname for the new set
- Bot creates per-format sets if needed (static/animated/video) and splits if exceeding per-set limits
- Returns links: `t.me/addstickers/<shortname>`

## Notes

- Formats are not mixed within a set
- Handles network/API errors and logs without PII (user id only)
- Uses uploadStickerFile + addStickerToSet for each sticker
