
import { useState } from 'react';
import { getShopifyAuthUrl } from '../api/shopify';
import { getQboAuthUrl } from '../api/qbo';
import { createConnection } from '../api/connections';

const TIMEZONES = [
  'UTC', 'America/New_York', 'America/Los_Angeles', 'America/Chicago',
  'Europe/London', 'Europe/Berlin', 'Asia/Kolkata', 'Asia/Tokyo', 'Australia/Sydney'
];

export default function ConnectionWizard({ onDone }) {
  const [step, setStep] = useState(1);
  const [shopDomain, setShopDomain] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Step 3 form state
  const [name, setName] = useState('');
  const [timezone, setTimezone] = useState(Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC');
  const [intervalHours, setIntervalHours] = useState(4);
  const [sourceAccountId, setSourceAccountId] = useState('');
  const [destinationAccountId, setDestinationAccountId] = useState('');

  const handleShopifyConnect = async () => {
    if (!shopDomain) {
      setError('Enter your Shopify store domain (e.g. mystore.myshopify.com)');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const res = await getShopifyAuthUrl(shopDomain);
      // Redirect to Shopify's OAuth consent screen
      window.location.href = res.data.authorizationUrl;
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to start Shopify connection');
      setLoading(false);
    }
  };

  const handleQboConnect = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await getQboAuthUrl();
      window.location.href = res.data.authorizationUrl;
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to start QuickBooks connection');
      setLoading(false);
    }
  };

  const handleCreateConnection = async (e) => {
    e.preventDefault();
    setError('');

    if (!sourceAccountId || !destinationAccountId) {
      setError('Please provide both the Shopify account ID and QuickBooks account ID returned from the OAuth callbacks (check docker logs if needed).');
      return;
    }

    setLoading(true);
    try {
      await createConnection({
        name,
        sourceAccountId,
        destinationAccountId,
        timezone,
        intervalHours: Number(intervalHours),
        backfillDays: 30,
      });
      onDone();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create connection');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>New Connection</h2>

      <div className="wizard-steps">
        <div className={`wizard-step ${step === 1 ? 'active' : step > 1 ? 'done' : ''}`}>1. Connect Shopify</div>
        <div className={`wizard-step ${step === 2 ? 'active' : step > 2 ? 'done' : ''}`}>2. Connect QuickBooks</div>
        <div className={`wizard-step ${step === 3 ? 'active' : ''}`}>3. Configure Sync</div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {step === 1 && (
        <div className="card">
          <h3>Connect your Shopify store</h3>
          <p>Enter your store's myshopify.com domain. You'll be redirected to Shopify to authorize access.</p>
          <div className="form-group">
            <label>Shopify Store Domain</label>
            <input
              placeholder="mystore.myshopify.com"
              value={shopDomain}
              onChange={(e) => setShopDomain(e.target.value)}
            />
          </div>
          <button className="btn" onClick={handleShopifyConnect} disabled={loading}>
            {loading ? 'Redirecting...' : 'Connect Shopify'}
          </button>
          <div style={{ marginTop: '1rem' }}>
            <button className="btn btn-secondary" onClick={() => setStep(2)}>
              Skip (already connected) →
            </button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="card">
          <h3>Connect QuickBooks Online</h3>
          <p>You'll be redirected to Intuit to authorize access to your QuickBooks company.</p>
          <button className="btn" onClick={handleQboConnect} disabled={loading}>
            {loading ? 'Redirecting...' : 'Connect QuickBooks'}
          </button>
          <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem' }}>
            <button className="btn btn-secondary" onClick={() => setStep(1)}>← Back</button>
            <button className="btn btn-secondary" onClick={() => setStep(3)}>
              Skip (already connected) →
            </button>
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="card">
          <h3>Configure Sync</h3>
          <form onSubmit={handleCreateConnection}>
            <div className="form-group">
              <label>Connection Name</label>
              <input
                placeholder="My Store → QuickBooks"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label>Shopify Account ID (from OAuth callback response)</label>
              <input
                placeholder="UUID returned by /shopify/oauth/callback"
                value={sourceAccountId}
                onChange={(e) => setSourceAccountId(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label>QuickBooks Account ID (from OAuth callback response)</label>
              <input
                placeholder="UUID returned by /qbo/oauth/callback"
                value={destinationAccountId}
                onChange={(e) => setDestinationAccountId(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label>Timezone</label>
              <select value={timezone} onChange={(e) => setTimezone(e.target.value)}>
                {TIMEZONES.map((tz) => <option key={tz} value={tz}>{tz}</option>)}
              </select>
            </div>

            <div className="form-group">
              <label>Sync Interval</label>
              <select value={intervalHours} onChange={(e) => setIntervalHours(e.target.value)}>
                <option value={1}>Every 1 hour</option>
                <option value={4}>Every 4 hours</option>
                <option value={12}>Every 12 hours</option>
                <option value={24}>Every 24 hours</option>
              </select>
            </div>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setStep(2)}>← Back</button>
              <button className="btn" type="submit" disabled={loading}>
                {loading ? 'Creating...' : 'Create Connection'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}