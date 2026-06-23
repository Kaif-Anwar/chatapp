import { useState, useEffect } from 'react';
import Avatar from '../common/Avatar';
import { formatMessageTime } from '../../utils/formatDate';
import { API_BASE_HOST } from '../../utils/constants';
import { canEditMessage, MESSAGE_EDIT_WINDOW_MS } from '../../utils/messageUtils';
import { parseApiDateMs } from '../../utils/parseApiDate';
import { Pencil, Trash2, Check, CheckCheck } from 'lucide-react';

function resolveUrl(src) {
  if (!src) return null;
  if (src.startsWith('/')) return `${API_BASE_HOST}${src}`;
  return src;
}

function ReadStatus({ status }) {
  if (status === 'READ') return <CheckCheck size={12} style={{ marginLeft: 4 }} />;
  if (status === 'DELIVERED' || status === 'SENT') return <Check size={12} style={{ marginLeft: 4, opacity: 0.7 }} />;
  return null;
}

function useEditWindowOpen(createdAt, isSent) {
  const [editable, setEditable] = useState(() => isSent && canEditMessage(createdAt));

  useEffect(() => {
    if (!isSent || !createdAt) {
      setEditable(false);
      return undefined;
    }

    const refresh = () => setEditable(canEditMessage(createdAt));
    refresh();

    const sentAt = parseApiDateMs(createdAt);
    if (Number.isNaN(sentAt)) {
      setEditable(false);
      return undefined;
    }

    const remaining = MESSAGE_EDIT_WINDOW_MS - (Date.now() - sentAt);
    if (remaining <= 0) {
      setEditable(false);
      return undefined;
    }

    const timer = setTimeout(() => setEditable(false), remaining);
    return () => clearTimeout(timer);
  }, [createdAt, isSent]);

  return editable;
}

export default function MessageBubble({ message, isSent, showSender, onEdit, onDelete }) {
  const createdAt = message?.createdAt;
  const isDeleted = Boolean(message?.deleted);
  const showEdit = useEditWindowOpen(createdAt, isSent && !isDeleted);

  if (!message || typeof message !== 'object' || message.id == null) {
    return null;
  }

  const { content, senderUsername, editedAt, deleted, attachmentUrl, attachmentName, type, status } = message;
  const safeContent = typeof content === 'string' ? content : '';

  if (deleted) {
    return (
      <div className={`message-row ${isSent ? 'sent' : 'received'}`}>
        <div className="message-bubble">
          <p className="message-deleted">🚫 This message was deleted</p>
        </div>
      </div>
    );
  }

  const resolvedAttachment = resolveUrl(attachmentUrl);

  return (
    <div className={`message-row ${isSent ? 'sent' : 'received'}`}>
      {!isSent && <Avatar name={senderUsername} size="sm" />}
      <div className="message-bubble">
        {showSender && !isSent && (
          <div className="message-sender">{senderUsername}</div>
        )}
        {resolvedAttachment && type === 'IMAGE' && (
          <img src={resolvedAttachment} alt={attachmentName || 'attachment'} style={{ maxWidth: '100%', borderRadius: '8px', marginBottom: '6px' }} />
        )}
        {resolvedAttachment && type === 'FILE' && (
          <a href={resolvedAttachment} target="_blank" rel="noopener noreferrer" style={{ display: 'block', marginBottom: '6px', fontSize: '13px' }}>
            📎 {attachmentName || 'Download file'}
          </a>
        )}
        {safeContent && <p>{safeContent}</p>}
        <div className="message-time" style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
          {editedAt && <span className="message-edited">edited · </span>}
          {formatMessageTime(createdAt)}
          {isSent && <ReadStatus status={status} />}
        </div>
        {isSent && (
          <div className="message-actions">
            {showEdit && (
              <button className="message-action-btn" onClick={() => onEdit?.(message)} title="Edit">
                <Pencil size={14} />
              </button>
            )}
            <button className="message-action-btn danger" onClick={() => onDelete?.(message)} title="Delete">
              <Trash2 size={14} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
