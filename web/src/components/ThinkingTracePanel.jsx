import { useState, memo } from 'react';

const Section = memo(({ title, content }) => {
  const [open, setOpen] = useState(false);
  return (
    <div className="accordion-item">
      <button className="accordion-trigger" onClick={() => setOpen(o => !o)}>
        <span>{title}</span>
        <span style={{ color: 'var(--text-4)', fontSize: '1.1rem' }}>{open ? '▲' : '▼'}</span>
      </button>
      {open && (
        <div className="accordion-body animate-fade-in">
          {Array.isArray(content)
            ? content.map((c, i) => <div key={i}>• {c}</div>)
            : content}
        </div>
      )}
    </div>
  );
});

const ThinkingTracePanel = memo(({ trace }) => {
  if (!trace) return null;
  return (
    <div className="card" style={{ marginTop: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
        <span style={{ fontSize: '0.7rem', background: 'var(--blue)', color: '#fff', padding: '2px 8px', borderRadius: '999px', fontWeight: 700 }}>
          AI TRACE
        </span>
        <span style={{ fontSize: '0.8rem', color: 'var(--text-3)' }}>Gemma 4 reasoning chain</span>
      </div>
      <Section title="📡 Input Signals Detected" content={trace.inputSignals} />
      <Section title="🔍 Pattern Matched" content={trace.patternMatched} />
      <Section title="⚠️ Risk Classification" content={trace.riskClassification} />
      <Section title="📚 Evidence Base" content={trace.evidenceBase} />
    </div>
  );
});

export default ThinkingTracePanel;
