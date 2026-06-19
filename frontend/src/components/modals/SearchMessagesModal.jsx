import { useState } from 'react';
import { X, Search } from 'lucide-react';
import { useUiStore } from '../../stores/uiStore';
import { messagesApi } from '../../api/messages';
import MessageBubble from '../chat/MessageBubble';
import Spinner from '../common/Spinner';

export default function SearchMessagesModal() {
  const { modalData: conversationId, closeModal } = useUiStore();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);

  if (!conversationId) return null;

  const handleSearch = async (q) => {
    setQuery(q);
    if (q.trim().length < 2) { setResults([]); return; }
    setSearching(true);
    try {
      const data = await messagesApi.search(conversationId, q);
      setResults(data || []);
    } catch { setResults([]); }
    setSearching(false);
  };

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title">Search messages</span>
          <button className="btn-icon" onClick={closeModal}><X size={18} /></button>
        </div>
        <div className="modal-body">
          <div className="search-wrapper" style={{ marginBottom: '16px' }}>
            <Search size={16} />
            <input className="search-input" placeholder="Search..." value={query} onChange={(e) => handleSearch(e.target.value)} autoFocus />
          </div>
          {searching && <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}><Spinner /></div>}
          {results.map((msg) => (
            <div key={msg.id} style={{ marginBottom: '8px' }}>
              <MessageBubble message={msg} isSent={false} showSender />
            </div>
          ))}
          {!searching && query.length >= 2 && results.length === 0 && (
            <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '16px' }}>No messages found</p>
          )}
        </div>
      </div>
    </div>
  );
}
