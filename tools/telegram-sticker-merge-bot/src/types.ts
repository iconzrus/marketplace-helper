export type StickerFormat = "static" | "animated" | "video";

export type UserSelectionMode = "ranges" | "emojis";

export interface SourceSet {
  name: string; // short name or link
  title?: string;
  stickers: TelegramStickerLite[];
}

export interface TelegramStickerLite {
  fileId: string;
  emoji?: string;
  format: StickerFormat;
  // Optional: for ordering in the source set
  index?: number;
}

export interface ChosenStickerRef {
  sourceSetName: string;
  indexInSource: number; // 0-based index in the displayed list
}

export interface CreateResultSummary {
  createdSets: Array<{
    shortName: string;
    title: string;
    format: StickerFormat;
    total: number;
    added: number;
    skipped: Array<{ reason: string; index: number }>;
  }>;
}

export interface SessionData {
  stage:
    | "idle"
    | "awaiting_sets"
    | "awaiting_custom_emoji"
    | "review_sets"
    | "awaiting_selection"
    | "confirm_create"
    | "creating";
  sourceInputs: string[]; // user inputs (names/links)
  sourceSets: SourceSet[];
  selectionMode: UserSelectionMode;
  selectionQuery: string; // raw user input describing selection
  chosen: ChosenStickerRef[]; // resolved selection
  desiredTitle?: string;
  desiredShortName?: string;
  customEmojiIds?: string[]; // collected custom emoji ids from messages
  emojiCollectMode?: "items" | "full_sets";
}

export const MAX_STICKERS_PER_SET = 120; // conservative default; API may allow more for video


