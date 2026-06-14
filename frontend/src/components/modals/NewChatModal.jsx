import { useState } from 'react';
import { X, Search } from 'lucide-react';
import { useUiStore } from '../../stores/uiStore';
import { useChatStore } from '../../stores/chatStore';
import { usersApi } from '../../api/users';
import { conversationsApi } from '../../api/conversations';
import Avatar from '../common/Avatar';
import Spinner from '../common/Spinner';

export default function NewChatModal() {
  const closeModal = useUiStore((s) => s.closeModal);
  const addToast = useUiStore((s) => s.addToast);
  const fetchConversations = useChatStore((s) => s.fetchConversations);
  const setActive = useChatStore((s) => s.setActiveConversation);

  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [creating, setCreating] = useState(false);

  const handleSearch = async (q) => {
    setQuery(q);
    if (q.trim().length < 2) { setResults([]); return; }
    setSearching(true);
    try {
      const data = await usersApi.search(q);
      setResults(data || []);
    } catch { setResults([]); }
    setSearching(false);
  };

  const handleSelect = async (user) => {
    setCreating(true);
    try {
      const conv = await conversationsApi.create({
        type: 'PRIVATE',
        participantIds: [user.id],
      });
      await fetchConversations();
      setActive(conv.id);
      closeModal();
      addToast(`Chat started with ${user.username}`, 'success');
    } catch (err) {
      addToast(err.message || 'Failed to create chat', 'error');
    }
    setCreating(false);
  };

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title">New Chat</span>
          <button className="btn-icon" onClick={closeModal}><X size={18} /></button>
        </div>
        <div className="modal-body">
          <div className="search-wrapper" style={{ marginBottom: '16px' }}>
            <Search size={16} />
            <input
              className="search-input"
              placeholder="Search users by username..."
              value={query}
              onChange={(e) => handleSearch(e.target.value)}
              autoFocus
            />
          </div>

          {searching && <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}><Spinner /></div>}

          {results.map((u) => (
            <div key={u.id} className="conv-item" onClick={() => !creating && handleSelect(u)} style={{ opacity: creating ? 0.5 : 1 }}>
              <Avatar name={u.displayName || u.username} src={u.avatarUrl} size="md" />
              <div className="conv-item-content">
                <div className="conv-item-name">{u.displayName || u.username}</div>
                <div className="conv-item-preview">@{u.username}</div>
              </div>
            </div>
          ))}

          {!searching && query.length >= 2 && results.length === 0 && (
            <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '20px', fontSize: '13px' }}>
              No users found
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
