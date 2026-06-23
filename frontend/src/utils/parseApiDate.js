/**
 * Parse API timestamps. The backend stores UTC but historically serialized
 * LocalDateTime without a zone; naive strings must be treated as UTC.
 */
export function parseApiDate(dateStr) {
  if (dateStr == null || dateStr === '') return null;

  if (dateStr instanceof Date) {
    return Number.isNaN(dateStr.getTime()) ? null : dateStr;
  }

  if (typeof dateStr === 'number') {
    const date = new Date(dateStr);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  const text = String(dateStr).trim();
  if (!text) return null;

  const hasZone = /[zZ]$|[+-]\d{2}:\d{2}$/.test(text);
  const date = new Date(hasZone ? text : `${text}Z`);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function parseApiDateMs(dateStr) {
  const date = parseApiDate(dateStr);
  return date ? date.getTime() : NaN;
}
