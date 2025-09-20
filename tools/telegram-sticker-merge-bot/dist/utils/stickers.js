import { InputFile } from "grammy";
export function mapStickerFormat(s) {
    if (s.is_animated)
        return "animated";
    if (s.is_video)
        return "video";
    return "static";
}
export function toLite(st, index) {
    return {
        fileId: st.file_id,
        emoji: st.emoji ?? undefined,
        format: mapStickerFormat(st),
        index,
    };
}
export async function downloadFile(api, fileId) {
    const file = await api.getFile(fileId);
    const url = `https://api.telegram.org/file/bot${api.token}/${file.file_path}`;
    const res = await fetch(url);
    if (!res.ok)
        throw new Error(`Failed to download file: ${res.status}`);
    const array = await res.arrayBuffer();
    return Buffer.from(array);
}
export async function uploadSticker(api, userId, data, format) {
    const sticker_format = format === "static" ? "static" : format === "animated" ? "animated" : "video";
    const uploaded = await api.uploadStickerFile(userId, sticker_format, new InputFile(data));
    return uploaded.file_id;
}
