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

export function taskAction(runId: string, taskId: string, action: 'claim' | 'complete' | 'heartbeat', workerId: string): Promise<WorkflowRun> {
  return request(`/api/runs/${runId}/tasks/${taskId}/${action}`, { method: 'POST', headers: { 'Worker-Id': workerId } });
}
