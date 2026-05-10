import { useState, useEffect, useRef, useCallback, memo } from 'react';
import { useNavigate } from 'react-router-dom';
import VitalCard from '../components/VitalCard';
import StatusPill from '../components/StatusPill';
import { generateVitals } from '../ai/generateVitals';
import { useLiteRT } from '../context/LiteRTContext';

// Static UI Data moved outside to prevent re-creation
const CORNER_POSITIONS = ['0 0', '0 auto', 'auto 0', 'auto auto'];

const CornerBrackets = memo(() => (
  <>
    {CORNER_POSITIONS.map((pos, i) => (
      <div key={i} style={{
        position: 'absolute',
        top: pos.split(' ')[0] !== 'auto' ? 12 : 'auto',
        bottom: pos.split(' ')[0] === 'auto' ? 12 : 'auto',
        left: pos.split(' ')[1] !== 'auto' ? 12 : 'auto',
        right: pos.split(' ')[1] === 'auto' ? 12 : 'auto',
        width: 20, height: 20,
        borderTop: i < 2 ? '2px solid var(--blue)' : 'none',
        borderBottom: i >= 2 ? '2px solid var(--blue)' : 'none',
        borderLeft: i % 2 === 0 ? '2px solid var(--blue)' : 'none',
        borderRight: i % 2 === 1 ? '2px solid var(--blue)' : 'none',
      }} />
    ))}
  </>
));

export default function CameraMonitor() {
  const { status } = useLiteRT();
  const [vitals, setVitals] = useState(() => generateVitals());
  const [recording, setRecording] = useState(false);
  const [alertTriggered, setAlertTriggered] = useState(false);
  const intervalRef = useRef(null);
  const navigate = useNavigate();

  // Optimized Vitals Loop
  useEffect(() => {
    if (status !== 'ready') return;

    intervalRef.current = setInterval(() => {
      setVitals(generateVitals(alertTriggered ? 'HIGH' : 'NORMAL'));
    }, 2000);
    
    return () => clearInterval(intervalRef.current);
  }, [alertTriggered, status]);

  // Auto-trigger alert simulation
  useEffect(() => {
    const timer = setTimeout(() => setAlertTriggered(true), 12000);
    return () => clearTimeout(timer);
  }, []);

  const handleMic = useCallback(() => setRecording(r => !r), []);
  const handleTestAlert = useCallback(() => navigate('/alert'), [navigate]);

  return (
    <div className="animate-slide-up" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 style={{ fontSize: '1.2rem' }}>Camera Monitor</h1>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-3)', marginTop: 2 }}>Live maternal vitals via AI</p>
        </div>
        <StatusPill status={status === 'ready' ? 'ACTIVE' : 'IDLE'} label={status === 'ready' ? 'LITERT READY' : 'INIT ENGINE...'} />
      </div>

      {/* Camera Feed - Memoized overlay */}
      <div className="camera-feed" id="camera-feed">
        <div className="camera-scan-line" />
        <div style={{ textAlign: 'center', zIndex: 1 }}>
          <div style={{ fontSize: '3rem', marginBottom: 8 }}>👁️</div>
          <div style={{ fontSize: '0.875rem', color: 'var(--blue)', fontWeight: 600 }}>
            {status === 'ready' ? 'AI Watching' : 'Engine Loading...'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-4)', marginTop: 4 }}>
            Analysing maternal signals...
          </div>
        </div>
        
        <CornerBrackets />

        {alertTriggered && (
          <div style={{
            position: 'absolute', inset: 0,
            background: 'rgba(239,68,68,0.15)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            animation: 'blink 0.8s infinite',
          }}>
            <span style={{ fontSize: '0.875rem', color: 'var(--red)', fontWeight: 700 }}>⚠️ RISK DETECTED</span>
          </div>
        )}
      </div>

      {/* Vitals Grid - Individual cards are memoized */}
      <div className="vitals-grid">
        <VitalCard label="Heart Rate" value={vitals.hr} vKey="hr" />
        <VitalCard label="SpO₂" value={vitals.spo2} vKey="spo2" />
        <VitalCard label="Breathing" value={vitals.rr} vKey="rr" />
        <VitalCard label="Temperature" value={vitals.temp} vKey="temp" />
      </div>

      {/* BP Full width - Simple card, minimal logic */}
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div className="label">💉 Blood Pressure</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 700, color: 'var(--text)', marginTop: 4 }}>{vitals.bp}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-4)' }}>mmHg</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="label">Last update</div>
          <div style={{ fontSize: '0.875rem', color: 'var(--text-3)', marginTop: 4 }}>
            {new Date(vitals.ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
          </div>
        </div>
      </div>

      <button id="btn-test-alert" onClick={handleTestAlert}
        style={{
          background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)',
          color: 'var(--red)', borderRadius: 'var(--radius-md)', padding: '10px 16px',
          fontFamily: 'inherit', fontSize: '0.875rem', fontWeight: 600, cursor: 'pointer',
          width: '100%',
        }}>
        🚨 Test Alert Screen
      </button>

      <button id="btn-voice" className={`fab${recording ? ' recording' : ''}`} onClick={handleMic}>
        🎤
      </button>
    </div>
  );
}
