import { useEffect, useState } from 'react';
import { Bell, Check } from 'lucide-react';
import { useNotificationStore } from '../../stores/notificationStore';
import { formatTime } from '../../utils/formatDate';

export default function NotificationPanel() {
  const [open, setOpen] = useState(false);
  const { notifications, unreadCount, fetchNotifications, markAllRead } = useNotificationStore();

  useEffect(() => {
    if (open) fetchNotifications();
  }, [open, fetchNotifications]);

  return (
    <div style={{ position: 'relative' }}>
      <button className="btn-icon" onClick={() => setOpen(!open)} title="Notifications">
        <Bell size={18} />
        {unreadCount > 0 && (
          <span style={{
            position: 'absolute', top: '-2px', right: '-2px',
            minWidth: '16px', height: '16px', padding: '0 4px',
            background: 'var(--danger)', borderRadius: 'var(--radius-full)',
            fontSize: '10px', fontWeight: 700, color: 'white',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <>
          <div style={{ position: 'fixed', inset: 0, zIndex: 49 }} onClick={() => setOpen(false)} />
          <div className="notif-panel">
            <div style={{ padding: '12px 16px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Notifications</span>
              {unreadCount > 0 && (
                <button className="btn-ghost" style={{ fontSize: '12px', gap: '4px' }} onClick={markAllRead}>
                  <Check size={14} /> Mark all read
                </button>
              )}
            </div>
            {notifications.length === 0 ? (
              <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
                No notifications yet
              </div>
            ) : (
              notifications.map((n) => (
                <div key={n.id} className={`notif-item ${!n.read ? 'unread' : ''}`}>
                  <div className="notif-title">{n.title}</div>
                  {n.body && <div className="notif-body">{n.body}</div>}
                  <div className="notif-time">{formatTime(n.createdAt)}</div>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}
