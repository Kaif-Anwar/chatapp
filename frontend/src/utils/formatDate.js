import { parseApiDate } from './parseApiDate';

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

export function formatTime(dateStr) {
  if (!dateStr) return '';
  const date = parseApiDate(dateStr);
  if (!date) return '';
  const now = new Date();
  const diff = now - date;

  if (diff < MINUTE) return 'now';
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)}m`;
  if (diff < DAY) return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) return 'Yesterday';

  return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

export function formatMessageTime(dateStr) {
  if (!dateStr) return '';
  const date = parseApiDate(dateStr);
  if (!date) return '';
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export function formatDayLabel(dateStr) {
  if (!dateStr) return '';
  const date = parseApiDate(dateStr);
  if (!date) return '';
  const now = new Date();
  if (date.toDateString() === now.toDateString()) return 'Today';
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return date.toLocaleDateString([], { weekday: 'long', month: 'short', day: 'numeric' });
}

export function isSameDay(d1, d2) {
  if (!d1 || !d2) return false;
  const a = parseApiDate(d1);
  const b = parseApiDate(d2);
  if (!a || !b) return false;
  return a.toDateString() === b.toDateString();
}
