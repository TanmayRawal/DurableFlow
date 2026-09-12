import { Background, Handle, Position, ReactFlow, type Edge, type Node, type NodeProps } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import type { WorkflowDefinition, WorkflowRun, TaskStatus } from './types';

type FlowNodeData = { label: string; handlerType: string; status: TaskStatus };
const statusClass = (status: TaskStatus) => `node node-${status.toLowerCase()}`;

function TaskNode({ data }: NodeProps<Node<FlowNodeData>>) {
  return <div className={statusClass(data.status)}><Handle type="target" position={Position.Top} />
    <span className="node-status">{data.status.replace('_', ' ')}</span><strong>{data.label}</strong><small>{data.handlerType} handler</small>
    <Handle type="source" position={Position.Bottom} />
  </div>;
}

const nodeTypes = { task: TaskNode };

export function WorkflowGraph({ definition, run }: { definition: WorkflowDefinition; run: WorkflowRun }) {
  const statusByKey = new Map(run.tasks.map((task) => [task.nodeKey, task.status]));
  const nodes: Node<FlowNodeData>[] = definition.graph.nodes.map((node, index) => ({
    id: node.key, type: 'task', position: { x: 120 + (index % 3) * 235, y: 70 + Math.floor(index / 3) * 175 },
    data: { label: node.key, handlerType: node.handlerType, status: statusByKey.get(node.key) ?? 'PENDING' }
  }));
  const edges: Edge[] = definition.graph.edges.map((edge) => ({ id: `${edge.from}-${edge.to}`, source: edge.from, target: edge.to, animated: statusByKey.get(edge.from) === 'RUNNING' }));
  return <div className="graph"><ReactFlow nodes={nodes} edges={edges} nodeTypes={nodeTypes} fitView nodesDraggable={false} nodesConnectable={false}>
    <Background gap={20} color="#26324a" />
  </ReactFlow></div>;
}
