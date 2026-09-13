import { useEffect, useMemo, useState } from 'react';
import { createHttpAutomation, getRunBundle, taskAction, triggerAutomation } from './api';
import { ExecutionTimeline } from './ExecutionTimeline';
import { WorkflowGraph } from './WorkflowGraph';
import { subscribeToRun } from './realtime';
import type { RunBundle, TaskStatus, WorkflowDefinition } from './types';

const workerId = 'console-operator';
const metricOrder: TaskStatus[] = ['READY', 'RUNNING', 'RETRYING', 'SUCCEEDED'];
const sampleEvent = '{\n  "event": "invoice.paid",\n  "invoiceId": "inv-42"\n}';

export default function App() {
  const [runId, setRunId] = useState('');
  const [bundle, setBundle] = useState<RunBundle | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [socketConnected, setSocketConnected] = useState(false);
  const [automationName, setAutomationName] = useState('Invoice delivery');
  const [destinationUrl, setDestinationUrl] = useState('');
  const [eventBody, setEventBody] = useState(sampleEvent);
  const [automation, setAutomation] = useState<WorkflowDefinition | null>(null);
  const [creating, setCreating] = useState(false);
  const [triggering, setTriggering] = useState(false);
  const [copied, setCopied] = useState(false);

  const load = async (id = runId) => {
    if (!id.trim()) return;
    setLoading(true); setError('');
    try { setBundle(await getRunBundle(id.trim())); }
    catch (cause) { setBundle(null); setError(cause instanceof Error ? cause.message : 'Unable to load workflow run'); }
    finally { setLoading(false); }
  };

  useEffect(() => {
    if (!bundle) return;
    return subscribeToRun(bundle.run.id, (run) => setBundle((current) => current ? { ...current, run } : current), setSocketConnected);
  }, [bundle?.run.id]);

  const metrics = useMemo(() => metricOrder.map((status) => ({ status, total: bundle?.run.tasks.filter((task) => task.status === status).length ?? 0 })), [bundle]);
  const webhookUrl = automation ? `${window.location.origin}/api/hooks/workflows/${automation.id}` : '';

  const act = async (taskId: string, action: 'claim' | 'complete' | 'heartbeat') => {
    if (!bundle) return;
    setError('');
    try { const run = await taskAction(bundle.run.id, taskId, action, workerId); setBundle({ ...bundle, run }); }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Task action failed'); }
  };

  const createAutomation = async (event: React.FormEvent) => {
    event.preventDefault(); setError('');
    let payload: unknown;
    try { payload = JSON.parse(eventBody); }
    catch { setError('Example event must be valid JSON.'); return; }
    try {
      new URL(destinationUrl);
      if (!destinationUrl.startsWith('https://')) throw new Error('Destination must start with https://');
      setCreating(true);
      setAutomation(await createHttpAutomation(automationName, destinationUrl, payload));
      setCopied(false);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not create automation'); }
    finally { setCreating(false); }
  };

  const copyWebhookUrl = async () => {
    if (!webhookUrl) return;
    await navigator.clipboard.writeText(webhookUrl);
    setCopied(true);
  };

  const sendSample = async () => {
    if (!automation) return;
    setError('');
    try {
      const payload = JSON.parse(eventBody);
      setTriggering(true);
      const run = await triggerAutomation(automation.id, payload);
      setRunId(run.id);
      setBundle(await getRunBundle(run.id));
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not send sample event'); }
    finally { setTriggering(false); }
  };

  return <main>
    <header><div className="brand"><span className="brand-mark">D</span><div><strong>DurableFlow</strong><span>Webhook automation, made durable</span></div></div><div className="header-status"><span className={socketConnected ? 'pulse' : 'pulse pulse-off'} /> {socketConnected ? 'Live stream connected' : 'Waiting for live stream'}</div></header>
    <section className="hero"><div><p className="eyebrow">Self-hosted automation</p><h1>Turn every webhook into<br /><em>a reliable action.</em></h1><p className="subtle">Create an HTTPS delivery automation, give its trigger URL to any tool, and see every delivery, retry, and final outcome in one place.</p></div>
      <form className="run-search" onSubmit={(event) => { event.preventDefault(); load(); }}><label htmlFor="run-id">Workflow run ID</label><div className="search"><input id="run-id" value={runId} onChange={(event) => setRunId(event.target.value)} placeholder="Paste a run UUID" /><button disabled={loading}>{loading ? 'Loading' : 'Open run'}</button></div></form>
    </section>
    <section className="automation-builder"><div className="builder-copy"><p className="eyebrow">01 — Create automation</p><h2>Deliver an event to your endpoint</h2><p>DurableFlow accepts a webhook, then calls your HTTPS endpoint. A failed call is retried automatically and remains visible here.</p><span>One trigger · one durable HTTP action</span></div>
      <form className="automation-form" onSubmit={createAutomation}>
        <label>Automation name<input value={automationName} onChange={(event) => setAutomationName(event.target.value)} maxLength={80} required /></label>
        <label>HTTPS destination URL<input type="url" value={destinationUrl} onChange={(event) => setDestinationUrl(event.target.value)} placeholder="https://your-app.com/hooks/invoice" required /></label>
        <label>Example event JSON<textarea value={eventBody} onChange={(event) => setEventBody(event.target.value)} rows={4} spellCheck="false" required /></label>
        <button disabled={creating}>{creating ? 'Creating…' : 'Create automation'}</button>
      </form>
    </section>
    {automation && <section className="webhook-card"><div><p className="eyebrow">02 — Connect a source app</p><h2>Your webhook trigger URL</h2><p>Put this URL in Stripe, GitHub, a form tool, or your own backend. Each POST creates a tracked delivery run.</p></div><div className="webhook-actions"><code>{webhookUrl}</code><div><button className="secondary" onClick={copyWebhookUrl}>{copied ? 'Copied' : 'Copy URL'}</button><button onClick={sendSample} disabled={triggering}>{triggering ? 'Sending…' : 'Send sample event'}</button></div></div></section>}
    {error && <p className="error page-error">{error}</p>}
    {!bundle && !loading && <section className="empty"><span>03</span><h2>Watch your deliveries</h2><p>Create an automation above, send the sample event, or POST to its trigger URL. DurableFlow will open the run automatically and show the delivery result.</p></section>}
    {bundle && <><section className="metrics">{metrics.map(({ status, total }) => <article key={status}><span className={`metric-dot dot-${status.toLowerCase()}`} /><strong>{String(total).padStart(2, '0')}</strong><small>{status.toLowerCase().replace('_', ' ')}</small></article>)}<article><span className="metric-dot">v{bundle.definition.version}</span><strong>{bundle.run.status}</strong><small>run state</small></article></section>
      <section className="workspace"><div className="canvas-panel"><div className="panel-heading"><div><p className="eyebrow">{bundle.definition.name}</p><h2>Delivery graph</h2></div><span>Run {bundle.run.id.slice(0, 8)}</span></div><WorkflowGraph definition={bundle.definition} run={bundle.run} /></div><ExecutionTimeline tasks={bundle.run.tasks} /></section>
      <section className="panel actions"><div className="panel-heading"><div><p className="eyebrow">Operator controls</p><h2>Manual task operations</h2></div><span>Worker: {workerId}</span></div><div className="action-grid">{bundle.run.tasks.map((task) => <article key={task.id}><div><strong>{task.nodeKey}</strong><span>{task.status}</span></div><div>{task.status === 'READY' && <button onClick={() => act(task.id, 'claim')}>Claim</button>}{task.status === 'RUNNING' && <><button className="secondary" onClick={() => act(task.id, 'heartbeat')}>Heartbeat</button><button onClick={() => act(task.id, 'complete')}>Complete</button></>}</div></article>)}</div></section></>}
  </main>;
}
