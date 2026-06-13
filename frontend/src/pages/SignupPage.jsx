import { useState } from 'react';
import { signup, verifyEmail } from '../api/auth';

export default function SignupPage({ onGoLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [verifyToken, setVerifyToken] = useState('');
  const [verified, setVerified] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      await signup(email, password, fullName);
      setMessage('Signup successful! Check the auth-service logs for your verification link, then paste the token below.');
    } catch (err) {
      setError(err.response?.data?.message || 'Signup failed');
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await verifyEmail(verifyToken);
      setVerified(true);
      setMessage('Email verified! You can now log in.');
    } catch (err) {
      setError(err.response?.data?.message || 'Verification failed');
    }
  };

  return (
    <div className="card" style={{ maxWidth: 450, margin: '2rem auto' }}>
      <h2>Sign up</h2>
      {error && <div className="error-banner">{error}</div>}
      {message && <div className="card" style={{ background: '#eef2ff' }}>{message}</div>}

      {!verified && !message && (
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Full Name</label>
            <input value={fullName} onChange={e => setFullName(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Sign up</button>
        </form>
      )}

      {message && !verified && (
        <form onSubmit={handleVerify} style={{ marginTop: '1rem' }}>
          <div className="form-group">
            <label>Verification Token</label>
            <input value={verifyToken} onChange={e => setVerifyToken(e.target.value)} required />
          </div>
          <button className="btn" type="submit">Verify Email</button>
        </form>
      )}

      {verified && (
        <button className="btn" onClick={onGoLogin}>Go to Login</button>
      )}

      {!message && (
        <p style={{ marginTop: '1rem' }}>
          Already have an account? <a href="#" onClick={onGoLogin}>Log in</a>
        </p>
      )}
    </div>
  );
}