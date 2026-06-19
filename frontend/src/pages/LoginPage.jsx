import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import Spinner from '../components/common/Spinner';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { login, loading, error, clearError } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    const ok = await login({ email, password });
    if (ok) navigate('/chat');
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-orb" />
      <div className="auth-bg-orb" />
      <div className="auth-bg-orb" />

      <div className="auth-card">
        <div className="auth-logo"><span>💬 ChatApp</span></div>
        <p className="auth-subtitle">Welcome back! Sign in to continue</p>

        {error && <div className="auth-alert">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">Email</label>
            <input
              id="login-email"
              className="input"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => { setEmail(e.target.value); clearError(); }}
              required
            />
          </div>

          <div className="input-group">
            <label className="input-label">Password</label>
            <input
              id="login-password"
              className="input"
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); clearError(); }}
              required
            />
          </div>

          <button id="login-submit" className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? <Spinner size="sm" /> : 'Sign In'}
          </button>
        </form>

        <p className="auth-footer">
          <Link to="/forgot-password">Forgot password?</Link>
        </p>
        <p className="auth-footer">
          Don't have an account? <Link to="/register">Create one</Link>
        </p>
      </div>
    </div>
  );
}
