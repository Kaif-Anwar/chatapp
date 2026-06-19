import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';
import { WS_URL } from '../utils/constants';
import { useChatStore } from '../stores/chatStore';
import { useNotificationStore } from '../stores/notificationStore';
import { useUiStore } from '../stores/uiStore';

function isValidWsMessage(msg) {
  return (
    msg != null &&
    typeof msg === 'object' &&
    msg.id != null &&
    typeof msg.content === 'string'
  );
}

export function useWebSocket(user) {
  const clientRef = useRef(null);
  const subscribedConvIds = useRef(new Set());
  const conversations = useChatStore((s) => s.conversations);
  const addMessage = useChatStore((s) => s.addMessage);
  const fetchConversations = useChatStore((s) => s.fetchConversations);
  const addNotification = useNotificationStore((s) => s.addNotification);
  const addToast = useUiStore((s) => s.addToast);
  const setTypingUsers = useChatStore((s) => s.setTypingUsers);
  const updateMessageStatus = useChatStore((s) => s.updateMessageStatus);

  const setTypingUsersRef = useRef(setTypingUsers);
  const updateMessageStatusRef = useRef(updateMessageStatus);

  useEffect(() => { setTypingUsersRef.current = setTypingUsers; }, [setTypingUsers]);
  useEffect(() => { updateMessageStatusRef.current = updateMessageStatus; }, [updateMessageStatus]);
  const addMessageRef = useRef(addMessage);
  const addNotificationRef = useRef(addNotification);
  const addToastRef = useRef(addToast);
  const fetchConversationsRef = useRef(fetchConversations);

  useEffect(() => { addMessageRef.current = addMessage; }, [addMessage]);
  useEffect(() => { addNotificationRef.current = addNotification; }, [addNotification]);
  useEffect(() => { addToastRef.current = addToast; }, [addToast]);
  useEffect(() => { fetchConversationsRef.current = fetchConversations; }, [fetchConversations]);

  const handleIncomingMessage = useCallback((conversationId, raw) => {
    let msg;
    try {
      msg = typeof raw === 'string' ? JSON.parse(raw) : raw;
    } catch {
      console.warn('WebSocket: failed to parse message payload');
      return;
    }
    if (!isValidWsMessage(msg)) {
      console.warn('WebSocket: ignoring invalid message payload', msg);
      return;
    }
    addMessageRef.current(conversationId ?? msg.conversationId, msg);
  }, []);

  // Stable connect function — only depends on user identity, NOT conversations
  const connect = useCallback(() => {
    if (!user || clientRef.current?.active) return;

    const token = localStorage.getItem('accessToken');
    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_URL}?token=${token}`),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        console.log('WebSocket connected');

        // Subscribe to personal notifications queue
        client.subscribe('/user/queue/notifications', (frame) => {
          try {
            const notif = JSON.parse(frame.body);
            addNotificationRef.current(notif);
            addToastRef.current(notif.title || 'New notification', 'info');
            // Refresh conversation list on group invites or new messages
            if (notif.type === 'GROUP_INVITE' || notif.type === 'NEW_MESSAGE') {
              fetchConversationsRef.current();
            }
          } catch {}
        });

        // Subscribe to personal message queue (private messages)
        client.subscribe('/user/queue/messages', (frame) => {
          try {
            const msg = JSON.parse(frame.body);
            if (msg?.conversationId) {
              handleIncomingMessage(msg.conversationId, msg);
            }
          } catch {}
        });

        // Reset tracked subscriptions — they were lost on reconnect
        subscribedConvIds.current.clear();
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message);
      },
    });

    client.activate();
    clientRef.current = client;
  }, [user, handleIncomingMessage]);

  // Connect when user logs in — stable dependency, won't loop
  useEffect(() => {
    connect();
    return () => {
      if (clientRef.current?.active) {
        clientRef.current.deactivate();
      }
      subscribedConvIds.current.clear();
    };
  }, [connect]);

  // Subscribe to NEW conversation topics when conversations list changes
  useEffect(() => {
    const client = clientRef.current;
    if (!client?.connected) return;

    conversations.forEach((conv) => {
      if (!subscribedConvIds.current.has(conv.id)) {
        const dest = `/topic/conversation.${conv.id}`;
        client.subscribe(dest, (frame) => {
          try { handleIncomingMessage(conv.id, frame.body); } catch {}
        });
        client.subscribe(`/topic/conversation.${conv.id}.typing`, (frame) => {
          try {
            const event = JSON.parse(frame.body);
            setTypingUsersRef.current(conv.id, [...(event.typingUsers || [])]);
          } catch {}
        });
        client.subscribe(`/topic/conversation.${conv.id}.read`, (frame) => {
          try {
            const event = JSON.parse(frame.body);
            if (event.messageId && event.status) {
              updateMessageStatusRef.current(conv.id, event.messageId, event.status);
            }
          } catch {}
        });
        subscribedConvIds.current.add(conv.id);
      }
    });
  }, [conversations, handleIncomingMessage]);

  return clientRef;
}
