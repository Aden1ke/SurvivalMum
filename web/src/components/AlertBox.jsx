import { memo } from 'react';

const AlertBox = memo(({ level = 'info', title, children }) => {
  const styles = {
    HIGH:    { border: 'var(--red)',   bg: 'rgba(239,68,68,0.1)',   icon: '🚨' },
    MEDIUM:  { border: 'var(--amber)', bg: 'rgba(245,158,11,0.1)',  icon: '⚠️' },
    LOW:     { border: 'var(--green)', bg: 'rgba(16,185,129,0.1)',  icon: '✅' },
    info:    { border: 'var(--blue)',  bg: 'rgba(59,130,246,0.1)',  icon: 'ℹ️' },
  };
  const s = styles[level] || styles.info;

  return (
    <div style={{
      background: s.bg,
      border: `1px solid ${s.border}`,
      borderRadius: 'var(--radius-md)',
      padding: '14px 16px',
    }}>
      {title && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <span style={{ fontSize: '1.1rem' }}>{s.icon}</span>
          <strong style={{ fontSize: '0.95rem', color: s.border }}>{title}</strong>
        </div>
      )}
      <div style={{ fontSize: '0.875rem', color: 'var(--text-2)', lineHeight: 1.6 }}>
        {children}
      </div>
    </div>
  );
});

export default AlertBox;
