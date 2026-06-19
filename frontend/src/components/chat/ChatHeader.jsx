import Avatar from '../common/Avatar';
import { Phone, Video, Info, Menu, Search } from 'lucide-react';
import { useUiStore } from '../../stores/uiStore';

export default function ChatHeader({ conversation, currentUserId, typingText }) {
  const toggleSidebar = useUiStore((s) => s.toggleSidebar);
  const openModal = useUiStore((s) => s.openModal);

  if (!conversation) return null;

  const { name, type, participants } = conversation;
  let displayName = name;
  let statusText = '';
  let otherUser = null;

  if (type === 'PRIVATE' && participants) {
    otherUser = participants.find((p) => p.id !== currentUserId);
    displayName = otherUser?.displayName || otherUser?.username || name || 'Chat';
    statusText = otherUser?.status === 'ONLINE' ? 'Online' : 'Offline';
  } else {
    statusText = `${(participants || []).length} members`;
  }

  return (
    <div className="chat-header">
      <button className="btn-icon mobile-menu-btn" onClick={toggleSidebar} aria-label="Open conversations">
        <Menu size={20} />
      </button>
      <Avatar
        name={displayName}
        src={otherUser?.avatarUrl}
        size="md"
        online={otherUser?.status === 'ONLINE'}
      />
      <div className="chat-header-info">
        <div className="chat-header-name">{displayName}</div>
        <div className={`chat-header-status ${otherUser?.status === 'ONLINE' ? 'online' : ''}`}>
          {typingText || statusText}
        </div>
      </div>
      {type === 'GROUP' && (
        <button className="btn-icon" onClick={() => openModal('groupInfo', conversation)} title="Group Info">
          <Info size={18} />
        </button>
      )}
      <button className="btn-icon" onClick={() => openModal('searchMessages', conversation.id)} title="Search messages">
        <Search size={18} />
      </button>
    </div>
  );
}
