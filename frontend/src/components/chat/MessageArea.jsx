import { useRef, useEffect, useCallback } from 'react';
import MessageBubble from './MessageBubble';
import Spinner from '../common/Spinner';
import { isSameDay, formatDayLabel } from '../../utils/formatDate';
import { useChatStore } from '../../stores/chatStore';
import { messagesApi } from '../../api/messages';
import { useUiStore } from '../../stores/uiStore';

export default function MessageArea({ conversationId, currentUserId }) {
  const userId = Number(currentUserId);
  const messages = useChatStore((s) => s.messages[conversationId] || []);
  const pagination = useChatStore((s) => s.messagePagination[conversationId]);
  const loadMore = useChatStore((s) => s.loadMoreMessages);
  const updateMessage = useChatStore((s) => s.updateMessage);
  const removeMessage = useChatStore((s) => s.removeMessage);
  const markLatestRead = useChatStore((s) => s.markLatestRead);
  const addToast = useUiStore((s) => s.addToast);
  const bottomRef = useRef(null);
  const containerRef = useRef(null);
  const prevLengthRef = useRef(0);

  useEffect(() => {
    if (conversationId && currentUserId) {
      markLatestRead(conversationId, currentUserId);
    }
  }, [conversationId, currentUserId, messages.length, markLatestRead]);

  useEffect(() => {
    if (messages.length > prevLengthRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
      markLatestRead(conversationId, currentUserId);
    }
    prevLengthRef.current = messages.length;
  }, [messages.length, conversationId, currentUserId, markLatestRead]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView();
    prevLengthRef.current = messages.length;
  }, [conversationId]);

  const handleScroll = useCallback(() => {
    const el = containerRef.current;
    if (el && el.scrollTop < 50 && pagination && !pagination.loading && pagination.page < pagination.totalPages) {
      const prevHeight = el.scrollHeight;
      loadMore(conversationId);
      requestAnimationFrame(() => {
        el.scrollTop = el.scrollHeight - prevHeight;
      });
    }
  }, [conversationId, pagination, loadMore]);

  const handleEdit = async (msg) => {
    const newContent = prompt('Edit message:', msg.content);
    if (newContent && newContent !== msg.content) {
      try {
        await messagesApi.edit(conversationId, msg.id, { content: newContent });
        updateMessage(conversationId, msg.id, { content: newContent, editedAt: new Date().toISOString() });
      } catch {
        addToast('Failed to edit message', 'error');
      }
    }
  };

  const handleDelete = async (msg) => {
    if (!confirm('Delete this message?')) return;
    try {
      await messagesApi.delete(conversationId, msg.id);
      removeMessage(conversationId, msg.id);
    } catch {
      addToast('Failed to delete message', 'error');
    }
  };

  return (
    <div className="messages-container" ref={containerRef} onScroll={handleScroll}>
      {pagination?.loading && (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '12px' }}>
          <Spinner size="sm" />
        </div>
      )}

      {messages.map((msg, idx) => {
        if (!msg || msg.id == null) return null;

        const prev = messages[idx - 1];
        const senderId = Number(msg.senderId);
        const showDay = !prev || !isSameDay(prev.createdAt, msg.createdAt);
        const showSender = senderId !== userId && (!prev || Number(prev.senderId) !== senderId);

        return (
          <div key={msg.id}>
            {showDay && (
              <div className="day-separator">{formatDayLabel(msg.createdAt)}</div>
            )}
            <MessageBubble
              message={msg}
              isSent={senderId === userId}
              showSender={showSender}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          </div>
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
