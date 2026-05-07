import { useState, useEffect, useCallback, useRef, memo } from 'react';
import { classifyCry } from '../ai/classifyCry';
import StatusPill from '../components/StatusPill';
import ActionButton from '../components/ActionButton';
import { useLiteRT } from '../context/LiteRTContext';

const BARS = Array.from({ length: 20 }, (_, i) => i);

// Isolated Waveform Component to prevent full-screen re-renders during animation
const Waveform = memo(({ analysing, color }) => (
  <div className="waveform" style={{ marginBottom: 16, justifyContent: 'center' }}>
    {BARS.map(i => (
      <div
        key={i}
        className="wave-bar"
        style={{
          background: analysing ? 'var(--blue)' : color,
          animationName: 'waveform',
          animationDuration: `${0.4 + (i % 5) * 0.12}s`,
          animationTimingFunction: 'ease-in-out',
          animationIterationCount: 'infinite',
          animationDelay: `${i * 0.05}s`,
        }}
      />
    ))}
  </div>
));

export default function NewbornMonitor() {
  const { status } = useLiteRT();
  const [result, setResult] = useState(null);
  const [analysing, setAnalysing] = useState(false);
  const intervalRef = useRef(null);

  const runAnalysis = useCallback(async () => {
    if (status !== 'ready') return;
    setAnalysing(true);
    const r = await classifyCry();
    setResult(r);
    setAnalysing(false);
  }, [status]);

  useEffect(() => {
    if (status !== 'ready') return;
    runAnalysis();
    intervalRef.current = setInterval(runAnalysis, 8000);
    return () => clearInterval(intervalRef.current);
  }, [runAnalysis, status]);

  const cryColor = result?.cry?.color || 'var(--blue)';

  return (
    <div className="animate-slide-up" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div>
        <h1 style={{ fontSize: '1.2rem' }}>Newborn Monitor</h1>
        <p style={{ fontSize: '0.8rem', color: 'var(--text-3)', marginTop: 2 }}>AI cry & breathing analysis</p>
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
          <h3>Cry Analysis</h3>
          {analysing
            ? <StatusPill status="SCANNING" label="Listening..." />
            : result && <StatusPill status={result.cry.flag ? 'ALERT' : 'ACTIVE'} label="LITERT LIVE" />}
        </div>

        <Waveform analysing={analysing} color={cryColor} />

        {result ? (
          <div style={{ textAlign: 'center' }} className="animate-fade-in">
            <div style={{ fontSize: '1.6rem', fontWeight: 700, color: cryColor }}>{result.cry.type}</div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-3)', marginTop: 4 }}>
              {Math.round(result.cry.confidence * 100)}% confidence
            </div>
            {result.cry.flag && (
              <div style={{ marginTop: 10, padding: '8px 14px', background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 'var(--radius-sm)', fontSize: '0.875rem', color: 'var(--red)' }}>
                ⚠️ {result.cry.flag}
              </div>
            )}
          </div>
        ) : (
          <div style={{ textAlign: 'center', color: 'var(--text-4)', fontSize: '0.875rem' }}>
            {status === 'ready' ? 'Analysing cry pattern...' : 'Waiting for LiteRT engine...'}
          </div>
        )}
      </div>

      <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <div className="label">🌬️ Breathing Rate</div>
          <div className="vital-num" style={{ fontSize: '2.4rem', color: result ? (result.breathingRate > 60 || result.breathingRate < 30 ? 'var(--red)' : 'var(--green)') : 'var(--text)' }}>
            {result?.breathingRate ?? '—'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-4)' }}>breaths/min</div>
        </div>
        <div style={{ fontSize: '0.8rem', color: 'var(--text-3)', textAlign: 'right', maxWidth: 120 }}>
          Normal: 30–60<br/>breaths/min<br/>
          {result && (
            <span style={{ color: result.breathingRate > 60 || result.breathingRate < 30 ? 'var(--red)' : 'var(--green)', fontWeight: 600 }}>
              {result.breathingRate > 60 || result.breathingRate < 30 ? '⚠️ Abnormal' : '✓ Normal'}
            </span>
          )}
        </div>
      </div>

      {result && (
        <div className="card animate-fade-in">
          <div className="section-header">
            <h3>Jaundice Assessment</h3>
            <span className={`pill ${result.jaundice.risk === 'HIGH' ? 'pill-red' : result.jaundice.risk.includes('MEDIUM') ? 'pill-amber' : 'pill-green'}`}>
              {result.jaundice.risk}
            </span>
          </div>
          <div className="skin-swatch" style={{ background: result.jaundice.color, marginBottom: 12 }} />
          <div style={{ fontWeight: 600, marginBottom: 4 }}>{result.jaundice.level} Jaundice</div>
          <div style={{ fontSize: '0.875rem', color: 'var(--text-3)', lineHeight: 1.6 }}>
            {result.jaundice.advice}
          </div>
        </div>
      )}

      <ActionButton variant="ghost" fullWidth onClick={runAnalysis} disabled={analysing || status !== 'ready'}>
        {analysing ? '⏳ Analysing...' : '🔄 Re-analyse Now'}
      </ActionButton>
    </div>
  );
}
