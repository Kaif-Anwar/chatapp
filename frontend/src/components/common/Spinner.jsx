export default function Spinner({ size = '' }) {
  return <div className={`spinner ${size ? `spinner-${size}` : ''}`} />;
}
