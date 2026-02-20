import { useCallback, useEffect, useRef, useState } from 'react';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_URL } from '../config';

type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error';

type SubscriptionEntry = {
  onMessage: (body: unknown) => void;
  stompSub?: StompSubscription;
};

export function useStomp() {
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected');
  const [lastMessage, setLastMessage] = useState<unknown>(null);
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, SubscriptionEntry>>(new Map());

  const attachSubscription = useCallback((destination: string, entry: SubscriptionEntry) => {
    const client = clientRef.current;
    if (!client?.connected) return;

    entry.stompSub?.unsubscribe();
    entry.stompSub = client.subscribe(destination, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body) as unknown;
        setLastMessage(parsed);
        entry.onMessage(parsed);
      } catch {
        entry.onMessage(message.body);
      }
    });
  }, []);

  const connect = useCallback(() => {
    if (clientRef.current?.active || clientRef.current?.connected) return;

    setConnectionState('connecting');
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnectionState('connected');
        subscriptionsRef.current.forEach((entry, destination) => {
          attachSubscription(destination, entry);
        });
      },
      onStompError: () => setConnectionState('error'),
      onWebSocketClose: () => setConnectionState('disconnected'),
    });

    client.activate();
    clientRef.current = client;
  }, [attachSubscription]);

  const disconnect = useCallback(() => {
    subscriptionsRef.current.forEach((entry) => {
      entry.stompSub?.unsubscribe();
      entry.stompSub = undefined;
    });
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
    const entry: SubscriptionEntry = { onMessage };
    subscriptionsRef.current.set(destination, entry);
    attachSubscription(destination, entry);

    return () => {
      const current = subscriptionsRef.current.get(destination);
      if (!current) return;
      current.stompSub?.unsubscribe();
      subscriptionsRef.current.delete(destination);
    };
  }, [attachSubscription]);

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
