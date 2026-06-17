import Avatar from '../common/Avatar';
import { formatMessageTime } from '../../utils/formatDate';
import { Pencil, Trash2 } from 'lucide-react';

export default function MessageBubble({ message, isSent, showSender, onEdit, onDelete }) {
  if (!message || typeof message !== 'object' || message.id == null) {
    return null;
  }

  const { content, senderUsername, createdAt, editedAt, deleted } = message;

  if (deleted) {
    return (
      <div className={`message-row ${isSent ? 'sent' : 'received'}`}>
        <div className="message-bubble">
          <p className="message-deleted">🚫 This message was deleted</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`message-row ${isSent ? 'sent' : 'received'}`}>
      {!isSent && <Avatar name={senderUsername} size="sm" />}
      <div className="message-bubble">
        {showSender && !isSent && (
          <div className="message-sender">{senderUsername}</div>
        )}
        <p>{content}</p>
        <div className="message-time">
          {editedAt && <span className="message-edited">edited · </span>}
          {formatMessageTime(createdAt)}
        </div>
        {isSent && (
          <div className="message-actions">
            <button className="message-action-btn" onClick={() => onEdit?.(message)} title="Edit">
              <Pencil size={14} />
            </button>
            <button className="message-action-btn danger" onClick={() => onDelete?.(message)} title="Delete">
              <Trash2 size={14} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
