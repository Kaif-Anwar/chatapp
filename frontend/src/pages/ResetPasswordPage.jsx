import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import Spinner from '../components/common/Spinner';

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get('token') || '';
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!token) { setError('Invalid reset link'); return; }
    setLoading(true);
    setError('');
    try {
      await authApi.resetPassword({ token, newPassword: password });
      navigate('/login');
    } catch (err) {
      setError(err.message || 'Reset failed');
    }
    setLoading(false);
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-orb" /><div className="auth-bg-orb" /><div className="auth-bg-orb" />
      <div className="auth-card">
        <div className="auth-logo"><span>💬 ChatApp</span></div>
        <p className="auth-subtitle">Choose a new password</p>
        {error && <div className="auth-alert">{error}</div>}
        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">New password</label>
            <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
          </div>
          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? <Spinner size="sm" /> : 'Update password'}
          </button>
        </form>
        <p className="auth-footer"><Link to="/login">Back to sign in</Link></p>
      </div>
    </div>
  );
}
