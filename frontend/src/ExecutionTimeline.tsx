import type { Task } from './types';

const order = ['PENDING', 'READY', 'RUNNING', 'RETRYING', 'SUCCEEDED', 'DEAD_LETTER'] as const;
export function ExecutionTimeline({ tasks }: { tasks: Task[] }) {
  return <section className="panel timeline"><div className="panel-heading"><div><p className="eyebrow">Execution trace</p><h2>Task state</h2></div><span>{tasks.length} nodes</span></div>
    <div className="task-list">{tasks.map((task) => <article className="task-row" key={task.id}>
      <div className={`dot dot-${task.status.toLowerCase()}`} /><div className="task-copy"><strong>{task.nodeKey}</strong><span>{task.handlerType} handler · attempt {task.attempt}</span></div>
      <span className={`badge badge-${task.status.toLowerCase()}`}>{task.status.replace('_', ' ')}</span>
    </article>)}</div>
    <div className="legend">{order.map((state) => <span key={state}><i className={`dot dot-${state.toLowerCase()}`} />{state.replace('_', ' ')}</span>)}</div>
  </section>;
}
