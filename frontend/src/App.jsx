import { useState, useEffect } from 'react';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
// import Dashboard from './pages/Dashboard';
// import ConnectionWizard from './pages/ConnectionWizard';
// import ConnectionDetail from './pages/ConnectionDetail';
import './App.css';

function App() {
  const [page, setPage] = useState('login');
  const [selectedConnectionId, setSelectedConnectionId] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    if (token) setPage('dashboard');
  }, []);

  const navigate = (target, connectionId = null) => {
    if (connectionId) setSelectedConnectionId(connectionId);
    setPage(target);
  };

  const logout = () => {
    localStorage.removeItem('jwt_token');
    setPage('login');
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>SyncPlatform</h1>
        {page !== 'login' && page !== 'signup' && (
          <nav>
            <button onClick={() => navigate('dashboard')}>Dashboard</button>
            <button onClick={logout}>Logout</button>
          </nav>
        )}
      </header>

      <main className="app-main">
        {page === 'login' && <LoginPage onLogin={() => navigate('dashboard')} onGoSignup={() => navigate('signup')} />}
        {page === 'signup' && <SignupPage onGoLogin={() => navigate('login')} />}
      </main>
    </div>
  );
}

export default App;