import { useState, useEffect } from 'react';
import { getConnection, triggerManualSync, searchSyncHistory, getSyncHistory } from '../api/connections';

export default function ConnectionDetail({ connectionId, onBack }) {
  const [connection, setConnection] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [syncMessage, setSyncMessage] = useState('');
  const [syncing, setSyncing] = useState(false);

  useEffect(() => {
    loadAll();
  }, [connectionId]);

  const loadAll = async () => {
    setLoading(true);
    setError('');
    try {
      const connRes = await getConnection(connectionId);
      setConnection(connRes.data);
      await loadHistory();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load connection');
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async () => {
    try {
      const res = await searchSyncHistory(connectionId);
      setHistory(res.data);
    } catch {
      // Fallback to Postgres-backed history if ES search fails
      try {
        const res = await getSyncHistory(connectionId);
        setHistory(res.data.content || []);
      } catch (err2) {
        console.warn('Could not load sync history', err2);
      }
    }
  };

  const handleManualSync = async () => {
    setSyncMessage('');
    setSyncing(true);
    try {
      const res = await triggerManualSync(connectionId);
      setSyncMessage(res.data.message || 'Sync started');
      setTimeout(loadAll, 2000);
    } catch (err) {
      const data = err.response?.data;
      if (err.response?.status === 409) {
        setSyncMessage('A sync is already in progress for this connection.');
      } else if (err.response?.status === 429) {
        setSyncMessage('Manual sync quota exceeded for today. Try again tomorrow.');
      } else {
        setSyncMessage(data?.message || 'Failed to trigger sync');
      }
    } finally {
      setSyncing(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString();
  };

  if (loading) {
    return <div className="card">Loading...</div>;
  }

  if (error) {
    return (
      <div>
        <button className="btn btn-secondary" onClick={onBack}>← Back</button>
        <div className="error-banner" style={{ marginTop: '1rem' }}>{error}</div>
      </div>
    );
  }

  return (
    <div>
      <button className="btn btn-secondary" onClick={onBack}>← Back</button>

      <div className="card" style={{ marginTop: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h2 style={{ margin: '0 0 0.4rem 0' }}>{connection.name}</h2>
            <span className={`status-badge status-${connection.status}`}>{connection.status}</span>
          </div>
          <button className="btn" onClick={handleManualSync} disabled={syncing}>
            {syncing ? 'Starting...' : 'Sync Now'}
          </button>
        </div>

        {syncMessage && (
          <div className="card" style={{ background: '#eef2ff', marginTop: '1rem', marginBottom: 0 }}>
            {syncMessage}
          </div>
        )}

        {connection.status === 'error' && (
          <div className="error-banner" style={{ marginTop: '1rem' }}>
            <strong>This connection has an error.</strong> The last sync could not complete successfully.
            This is often caused by an expired authorization token. Try reconnecting the affected platform
            account by creating a new connection, or check the sync history below for details.
          </div>
        )}

        <div style={{ marginTop: '1rem', fontSize: '0.9rem', color: '#555', lineHeight: '1.6' }}>
          <div><strong>Sync interval:</strong> every {connection.intervalHours} hours</div>
          <div><strong>Timezone:</strong> {connection.timezone}</div>
          <div><strong>Last synced:</strong> {formatDate(connection.lastSyncAt)}
            {connection.lastSyncStatus && (
              <span className={`status-badge status-${connection.lastSyncStatus}`} style={{ marginLeft: '0.5rem' }}>
                {connection.lastSyncStatus}
              </span>
            )}
          </div>
          <div><strong>Next scheduled sync:</strong> {formatDate(connection.nextRunAtUtc)}</div>
        </div>
      </div>

      <div className="card">
        <h3>Sync History</h3>
        {history.length === 0 ? (
          <p style={{ color: '#888' }}>No sync runs yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Started</th>
                <th>Trigger</th>
                <th>Status</th>
                <th>Processed</th>
                <th>Failed</th>
                <th>Duration</th>
              </tr>
            </thead>
            <tbody>
              {history.map((run) => (
                <tr key={run.runId || run.id}>
                  <td>{formatDate(run.startedAt)}</td>
                  <td>{run.triggerType}</td>
                  <td><span className={`status-badge status-${run.status}`}>{run.status}</span></td>
                  <td>{run.eventsProcessed}</td>
                  <td>{run.eventsFailed}</td>
                  <td>
                    {run.durationMs
                      ? `${(run.durationMs / 1000).toFixed(1)}s`
                      : (run.startedAt && run.completedAt
                          ? `${((new Date(run.completedAt) - new Date(run.startedAt)) / 1000).toFixed(1)}s`
                          : '—')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}