import { useState, useRef } from 'react';
import { X, Camera, CheckCircle, AlertCircle } from 'lucide-react';
import { useUiStore } from '../../stores/uiStore';
import { useAuthStore } from '../../stores/authStore';
import { usersApi } from '../../api/users';
import Avatar from '../common/Avatar';
import Spinner from '../common/Spinner';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
const MAX_SIZE = 5 * 1024 * 1024; // 5 MB — matches backend limit

export default function ProfileModal() {
  const closeModal = useUiStore((s) => s.closeModal);
  const addToast = useUiStore((s) => s.addToast);
  const user = useAuthStore((s) => s.user);

  const [displayName, setDisplayName] = useState(user?.displayName || user?.username || '');
  // avatarPreview shows either the server URL (after upload) or a local blob URL (while uploading)
  const [avatarPreview, setAvatarPreview] = useState(user?.avatarUrl || '');
  const [uploading, setUploading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState(null); // 'success' | 'error' | null
  const [saving, setSaving] = useState(false);
  const fileRef = useRef(null);

  // ── Avatar upload ─────────────────────────────────────────────────────────

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Client-side validation (mirrors backend rules)
    if (!ALLOWED_TYPES.includes(file.type)) {
      addToast('Only JPEG, PNG, GIF, and WebP images are allowed', 'error');
      return;
    }
    if (file.size > MAX_SIZE) {
      addToast('Image must be under 5 MB', 'error');
      return;
    }

    // Show a local blob preview immediately for snappy UX
    const localPreview = URL.createObjectURL(file);
    setAvatarPreview(localPreview);
    setUploading(true);
    setUploadStatus(null);

    try {
      const updatedUser = await usersApi.uploadAvatar(file);

      // Replace blob URL with the permanent server URL
      URL.revokeObjectURL(localPreview);
      const serverAvatarUrl = updatedUser.avatarUrl;
      setAvatarPreview(serverAvatarUrl);

      // Persist to auth store + localStorage so it survives page refresh
      const merged = { ...user, avatarUrl: serverAvatarUrl };
      localStorage.setItem('user', JSON.stringify(merged));
      useAuthStore.setState({ user: merged });

      setUploadStatus('success');
      addToast('Avatar updated!', 'success');
    } catch (err) {
      URL.revokeObjectURL(localPreview);
      // Roll back preview to the previously saved avatar
      setAvatarPreview(user?.avatarUrl || '');
      setUploadStatus('error');
      addToast(err.message || 'Failed to upload avatar', 'error');
    } finally {
      setUploading(false);
      // Reset file input so the same file can be re-selected after an error
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  // ── Profile save (display name only) ─────────────────────────────────────

  const handleSave = async () => {
    setSaving(true);
    try {
      await usersApi.updateMe({ displayName });
      const updatedUser = { ...user, displayName };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      useAuthStore.setState({ user: updatedUser });
      addToast('Profile updated!', 'success');
      closeModal();
    } catch (err) {
      addToast(err.message || 'Failed to update profile', 'error');
    }
    setSaving(false);
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="modal-overlay" onClick={closeModal}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>

        {/* Header */}
        <div className="modal-header">
          <span className="modal-title">Edit Profile</span>
          <button className="btn-icon" onClick={closeModal}><X size={18} /></button>
        </div>

        {/* Body */}
        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px' }}>

          {/* Avatar upload zone */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <div
              style={{ position: 'relative', cursor: uploading ? 'not-allowed' : 'pointer' }}
              onClick={() => !uploading && fileRef.current?.click()}
              title="Click to change avatar"
            >
              <Avatar name={displayName || user?.username} src={avatarPreview} size="xl" />

              {/* Camera badge / loading overlay */}
              <div style={{
                position: 'absolute', bottom: 0, right: 0,
                width: '30px', height: '30px', borderRadius: '50%',
                background: uploadStatus === 'success'
                  ? 'var(--success, #22c55e)'
                  : uploadStatus === 'error'
                    ? 'var(--error, #ef4444)'
                    : 'var(--accent-primary)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                border: '2px solid var(--bg-secondary)',
                transition: 'background 0.2s ease',
              }}>
                {uploading
                  ? <Spinner size="sm" />
                  : uploadStatus === 'success'
                    ? <CheckCircle size={14} color="white" />
                    : uploadStatus === 'error'
                      ? <AlertCircle size={14} color="white" />
                      : <Camera size={14} color="white" />
                }
              </div>

              {/* Full-avatar upload progress shimmer */}
              {uploading && (
                <div style={{
                  position: 'absolute', inset: 0, borderRadius: '50%',
                  background: 'rgba(0,0,0,0.45)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <span style={{ color: 'white', fontSize: '11px', fontWeight: 600 }}>Uploading…</span>
                </div>
              )}
            </div>

            <input
              ref={fileRef}
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              onChange={handleFileChange}
              style={{ display: 'none' }}
            />

            <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: 0, textAlign: 'center' }}>
              {uploading
                ? 'Uploading your photo…'
                : 'Click avatar to upload · JPEG, PNG, GIF, WebP · max 5 MB'}
            </p>
          </div>

          {/* Display name */}
          <div className="input-group" style={{ width: '100%' }}>
            <label className="input-label">Display Name</label>
            <input
              className="input"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Your display name"
            />
          </div>

          {/* Read-only fields */}
          <div className="input-group" style={{ width: '100%' }}>
            <label className="input-label">Username</label>
            <input className="input" value={user?.username || ''} disabled style={{ opacity: 0.5 }} />
          </div>
          <div className="input-group" style={{ width: '100%' }}>
            <label className="input-label">Email</label>
            <input className="input" value={user?.email || ''} disabled style={{ opacity: 0.5 }} />
          </div>
        </div>

        {/* Footer */}
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={closeModal}>Cancel</button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving || uploading}>
            {saving ? <Spinner size="sm" /> : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  );
}
