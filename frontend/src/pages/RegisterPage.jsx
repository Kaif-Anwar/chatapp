import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import Spinner from '../components/common/Spinner';

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '', displayName: '' });
  const { register, loading, error, clearError } = useAuthStore();
  const navigate = useNavigate();

  const update = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    clearError();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const ok = await register(form);
    if (ok) navigate('/chat');
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-orb" />
      <div className="auth-bg-orb" />
      <div className="auth-bg-orb" />

      <div className="auth-card">
        <div className="auth-logo"><span>💬 ChatApp</span></div>
        <p className="auth-subtitle">Create your account to get started</p>

        {error && <div className="auth-alert">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">Username</label>
            <input id="reg-username" className="input" placeholder="Pick a username" value={form.username} onChange={update('username')} required />
          </div>

          <div className="input-group">
            <label className="input-label">Display Name (optional)</label>
            <input id="reg-display" className="input" placeholder="How others will see you" value={form.displayName} onChange={update('displayName')} />
          </div>

          <div className="input-group">
            <label className="input-label">Email</label>
            <input id="reg-email" className="input" type="email" placeholder="you@example.com" value={form.email} onChange={update('email')} required />
          </div>

          <div className="input-group">
            <label className="input-label">Password</label>
            <input id="reg-password" className="input" type="password" placeholder="At least 6 characters" value={form.password} onChange={update('password')} required minLength={6} />
          </div>

          <button id="reg-submit" className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? <Spinner size="sm" /> : 'Create Account'}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
