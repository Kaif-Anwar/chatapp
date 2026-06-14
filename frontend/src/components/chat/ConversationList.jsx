import ConversationItem from './ConversationItem';
import Spinner from '../common/Spinner';

export default function ConversationList({ conversations, activeId, currentUserId, onSelect, loading }) {
  if (loading) {
    return (
      <div className="conversation-list" style={{ display: 'flex', justifyContent: 'center', padding: '40px 0' }}>
        <Spinner />
      </div>
    );
  }

  if (conversations.length === 0) {
    return (
      <div className="conversation-list" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '40px 20px', color: 'var(--text-muted)', textAlign: 'center' }}>
        <p style={{ fontSize: '14px', fontWeight: 500 }}>No conversations yet</p>
        <p style={{ fontSize: '12px', marginTop: '4px' }}>Start a new chat to begin messaging</p>
      </div>
    );
  }

  return (
    <div className="conversation-list">
      {conversations.map((conv) => (
        <ConversationItem
          key={conv.id}
          conversation={conv}
          active={conv.id === activeId}
          currentUserId={currentUserId}
          onClick={() => onSelect(conv.id)}
        />
      ))}
    </div>
  );
}
