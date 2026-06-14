import { API_BASE_HOST } from '../../utils/constants';

const COLORS = [
  '#7c5cff', '#00d4ff', '#ff6b6b', '#ffa502', '#00d68f',
  '#ff4da6', '#36d399', '#a78bfa', '#f472b6', '#38bdf8',
];

function getColor(name) {
  let hash = 0;
  for (let i = 0; i < (name || '').length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return COLORS[Math.abs(hash) % COLORS.length];
}

function getInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

/**
 * Resolves avatar URLs: relative paths (from backend) are prefixed with
 * the backend host; absolute URLs (http/https/blob) are used as-is.
 */
function resolveAvatarSrc(src) {
  if (!src) return null;
  if (src.startsWith('/')) return `${API_BASE_HOST}${src}`;
  return src; // already absolute (http://, https://, blob:, data:)
}

export default function Avatar({ name, src, size = 'md', online, className = '' }) {
  const sizeClass = `avatar-${size}`;
  const resolvedSrc = resolveAvatarSrc(src);
  const bg = !resolvedSrc ? getColor(name) : undefined;

  return (
    <div className={`avatar ${sizeClass} ${className}`} style={bg ? { background: bg } : {}}>
      {resolvedSrc ? <img src={resolvedSrc} alt={name} /> : getInitials(name)}
      {online !== undefined && (
        <span className={`avatar-status ${online ? 'online' : 'offline'}`} />
      )}
    </div>
  );
}

