import { Client } from '@stomp/stompjs';
import { useMarketStore } from '@/stores/marketStore';
import type { QuotesMessage } from '@/types/market';

export const QUOTES_TOPIC = '/topic/market/quotes';
const SUPPORTED_VERSION = 1;

let client: Client | null = null;

function webSocketUrl(): string {
  const configured: string | undefined = import.meta.env.VITE_WS_URL;
  if (configured) return configured;
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}/ws`;
}

/** Validates and applies one STOMP frame body. Unknown types/versions are ignored, not misinterpreted. */
export function handleQuotesFrame(body: string): boolean {
  let message: QuotesMessage;
  try {
    message = JSON.parse(body) as QuotesMessage;
  } catch {
    return false;
  }
  if (message.type !== 'QUOTES' || message.version !== SUPPORTED_VERSION || !Array.isArray(message.quotes)) {
    return false;
  }
  useMarketStore.getState().applyUpdate(message);
  return true;
}

/**
 * Opens the live market-data stream (idempotent). @stomp/stompjs reconnects automatically every 3 s after a
 * dropped connection and re-subscribes in onConnect.
 */
export function connectMarketStream(): void {
  if (client) return;
  const store = useMarketStore.getState();
  store.setStream('connecting');
  client = new Client({
    brokerURL: webSocketUrl(),
    reconnectDelay: 3000,
    onConnect: () => {
      useMarketStore.getState().setStream('connected');
      client?.subscribe(QUOTES_TOPIC, (frame) => handleQuotesFrame(frame.body));
    },
    onWebSocketClose: () => {
      if (client) useMarketStore.getState().setStream('connecting');
    },
    onStompError: () => useMarketStore.getState().setStream('connecting'),
  });
  client.activate();
}

export function disconnectMarketStream(): void {
  const current = client;
  client = null;
  void current?.deactivate();
  useMarketStore.getState().setStream('disconnected');
}
