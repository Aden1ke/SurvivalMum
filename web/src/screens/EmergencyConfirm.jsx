import { useState, useCallback, useEffect } from 'react';
import ActionButton from '../components/ActionButton';

const ACTIONS = [
  { id: 'referred', label: 'Patient Referred', icon: '🚑', variant: 'primary' },
  { id: 'medication', label: 'Medication Given', icon: '💊', variant: 'success' },
  { id: 'iv', label: 'IV Access Done', icon: '🩺', variant: 'ghost' },
  { id: 'transport', label: 'Transport Called', icon: '📞', variant: 'ghost' },
  { id: 'family', label: 'Family Notified', icon: '👥', variant: 'ghost' },
];

function loadLog() {
  try { return JSON.parse(localStorage.getItem('emergency_log') || '[]'); }
  catch { return []; }
}

export default function EmergencyConfirm() {
  const [log, setLog] = useState(loadLog);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    localStorage.setItem('emergency_log', JSON.stringify(log));
  }, [log]);

  const addEvent = useCallback((label) => {
    const entry = {
      id: Date.now(),
      desc: label,
      time: new Date().toLocaleTimeString(),
      date: new Date().toLocaleDateString(),
    };
    setLog(l => [entry, ...l]);
    setToast(`Logged: ${label} ✓`);
    setTimeout(() => setToast(null), 2000);
  }, []);

  const clearLog = useCallback(() => {
    setLog([]);
    localStorage.removeItem('emergency_log');
  }, []);

  return (
    <div className="animate-slide-up" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div>
        <h1 style={{ fontSize: '1.2rem' }}>Emergency Log</h1>
        <p style={{ fontSize: '0.8rem', color: 'var(--text-3)', marginTop: 2 }}>Record actions taken — saved offline</p>
      </div>

      {/* Big Action Buttons */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {ACTIONS.map(a => (
          <button
            key={a.id}
            id={`btn-action-${a.id}`}
            onClick={() => addEvent(a.label)}
            style={{
              background: a.variant === 'primary' ? 'linear-gradient(135deg,var(--blue),var(--blue-dark))'
                        : a.variant === 'success' ? 'linear-gradient(135deg,var(--green),var(--green-dark))'
                        : 'var(--surface-2)',
              border: a.variant === 'ghost' ? '1px solid var(--border)' : 'none',
              color: '#fff',
              borderRadius: 'var(--radius-lg)',
              padding: '20px 12px',
              fontFamily: 'inherit', fontSize: '0.875rem', fontWeight: 600,
              cursor: 'pointer', textAlign: 'center',
              boxShadow: a.variant === 'primary' ? '0 4px 16px var(--blue-glow)'
                        : a.variant === 'success' ? '0 4px 16px var(--green-glow)' : 'none',
              gridColumn: a.id === 'referred' || a.id === 'medication' ? 'span 1' : 'span 1',
              transition: 'transform 150ms ease',
            }}
            onPointerDown={e => e.currentTarget.style.transform = 'scale(0.95)'}
            onPointerUp={e => e.currentTarget.style.transform = 'scale(1)'}
          >
            <div style={{ fontSize: '1.8rem', marginBottom: 8 }}>{a.icon}</div>
            {a.label}
          </button>
        ))}
      </div>

      {/* Event Log */}
      <div>
        <div className="section-header">
          <h3>Event Log</h3>
          {log.length > 0 && (
            <button onClick={clearLog} style={{
              background: 'none', border: 'none', color: 'var(--text-4)',
              fontSize: '0.8rem', cursor: 'pointer', fontFamily: 'inherit',
            }}>
              Clear
            </button>
          )}
        </div>

        {log.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--text-4)', fontSize: '0.875rem' }}>
            No events logged yet. Press an action above.
          </div>
        ) : (
          <div className="event-log">
            {log.map(e => (
              <div key={e.id} className="event-item animate-fade-in">
                <div className="event-time">{e.time}</div>
                <div className="event-desc">{e.desc}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
