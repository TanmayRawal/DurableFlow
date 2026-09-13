import type { RunBundle, WorkflowDefinition, WorkflowRun } from './types';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, options);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: response.statusText }));
    throw new Error(error.error ?? `Request failed with ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function getRunBundle(runId: string): Promise<RunBundle> {
  const run = await request<WorkflowRun>(`/api/runs/${runId}`);
  const definition = await request<WorkflowDefinition>(`/api/workflows/${run.workflowDefinitionId}`);
  return { run, definition };
}

export function createHttpAutomation(name: string, url: string, body: unknown): Promise<WorkflowDefinition> {
  return request('/api/workflows', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
    name, description: 'Webhook-triggered HTTPS delivery', graph: { nodes: [{ key: 'deliver-event', handlerType: 'http', config: { url, method: 'POST', body } }], edges: [] }
  }) });
}

export function triggerAutomation(definitionId: string, payload: unknown): Promise<WorkflowRun> {
  return request(`/api/hooks/workflows/${definitionId}`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(payload) });
}
