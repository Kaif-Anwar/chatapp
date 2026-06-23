/**
 * Normalize API/WebSocket message payloads before they enter the store or UI.
 * Prevents render crashes from null content, wrong types, or missing fields.
 */

export const MESSAGE_EDIT_WINDOW_MS = 30_000;

export function canEditMessage(createdAt, now = Date.now()) {
  if (!createdAt) return false;
  const sentAt = new Date(createdAt).getTime();
  if (Number.isNaN(sentAt)) return false;
  return now - sentAt <= MESSAGE_EDIT_WINDOW_MS;
}
export function normalizeMessage(message) {
  if (!message || typeof message !== 'object' || message.id == null) {
    return null;
  }

  return {
    ...message,
    id: message.id,
    conversationId: message.conversationId != null ? Number(message.conversationId) : undefined,
    senderId: message.senderId != null ? Number(message.senderId) : null,
    senderUsername: typeof message.senderUsername === 'string' ? message.senderUsername : 'Unknown',
    content: typeof message.content === 'string' ? message.content : '',
    type: message.type ?? 'TEXT',
    status: message.status ?? 'SENT',
    createdAt: message.createdAt ?? null,
    editedAt: message.editedAt ?? null,
    deleted: Boolean(message.deleted),
    attachmentUrl: message.attachmentUrl ?? null,
    attachmentName: message.attachmentName ?? null,
  };
}

export function isValidMessage(message) {
  return normalizeMessage(message) != null;
}
