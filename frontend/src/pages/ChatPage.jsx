import { useEffect } from 'react';
import Sidebar from '../components/chat/Sidebar';
import ChatHeader from '../components/chat/ChatHeader';
import MessageArea from '../components/chat/MessageArea';
import MessageInput from '../components/chat/MessageInput';
import EmptyState from '../components/chat/EmptyState';
import { useChatStore } from '../stores/chatStore';
import { useAuthStore } from '../stores/authStore';
import { useNotificationStore } from '../stores/notificationStore';
import { useWebSocket } from '../hooks/useWebSocket';
import { usePresence } from '../hooks/usePresence';

export default function ChatPage() {
  const user = useAuthStore((s) => s.user);
  const fetchConversations = useChatStore((s) => s.fetchConversations);
  const activeId = useChatStore((s) => s.activeConversationId);
  const getActive = useChatStore((s) => s.getActiveConversation);
  const sendMessage = useChatStore((s) => s.sendMessage);
  const fetchUnreadCount = useNotificationStore((s) => s.fetchUnreadCount);

  const activeConversation = getActive();

  // Connect WebSocket and start presence heartbeat
  const stompRef = useWebSocket(user);
  usePresence(stompRef);

  useEffect(() => {
    fetchConversations();
    fetchUnreadCount();
  }, [fetchConversations, fetchUnreadCount]);

  const handleSend = async (content) => {
    if (activeId) {
      await sendMessage(activeId, content);
    }
  };

  return (
    <div className="chat-layout">
      <Sidebar />

      {activeConversation ? (
        <div className="message-area">
          <ChatHeader conversation={activeConversation} currentUserId={user?.id} />
          <MessageArea conversationId={activeId} currentUserId={user?.id} />
          <MessageInput onSend={handleSend} />
        </div>
      ) : (
        <div className="message-area">
          <EmptyState />
        </div>
      )}
    </div>
  );
}
