import { useEffect, useRef } from 'react';

/**
 * Sends a heartbeat ping every 20 seconds to keep the user marked as ONLINE.
 * Requires the STOMP client ref from useWebSocket.
 */
export function usePresence(stompClientRef) {
  const intervalRef = useRef(null);

  useEffect(() => {
    const sendPing = () => {
      const client = stompClientRef?.current;
      if (client?.active && client?.connected) {
        try {
          client.publish({ destination: '/app/presence.ping', body: '' });
        } catch {
          // Connection not yet ready — ignore and retry on next interval
        }
      }
    };

    // Delay initial ping to allow STOMP handshake to complete
    const initialTimeout = setTimeout(sendPing, 3000);

    // Ping every 20 seconds
    intervalRef.current = setInterval(sendPing, 20000);

    return () => {
      clearTimeout(initialTimeout);
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [stompClientRef]);
}
