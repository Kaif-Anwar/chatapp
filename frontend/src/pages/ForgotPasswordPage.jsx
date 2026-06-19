import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/auth';
import Spinner from '../components/common/Spinner';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await authApi.forgotPassword({ email });
      setSent(true);
    } catch (err) {
      setError(err.message || 'Request failed');
    }
    setLoading(false);
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-orb" /><div className="auth-bg-orb" /><div className="auth-bg-orb" />
      <div className="auth-card">
        <div className="auth-logo"><span>💬 ChatApp</span></div>
        <p className="auth-subtitle">Reset your password</p>
        {error && <div className="auth-alert">{error}</div>}
        {sent ? (
          <p style={{ color: 'var(--text-secondary)', fontSize: '14px', textAlign: 'center' }}>
            If an account exists for that email, a reset link has been sent.
          </p>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="input-group">
              <label className="input-label">Email</label>
              <input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? <Spinner size="sm" /> : 'Send reset link'}
            </button>
          </form>
        )}
        <p className="auth-footer"><Link to="/login">Back to sign in</Link></p>
      </div>
    </div>
  );
}
