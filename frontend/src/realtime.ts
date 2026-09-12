import { Client } from '@stomp/stompjs';
import type { WorkflowRun } from './types';

export function subscribeToRun(runId: string, onUpdate: (run: WorkflowRun) => void, onConnectionChange: (connected: boolean) => void): () => void {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const client = new Client({
    // Vite and nginx proxy /ws to the API service, keeping the browser on one origin.
    brokerURL: `${protocol}://${window.location.host}/ws`,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => { onConnectionChange(true); client.subscribe(`/topic/runs/${runId}`, (message) => onUpdate(JSON.parse(message.body) as WorkflowRun)); },
    onWebSocketClose: () => onConnectionChange(false),
    onStompError: () => onConnectionChange(false)
  });
  client.activate();
  return () => { void client.deactivate(); };
}
