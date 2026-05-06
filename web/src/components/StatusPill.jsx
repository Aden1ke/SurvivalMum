import { memo } from 'react';

const StatusPill = memo(({ status, label }) => {
  const classMap = {
    ACTIVE:    'pill pill-green',
    SAFE:      'pill pill-green',
    HIGH:      'pill pill-red',
    MEDIUM:    'pill pill-amber',
    LOW:       'pill pill-green',
    ALERT:     'pill pill-red',
    SCANNING:  'pill pill-blue',
    IDLE:      'pill pill-blue',
  };
  const dotMap = {
    ACTIVE: true, ALERT: true, SCANNING: true,
  };

  return (
    <span className={classMap[status] || 'pill pill-blue'}>
      {dotMap[status] && <span className="live-dot" style={{ width: 6, height: 6 }} />}
      {label || status}
    </span>
  );
});

export default StatusPill;
