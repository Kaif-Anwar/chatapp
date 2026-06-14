import { useState, useMemo } from 'react';
import { Search, MessageSquarePlus, Users } from 'lucide-react';
import ConversationList from './ConversationList';
import UserProfileStrip from './UserProfileStrip';
import NotificationPanel from '../notifications/NotificationPanel';
import { useChatStore } from '../../stores/chatStore';
import { useAuthStore } from '../../stores/authStore';
import { useUiStore } from '../../stores/uiStore';

export default function Sidebar() {
  const [searchQuery, setSearchQuery] = useState('');
  const conversations = useChatStore((s) => s.conversations);
  const activeId = useChatStore((s) => s.activeConversationId);
  const setActive = useChatStore((s) => s.setActiveConversation);
  const loading = useChatStore((s) => s.loadingConversations);
  const user = useAuthStore((s) => s.user);
  const openModal = useUiStore((s) => s.openModal);
  const sidebarOpen = useUiStore((s) => s.sidebarOpen);
  const setSidebarOpen = useUiStore((s) => s.setSidebarOpen);

  const filtered = useMemo(() => {
    if (!searchQuery.trim()) return conversations;
    const q = searchQuery.toLowerCase();
    return conversations.filter((c) => {
      const name = c.name || '';
      const participants = (c.participants || []).map((p) => p.username || p.displayName || '').join(' ');
      return name.toLowerCase().includes(q) || participants.toLowerCase().includes(q);
    });
  }, [conversations, searchQuery]);

  const handleSelect = (id) => {
    setActive(id);
    // Close sidebar on mobile
    if (window.innerWidth <= 768) setSidebarOpen(false);
  };

  return (
    <>
      {/* Mobile overlay */}
      {sidebarOpen && <div className="mobile-overlay" onClick={() => setSidebarOpen(false)} />}

      <div className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <div className="sidebar-title"><span>💬 ChatApp</span></div>
          <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
            <NotificationPanel />
            <button className="btn-icon" onClick={() => openModal('newChat')} title="New Chat">
              <MessageSquarePlus size={18} />
            </button>
            <button className="btn-icon" onClick={() => openModal('newGroup')} title="New Group">
              <Users size={18} />
            </button>
          </div>
        </div>

        <div className="sidebar-search">
          <div className="search-wrapper">
            <Search size={16} />
            <input
              className="search-input"
              placeholder="Search conversations..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        <ConversationList
          conversations={filtered}
          activeId={activeId}
          currentUserId={user?.id}
          onSelect={handleSelect}
          loading={loading}
        />

        <UserProfileStrip />
      </div>
    </>
  );
}

