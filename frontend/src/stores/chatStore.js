import { create } from 'zustand';
import { conversationsApi } from '../api/conversations';
import { messagesApi } from '../api/messages';
import { useUiStore } from './uiStore';

function isValidMessage(message) {
  return (
    message != null &&
    typeof message === 'object' &&
    message.id != null &&
    typeof message.content === 'string'
  );
}

export const useChatStore = create((set, get) => ({
  conversations: [],
  activeConversationId: null,
  messages: {},          // { [convId]: Message[] }
  messagePagination: {}, // { [convId]: { page, totalPages, loading } }
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
    // Fetch messages if we don't have them yet
    if (id && !get().messages[id]) {
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
      const newMessages = (data.content || []).filter(isValidMessage);
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
      if (!isValidMessage(msg)) {
        useUiStore.getState().addToast('Failed to send message', 'error');
        return null;
      }
      get().addMessage(conversationId, msg);
      return msg;
    } catch (err) {
      useUiStore.getState().addToast(err.message || 'Failed to send message', 'error');
      return null;
    }
  },

  addMessage: (conversationId, message) => {
    if (!isValidMessage(message)) return;

    set((s) => {
      const existing = s.messages[conversationId] || [];
      if (existing.some((m) => m?.id != null && m.id === message.id)) return s;
      return {
        messages: {
          ...s.messages,
          [conversationId]: [...existing, message],
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
