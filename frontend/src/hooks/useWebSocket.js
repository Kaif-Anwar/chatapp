import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';
import { WS_URL } from '../utils/constants';
import { useChatStore } from '../stores/chatStore';
import { useNotificationStore } from '../stores/notificationStore';
import { useUiStore } from '../stores/uiStore';

export function useWebSocket(user) {
  const clientRef = useRef(null);
  const addMessage = useChatStore((s) => s.addMessage);
  const conversations = useChatStore((s) => s.conversations);
  const fetchConversations = useChatStore((s) => s.fetchConversations);
  const addNotification = useNotificationStore((s) => s.addNotification);
  const addToast = useUiStore((s) => s.addToast);

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

        // Subscribe to each conversation's topic
        conversations.forEach((conv) => {
          client.subscribe(`/topic/conversation.${conv.id}`, (frame) => {
            try {
              const msg = JSON.parse(frame.body);
              addMessage(conv.id, msg);
            } catch {}
          });
        });

        // Subscribe to personal notifications queue
        client.subscribe('/user/queue/notifications', (frame) => {
          try {
            const notif = JSON.parse(frame.body);
            addNotification(notif);
            addToast(notif.title || 'New notification', 'info');
            // Refresh conversation list on group invites or new messages
            if (notif.type === 'GROUP_INVITE' || notif.type === 'NEW_MESSAGE') {
              fetchConversations();
            }
          } catch {}
        });

        // Subscribe to personal message queue (private messages)
        client.subscribe('/user/queue/messages', (frame) => {
          try {
            const msg = JSON.parse(frame.body);
            if (msg.conversationId) {
              addMessage(msg.conversationId, msg);
            }
          } catch {}
        });
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message);
      },
    });

    client.activate();
    clientRef.current = client;
  }, [user, conversations, addMessage, addNotification, addToast, fetchConversations]);

  // Connect when user logs in
  useEffect(() => {
    connect();
    return () => {
      if (clientRef.current?.active) {
        clientRef.current.deactivate();
      }
    };
  }, [connect]);

  // Re-subscribe when conversations change
  useEffect(() => {
    const client = clientRef.current;
    if (!client?.active) return;

    conversations.forEach((conv) => {
      const dest = `/topic/conversation.${conv.id}`;
      // Check if already subscribed (STOMP.js doesn't expose this easily, so we re-subscribe)
      client.subscribe(dest, (frame) => {
        try {
          const msg = JSON.parse(frame.body);
          addMessage(conv.id, msg);
        } catch {}
      });
    });
  }, [conversations.length]);

  return clientRef;
}
