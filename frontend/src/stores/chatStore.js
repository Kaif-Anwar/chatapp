import { create } from 'zustand';
import { conversationsApi } from '../api/conversations';
import { messagesApi } from '../api/messages';
import { useUiStore } from './uiStore';
import { normalizeMessage } from '../utils/messageUtils';

export const useChatStore = create((set, get) => ({
  conversations: [],
  activeConversationId: null,
  messages: {},          // { [convId]: Message[] }
  messagePagination: {}, // { [convId]: { page, totalPages, loading } }
  typingUsers: {},       // { [convId]: string[] }
  loadingConversations: false,

  // ── Conversations ──────────────────────────────────────────
  fetchConversations: async () => {
    set({ loadingConversations: true });
    try {
      const data = await conversationsApi.getAll();
      set({ conversations: data || [], loadingConversations: false });
    } catch {
      set({ loadingConversations: false });
    }
  },

  setActiveConversation: (id) => {
    set({ activeConversationId: id });
    if (id != null) {
      get().fetchMessages(id);
    }
  },

  getActiveConversation: () => {
    const { conversations, activeConversationId } = get();
    return conversations.find((c) => c.id === activeConversationId) || null;
  },

  // ── Messages ───────────────────────────────────────────────
  fetchMessages: async (conversationId) => {
    const pagination = get().messagePagination[conversationId] || { page: 0, totalPages: 1, loading: false };
    if (pagination.loading || pagination.page >= pagination.totalPages) return;

    set((s) => ({
      messagePagination: {
        ...s.messagePagination,
        [conversationId]: { ...pagination, loading: true },
      },
    }));

    try {
      const data = await messagesApi.getByConversation(conversationId, pagination.page);
      const newMessages = (data.content || [])
        .map(normalizeMessage)
        .filter(Boolean);
      set((s) => ({
        messages: {
          ...s.messages,
          [conversationId]: pagination.page === 0
            ? newMessages.reverse()
            : [...newMessages.reverse(), ...(s.messages[conversationId] || [])],
        },
        messagePagination: {
          ...s.messagePagination,
          [conversationId]: {
            page: pagination.page + 1,
            totalPages: data.totalPages || 1,
            loading: false,
          },
        },
      }));
    } catch {
      set((s) => ({
        messagePagination: {
          ...s.messagePagination,
          [conversationId]: { ...pagination, loading: false },
        },
      }));
    }
  },

  loadMoreMessages: (conversationId) => {
    get().fetchMessages(conversationId);
  },

  sendMessage: async (conversationId, content) => {
    try {
      const msg = await messagesApi.send(conversationId, { content, type: 'TEXT' });
      const normalized = normalizeMessage(msg);
      if (!normalized) {
        useUiStore.getState().addToast('Failed to send message', 'error');
        return null;
      }
      get().addMessage(conversationId, normalized);
      return normalized;
    } catch (err) {
      useUiStore.getState().addToast(err.message || 'Failed to send message', 'error');
      return null;
    }
  },

  sendAttachment: async (conversationId, file, content = '') => {
    try {
      const msg = await messagesApi.sendAttachment(conversationId, file, content);
      const normalized = normalizeMessage(msg);
      if (!normalized) {
        useUiStore.getState().addToast('Failed to send attachment', 'error');
        return null;
      }
      get().addMessage(conversationId, normalized);
      return normalized;
    } catch (err) {
      useUiStore.getState().addToast(err.message || 'Failed to send attachment', 'error');
      return null;
    }
  },

  markLatestRead: async (conversationId, currentUserId) => {
    const msgs = get().messages[conversationId] || [];
    const lastFromOther = [...msgs].reverse().find((m) => m?.senderId != null && Number(m.senderId) !== Number(currentUserId));
    if (lastFromOther?.id) {
      try {
        await messagesApi.markRead(conversationId, lastFromOther.id);
      } catch {}
    }
  },

  setTypingUsers: (conversationId, users) => {
    const list = Array.isArray(users) ? users : [];
    set((s) => ({
      typingUsers: { ...s.typingUsers, [conversationId]: list },
    }));
  },

  updateMessageStatus: (conversationId, messageId, status) => {
    set((s) => ({
      messages: {
        ...s.messages,
        [conversationId]: (s.messages[conversationId] || []).map((m) =>
          m?.id === messageId ? { ...m, status } : m
        ),
      },
    }));
  },

  addMessage: (conversationId, message) => {
    const normalized = normalizeMessage(message);
    if (!normalized) return;

    set((s) => {
      const existing = s.messages[conversationId] || [];
      if (existing.some((m) => m?.id != null && m.id === normalized.id)) return s;
      return {
        messages: {
          ...s.messages,
          [conversationId]: [...existing, normalized],
        },
      };
    });
  },

  updateMessage: (conversationId, messageId, updates) => {
    set((s) => ({
      messages: {
        ...s.messages,
        [conversationId]: (s.messages[conversationId] || []).map((m) =>
          m?.id === messageId ? { ...m, ...updates } : m
        ),
      },
    }));
  },

  removeMessage: (conversationId, messageId) => {
    set((s) => ({
      messages: {
        ...s.messages,
        [conversationId]: (s.messages[conversationId] || []).map((m) =>
          m?.id === messageId ? { ...m, deleted: true, content: 'This message was deleted' } : m
        ),
      },
    }));
  },
}));
