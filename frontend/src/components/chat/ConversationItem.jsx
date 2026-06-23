import Avatar from '../common/Avatar';
import { formatTime } from '../../utils/formatDate';

export default function ConversationItem({ conversation, active, currentUserId, onClick }) {
  const { name, type, participants, lastMessage, unreadCount } = conversation;

  // For private chats, show the other person's name
  let displayName = name;
  let otherUser = null;
  if (type === 'PRIVATE' && participants) {
    const uid = Number(currentUserId);
    otherUser = participants.find((p) => Number(p.id) !== uid);
    displayName = otherUser?.displayName || otherUser?.username || name || 'Chat';
  }

  const preview = typeof lastMessage?.content === 'string' && lastMessage.content
    ? lastMessage.content.length > 40
      ? lastMessage.content.substring(0, 40) + '...'
      : lastMessage.content
    : lastMessage?.attachmentUrl
      ? '📎 Attachment'
      : 'No messages yet';

  return (
    <div className={`conv-item ${active ? 'active' : ''}`} onClick={onClick} id={`conv-${conversation.id}`}>
      <Avatar
        name={displayName}
        src={otherUser?.avatarUrl}
        size="lg"
        online={otherUser?.status === 'ONLINE'}
      />
      <div className="conv-item-content">
        <div className="conv-item-header">
          <span className="conv-item-name">{displayName}</span>
          <span className="conv-item-time">{formatTime(lastMessage?.createdAt)}</span>
        </div>
        <div className="conv-item-preview">{preview}</div>
      </div>
      {unreadCount > 0 && <div className="unread-badge">{unreadCount > 99 ? '99+' : unreadCount}</div>}
    </div>
  );
}
