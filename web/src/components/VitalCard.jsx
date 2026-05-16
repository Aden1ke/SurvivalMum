import { memo } from 'react';

const icons = {
  hr:   '❤️',
  spo2: '🫁',
  rr:   '🌬️',
  bp:   '💉',
  temp: '🌡️',
};

const units = {
  hr:   'bpm',
  spo2: '%',
  rr:   '/min',
  bp:   'mmHg',
  temp: '°C',
};

function getStatus(key, val) {
  if (key === 'hr')   return val > 100 || val < 60 ? 'danger' : val > 90 ? 'warn' : 'ok';
  if (key === 'spo2') return val < 94 ? 'danger' : val < 96 ? 'warn' : 'ok';
  if (key === 'rr')   return val > 24 || val < 12 ? 'danger' : val > 20 ? 'warn' : 'ok';
  if (key === 'temp') return val > 38 ? 'danger' : val > 37.5 ? 'warn' : 'ok';
  return 'ok';
}

const statusColor = { ok: 'var(--green)', warn: 'var(--amber)', danger: 'var(--red)' };

const VitalCard = memo(({ label, value, vKey }) => {
  const status = getStatus(vKey, parseFloat(value));
  const color = statusColor[status];

  return (
    <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0, height: '3px',
        background: color, borderRadius: '12px 12px 0 0',
      }} />
      <div className="label" style={{ marginBottom: 8 }}>
        {icons[vKey]} {label}
      </div>
      <div className="vital-num" style={{ color, fontSize: '2.2rem' }}>
        {value ?? '—'}
      </div>
      <div style={{ fontSize: '0.75rem', color: 'var(--text-4)', marginTop: 4 }}>
        {units[vKey]}
      </div>
    </div>
  );
});

export default VitalCard;
