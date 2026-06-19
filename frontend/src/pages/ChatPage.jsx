import { useEffect, useCallback } from 'react';
import Sidebar from '../components/chat/Sidebar';
import ChatHeader from '../components/chat/ChatHeader';
import MessageArea from '../components/chat/MessageArea';
import MessageInput from '../components/chat/MessageInput';
import EmptyState from '../components/chat/EmptyState';
import ErrorBoundary from '../components/common/ErrorBoundary';
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
  const sendAttachment = useChatStore((s) => s.sendAttachment);
  const typingUsers = useChatStore((s) => (activeId ? s.typingUsers[activeId] : null));
  const fetchUnreadCount = useNotificationStore((s) => s.fetchUnreadCount);

  const activeConversation = getActive();
  const stompRef = useWebSocket(user);
  usePresence(stompRef);

  useEffect(() => {
    fetchConversations();
    fetchUnreadCount();
  }, [fetchConversations, fetchUnreadCount]);

  const handleSend = async (content) => {
    if (activeId) await sendMessage(activeId, content);
  };

  const handleAttachment = async (file) => {
    if (activeId) await sendAttachment(activeId, file);
  };

  const handleTyping = useCallback((typing) => {
    const client = stompRef?.current;
    if (!client?.connected || !activeId) return;
    try {
      client.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({ conversationId: activeId, typing }),
      });
    } catch {}
  }, [stompRef, activeId]);

  const typingText = typingUsers?.length
    ? `${typingUsers.join(', ')} ${typingUsers.length === 1 ? 'is' : 'are'} typing...`
    : '';

  return (
    <div className="chat-layout">
      <Sidebar />

      <ErrorBoundary>
        {activeConversation ? (
          <div className="message-area">
            <ChatHeader
              conversation={activeConversation}
              currentUserId={user?.id}
              typingText={typingText}
            />
            <MessageArea conversationId={activeId} currentUserId={user?.id} />
            <MessageInput
              conversationId={activeId}
              onSend={handleSend}
              onAttachment={handleAttachment}
              onTyping={handleTyping}
            />
          </div>
        ) : (
          <div className="message-area">
            <EmptyState />
          </div>
        )}
      </ErrorBoundary>
    </div>
  );
}
