import { useState, useEffect } from 'react';
import { listConnections } from '../api/connections';

export default function Dashboard({ onNewConnection, onViewConnection }) {
  const [connections, setConnections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadConnections();
  }, []);

  const loadConnections = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await listConnections();
      setConnections(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load connections');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString();
  };

  if (loading) {
    return <div className="card">Loading connections...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2>Your Connections</h2>
        <button className="btn" onClick={onNewConnection}>+ New Connection</button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {connections.length === 0 && !error && (
        <div className="card empty-state">
          <h3>No connections yet</h3>
          <p>Connect your Shopify store to QuickBooks Online to start syncing orders and customers automatically.</p>
          <button className="btn" onClick={onNewConnection}>Connect your first client</button>
        </div>
      )}

      {connections.map((conn) => (
        <div className="card connection-card" key={conn.id} onClick={() => onViewConnection(conn.id)}>
          <div>
            <h3 style={{ margin: '0 0 0.4rem 0' }}>{conn.name}</h3>
            <div style={{ fontSize: '0.85rem', color: '#666' }}>
              Last synced: {formatDate(conn.lastSyncAt)}
              {conn.nextRunAtUtc && <> · Next sync: {formatDate(conn.nextRunAtUtc)}</>}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <span className={`status-badge status-${conn.status}`}>{conn.status}</span>
            {conn.lastSyncStatus && (
              <div style={{ marginTop: '0.4rem' }}>
                <span className={`status-badge status-${conn.lastSyncStatus}`}>
                  last run: {conn.lastSyncStatus}
                </span>
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}