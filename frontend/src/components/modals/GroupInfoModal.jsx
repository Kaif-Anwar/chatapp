import { useState } from 'react';
import { X, Search, UserPlus, UserMinus, Shield } from 'lucide-react';
import { useUiStore } from '../../stores/uiStore';
import { useAuthStore } from '../../stores/authStore';
import { useChatStore } from '../../stores/chatStore';
import { usersApi } from '../../api/users';
import { conversationsApi } from '../../api/conversations';
import Avatar from '../common/Avatar';
import Spinner from '../common/Spinner';

export default function GroupInfoModal() {
  const { modalData: conversation, closeModal } = useUiStore();
  const addToast = useUiStore((s) => s.addToast);
  const currentUser = useAuthStore((s) => s.user);
  const fetchConversations = useChatStore((s) => s.fetchConversations);

  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);

  if (!conversation) return null;

  const participants = conversation.participants || [];
  const isAdmin = participants.find((p) => p.id === currentUser?.id)?.admin;

  const handleSearch = async (q) => {
    setQuery(q);
    if (q.trim().length < 2) { setResults([]); return; }
    setSearching(true);
    try {
      const data = await usersApi.search(q);
      const existingIds = new Set(participants.map((p) => p.id));
      setResults((data || []).filter((u) => !existingIds.has(u.id)));
    } catch { setResults([]); }
    setSearching(false);
  };

  const handleAdd = async (user) => {
    try {
      await conversationsApi.addParticipants(conversation.id, [user.id]);
      await fetchConversations();
      addToast(`${user.username} added to group`, 'success');
      setResults(results.filter((r) => r.id !== user.id));
    } catch (err) {
      addToast(err.message || 'Failed to add user', 'error');
    }
  };

  const handleRemove = async (userId) => {
    if (!confirm('Remove this member from the group?')) return;
    try {
      await conversationsApi.removeParticipant(conversation.id, userId);
      await fetchConversations();
      addToast('Member removed', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to remove member', 'error');
    }
  };

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title">{conversation.name || 'Group Info'}</span>
          <button className="btn-icon" onClick={closeModal}><X size={18} /></button>
        </div>
        <div className="modal-body">
          <div style={{ marginBottom: '16px' }}>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)' }}>
              {participants.length} Members
            </span>
          </div>

          {participants.map((p) => (
            <div key={p.id} className="conv-item">
              <Avatar name={p.displayName || p.username} src={p.avatarUrl} size="md" />
              <div className="conv-item-content">
                <div className="conv-item-name">
                  {p.displayName || p.username}
                  {p.id === currentUser?.id && <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}> (you)</span>}
                </div>
                <div className="conv-item-preview">@{p.username}</div>
              </div>
              {p.admin && <Shield size={14} style={{ color: 'var(--accent-primary)' }} title="Admin" />}
              {isAdmin && p.id !== currentUser?.id && (
                <button className="btn-icon" onClick={() => handleRemove(p.id)} title="Remove">
                  <UserMinus size={16} />
                </button>
              )}
            </div>
          ))}

          {isAdmin && (
            <>
              <div style={{ borderTop: '1px solid var(--border)', margin: '16px 0', paddingTop: '16px' }}>
                <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)' }}>Add Members</span>
              </div>
              <div className="search-wrapper" style={{ marginBottom: '12px' }}>
                <Search size={16} />
                <input className="search-input" placeholder="Search users..." value={query} onChange={(e) => handleSearch(e.target.value)} />
              </div>
              {searching && <div style={{ display: 'flex', justifyContent: 'center', padding: '12px' }}><Spinner size="sm" /></div>}
              {results.map((u) => (
                <div key={u.id} className="conv-item" onClick={() => handleAdd(u)}>
                  <Avatar name={u.username} src={u.avatarUrl} size="md" />
                  <div className="conv-item-content">
                    <div className="conv-item-name">{u.displayName || u.username}</div>
                    <div className="conv-item-preview">@{u.username}</div>
                  </div>
                  <UserPlus size={16} style={{ color: 'var(--success)' }} />
                </div>
              ))}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
