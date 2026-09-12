import { useEffect, useMemo, useState } from 'react';
import { getRunBundle, taskAction } from './api';
import { ExecutionTimeline } from './ExecutionTimeline';
import { WorkflowGraph } from './WorkflowGraph';
import { subscribeToRun } from './realtime';
import type { RunBundle, TaskStatus } from './types';

const workerId = 'console-operator';
const metricOrder: TaskStatus[] = ['READY', 'RUNNING', 'RETRYING', 'SUCCEEDED'];

export default function App() {
  const [runId, setRunId] = useState('');
  const [bundle, setBundle] = useState<RunBundle | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [socketConnected, setSocketConnected] = useState(false);

  const load = async (id = runId) => {
    if (!id.trim()) return;
    setLoading(true); setError('');
    try { setBundle(await getRunBundle(id.trim())); } catch (cause) { setBundle(null); setError(cause instanceof Error ? cause.message : 'Unable to load workflow run'); }
    finally { setLoading(false); }
  };
  useEffect(() => {
    if (!bundle) return;
    return subscribeToRun(bundle.run.id, (run) => setBundle((current) => current ? { ...current, run } : current), setSocketConnected);
  }, [bundle?.run.id]);

  const metrics = useMemo(() => metricOrder.map((status) => ({ status, total: bundle?.run.tasks.filter((task) => task.status === status).length ?? 0 })), [bundle]);
  const act = async (taskId: string, action: 'claim' | 'complete' | 'heartbeat') => {
    if (!bundle) return;
    setError('');
    try { const run = await taskAction(bundle.run.id, taskId, action, workerId); setBundle({ ...bundle, run }); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Task action failed'); }
  };

  return <main><header><div className="brand"><span className="brand-mark">D</span><div><strong>DurableFlow</strong><span>Workflow control plane</span></div></div><div className="header-status"><span className={socketConnected ? 'pulse' : 'pulse pulse-off'} /> {socketConnected ? 'Live stream connected' : 'Waiting for live stream'}</div></header>
    <section className="hero"><div><p className="eyebrow">Operations console</p><h1>Make workflow execution<br /><em>observable and recoverable.</em></h1><p className="subtle">Inspect live DAGs, follow task transitions, and operate work safely through durable state, leases, and retries.</p></div>
      <form onSubmit={(event) => { event.preventDefault(); load(); }}><label htmlFor="run-id">Workflow run ID</label><div className="search"><input id="run-id" value={runId} onChange={(event) => setRunId(event.target.value)} placeholder="Paste a UUID from POST /runs" /><button disabled={loading}>{loading ? 'Loading' : 'Open run'}</button></div>{error && <p className="error">{error}</p>}</form>
    </section>
    {!bundle && !loading && <section className="empty"><span>01</span><h2>Open a workflow run</h2><p>Start a run through the DurableFlow API, then paste its run ID here. The console reads the real backend state; it contains no disconnected demo data.</p></section>}
    {bundle && <><section className="metrics">{metrics.map(({ status, total }) => <article key={status}><span className={`metric-dot dot-${status.toLowerCase()}`} /><strong>{String(total).padStart(2, '0')}</strong><small>{status.toLowerCase().replace('_', ' ')}</small></article>)}<article><span className="metric-dot">v{bundle.definition.version}</span><strong>{bundle.run.status}</strong><small>run state</small></article></section>
      <section className="workspace"><div className="canvas-panel"><div className="panel-heading"><div><p className="eyebrow">{bundle.definition.name}</p><h2>Dependency graph</h2></div><span>Run {bundle.run.id.slice(0, 8)}</span></div><WorkflowGraph definition={bundle.definition} run={bundle.run} /></div><ExecutionTimeline tasks={bundle.run.tasks} /></section>
      <section className="panel actions"><div className="panel-heading"><div><p className="eyebrow">Operator controls</p><h2>Manual task operations</h2></div><span>Worker: {workerId}</span></div><div className="action-grid">{bundle.run.tasks.map((task) => <article key={task.id}><div><strong>{task.nodeKey}</strong><span>{task.status}</span></div><div>{task.status === 'READY' && <button onClick={() => act(task.id, 'claim')}>Claim</button>}{task.status === 'RUNNING' && <><button className="secondary" onClick={() => act(task.id, 'heartbeat')}>Heartbeat</button><button onClick={() => act(task.id, 'complete')}>Complete</button></>}</div></article>)}</div></section></>}
  </main>;
}
