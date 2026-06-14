import Avatar from '../common/Avatar';
import { Settings, LogOut } from 'lucide-react';
import { useAuthStore } from '../../stores/authStore';
import { useUiStore } from '../../stores/uiStore';
import { useNavigate } from 'react-router-dom';

export default function UserProfileStrip() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const openModal = useUiStore((s) => s.openModal);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!user) return null;

  return (
    <div className="profile-strip">
      <Avatar name={user.displayName || user.username || user.email} src={user.avatarUrl} size="md" />
      <div className="profile-strip-info">
        <div className="profile-strip-name">{user.username}</div>
        <div className="profile-strip-email">{user.email}</div>
      </div>
      <button className="btn-icon" onClick={() => openModal('profile')} title="Settings">
        <Settings size={18} />
      </button>
      <button className="btn-icon" onClick={handleLogout} title="Logout">
        <LogOut size={18} />
      </button>
    </div>
  );
}
