import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/auth';
import Spinner from '../components/common/Spinner';

export default function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [status, setStatus] = useState('loading');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!token) { setStatus('error'); setError('Invalid verification link'); return; }
    authApi.verifyEmail({ token })
      .then(() => setStatus('success'))
      .catch((err) => { setStatus('error'); setError(err.message || 'Verification failed'); });
  }, [token]);

  return (
    <div className="auth-page">
      <div className="auth-bg-orb" /><div className="auth-bg-orb" /><div className="auth-bg-orb" />
      <div className="auth-card">
        <div className="auth-logo"><span>💬 ChatApp</span></div>
        {status === 'loading' && <div style={{ display: 'flex', justifyContent: 'center', padding: '24px' }}><Spinner /></div>}
        {status === 'success' && (
          <>
            <p className="auth-subtitle">Email verified! You can now sign in.</p>
            <Link to="/login" className="btn btn-primary" style={{ textAlign: 'center' }}>Sign in</Link>
          </>
        )}
        {status === 'error' && (
          <>
            <div className="auth-alert">{error}</div>
            <p className="auth-footer"><Link to="/login">Back to sign in</Link></p>
          </>
        )}
      </div>
    </div>
  );
}
