export type TaskStatus = 'PENDING' | 'READY' | 'RUNNING' | 'RETRYING' | 'SUCCEEDED' | 'FAILED' | 'DEAD_LETTER' | 'CANCELLED';

export interface Task {
  id: string;
  nodeKey: string;
  handlerType: string;
  status: TaskStatus;
  attempt: number;
}

export interface WorkflowRun {
  id: string;
  workflowDefinitionId: string;
  status: 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  tasks: Task[];
}

export interface WorkflowDefinition {
  id: string;
  name: string;
  version: number;
  graph: {
    nodes: { key: string; handlerType: string }[];
    edges: { from: string; to: string }[];
  };
}

export interface RunBundle { run: WorkflowRun; definition: WorkflowDefinition; }
