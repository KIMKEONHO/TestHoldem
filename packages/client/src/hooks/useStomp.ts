import { useCallback, useEffect, useRef, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_URL } from '../config';

type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error';

export function useStomp() {
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected');
  const [lastMessage, setLastMessage] = useState<unknown>(null);
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, () => void>>(new Map());

  const connect = useCallback(() => {
    if (clientRef.current?.connected) return;

    setConnectionState('connecting');
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => setConnectionState('connected'),
      onStompError: () => setConnectionState('error'),
      onWebSocketClose: () => setConnectionState('disconnected'),
    });

    client.activate();
    clientRef.current = client;
  }, []);

  const disconnect = useCallback(() => {
    subscriptionsRef.current.forEach((unsub) => unsub());
    subscriptionsRef.current.clear();
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnectionState('disconnected');
    setLastMessage(null);
  }, []);

  const send = useCallback((destination: string, body: object = {}) => {
    if (!clientRef.current?.connected) return;
    clientRef.current.publish({
      destination,
      body: JSON.stringify(body),
      headers: { 'content-type': 'application/json' },
    });
  }, []);

  const subscribe = useCallback((destination: string, onMessage: (body: unknown) => void) => {
    if (!clientRef.current?.connected) return () => {};

    const sub = clientRef.current.subscribe(destination, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body) as unknown;
        setLastMessage(parsed);
        onMessage(parsed);
      } catch {
        onMessage(message.body);
      }
    });

    const unsub = () => {
      sub.unsubscribe();
      subscriptionsRef.current.delete(destination);
    };
    subscriptionsRef.current.set(destination, unsub);
    return unsub;
  }, []);

  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    connectionState,
    lastMessage,
    connect,
    disconnect,
    send,
    subscribe,
    isConnected: connectionState === 'connected',
  };
}
