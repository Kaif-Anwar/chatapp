import { useState } from 'react';
import { X, Search, UserPlus, UserMinus } from 'lucide-react';
import { useUiStore } from '../../stores/uiStore';
import { useChatStore } from '../../stores/chatStore';
import { usersApi } from '../../api/users';
import { conversationsApi } from '../../api/conversations';
import Avatar from '../common/Avatar';
import Spinner from '../common/Spinner';

export default function NewGroupModal() {
  const closeModal = useUiStore((s) => s.closeModal);
  const addToast = useUiStore((s) => s.addToast);
  const fetchConversations = useChatStore((s) => s.fetchConversations);
  const setActive = useChatStore((s) => s.setActiveConversation);

  const [name, setName] = useState('');
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [selected, setSelected] = useState([]);
  const [searching, setSearching] = useState(false);
  const [creating, setCreating] = useState(false);

  const handleSearch = async (q) => {
    setQuery(q);
    if (q.trim().length < 2) { setResults([]); return; }
    setSearching(true);
    try {
      const data = await usersApi.search(q);
      setResults((data || []).filter((u) => !selected.find((s) => s.id === u.id)));
    } catch { setResults([]); }
    setSearching(false);
  };

  const addUser = (user) => {
    setSelected([...selected, user]);
    setResults(results.filter((r) => r.id !== user.id));
  };

  const removeUser = (userId) => {
    setSelected(selected.filter((u) => u.id !== userId));
  };

  const handleCreate = async () => {
    if (!name.trim() || selected.length === 0) return;
    setCreating(true);
    try {
      const conv = await conversationsApi.create({
        type: 'GROUP',
        name: name.trim(),
        participantIds: selected.map((u) => u.id),
      });
      await fetchConversations();
      setActive(conv.id);
      closeModal();
      addToast(`Group "${name}" created!`, 'success');
    } catch (err) {
      addToast(err.message || 'Failed to create group', 'error');
    }
    setCreating(false);
  };

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title">New Group</span>
          <button className="btn-icon" onClick={closeModal}><X size={18} /></button>
        </div>
        <div className="modal-body">
          <div className="input-group" style={{ marginBottom: '16px' }}>
            <label className="input-label">Group Name</label>
            <input className="input" placeholder="Enter group name" value={name} onChange={(e) => setName(e.target.value)} />
          </div>

          {selected.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '16px' }}>
              {selected.map((u) => (
                <div key={u.id} style={{
                  display: 'flex', alignItems: 'center', gap: '6px',
                  padding: '4px 10px 4px 4px', background: 'var(--bg-tertiary)',
                  borderRadius: 'var(--radius-full)', fontSize: '13px',
                }}>
                  <Avatar name={u.username} size="sm" />
                  <span>{u.username}</span>
                  <button className="btn-icon" style={{ width: '20px', height: '20px' }} onClick={() => removeUser(u.id)}>
                    <X size={12} />
                  </button>
                </div>
              ))}
            </div>
          )}

          <div className="search-wrapper" style={{ marginBottom: '12px' }}>
            <Search size={16} />
            <input className="search-input" placeholder="Search users to add..." value={query} onChange={(e) => handleSearch(e.target.value)} />
          </div>

          {searching && <div style={{ display: 'flex', justifyContent: 'center', padding: '12px' }}><Spinner size="sm" /></div>}

          {results.map((u) => (
            <div key={u.id} className="conv-item" onClick={() => addUser(u)}>
              <Avatar name={u.username} src={u.avatarUrl} size="md" />
              <div className="conv-item-content">
                <div className="conv-item-name">{u.displayName || u.username}</div>
                <div className="conv-item-preview">@{u.username}</div>
              </div>
              <UserPlus size={16} style={{ color: 'var(--accent-primary)' }} />
            </div>
          ))}
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={closeModal}>Cancel</button>
          <button className="btn btn-primary" onClick={handleCreate} disabled={!name.trim() || selected.length === 0 || creating}>
            {creating ? <Spinner size="sm" /> : 'Create Group'}
          </button>
        </div>
      </div>
    </div>
  );
}
