import { MessageSquare } from 'lucide-react';

export default function EmptyState() {
  return (
    <div className="empty-state">
      <MessageSquare size={80} className="empty-state-icon" />
      <h2 className="empty-state-title">Select a conversation</h2>
      <p className="empty-state-text">
        Choose a conversation from the sidebar or start a new one to begin chatting
      </p>
    </div>
  );
}
