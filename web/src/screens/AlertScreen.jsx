import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import ThinkingTracePanel from '../components/ThinkingTracePanel';
import ActionButton from '../components/ActionButton';
import { detectRisk } from '../ai/detectRisk';

const STEPS = [
  'Stop all unnecessary movement',
  'Place patient in left lateral position immediately',
  'Check and secure airway — ensure clear breathing',
  'Call for emergency transport NOW',
  'Start IV access if available',
  'Administer MgSO₄ if pre-eclampsia suspected',
  'Document time of onset and all vitals',
  'Do NOT leave the patient alone',
];

export default function AlertScreen() {
  const [risk, setRisk] = useState(null);
  const [acknowledged, setAcknowledged] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    detectRisk().then(setRisk);
  }, []);

  const handleDismiss = useCallback(() => {
    navigate('/referral');
  }, [navigate]);

  return (
    <div style={{
      minHeight: '100%',
      background: 'linear-gradient(180deg, #1a0005 0%, #0A0E1A 100%)',
      display: 'flex', flexDirection: 'column', gap: 16,
      paddingBottom: 80,
    }}>
      {/* Critical Header */}
      <div style={{
        background: 'linear-gradient(135deg, #7f1d1d, #450a0a)',
        padding: '20px 16px 24px',
        textAlign: 'center',
        borderBottom: '2px solid var(--red)',
        animation: 'alert-flash 2s infinite',
      }}>
        <div style={{ fontSize: '3.5rem', marginBottom: 8 }}>🚨</div>
        <h1 style={{ color: '#fff', fontSize: '1.5rem', marginBottom: 6 }}>
          {risk ? risk.condition.toUpperCase() : 'RISK DETECTED'}
        </h1>
        <div style={{ color: 'var(--red)', fontSize: '0.9rem', fontWeight: 600, letterSpacing: '0.05em' }}>
          {risk ? `${risk.level} RISK — ${Math.round(risk.confidence * 100)}% CONFIDENCE` : 'ANALYSING...'}
        </div>
      </div>

      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {/* Immediate Steps */}
        <div className="card" style={{ borderColor: 'rgba(239,68,68,0.4)' }}>
          <h2 style={{ color: 'var(--red)', marginBottom: 12, fontSize: '1rem' }}>
            ⚡ Immediate Actions
          </h2>
          {STEPS.map((step, i) => (
            <div key={i} style={{
              display: 'flex', gap: 10, padding: '8px 0',
              borderBottom: i < STEPS.length - 1 ? '1px solid var(--border)' : 'none',
            }}>
              <span style={{
                width: 24, height: 24, minWidth: 24,
                background: 'var(--red)', color: '#fff',
                borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '0.7rem', fontWeight: 700,
              }}>{i + 1}</span>
              <span style={{ fontSize: '0.875rem', color: 'var(--text-2)', lineHeight: 1.5 }}>{step}</span>
            </div>
          ))}
        </div>

        {/* AI Thinking Trace */}
        {risk && <ThinkingTracePanel trace={risk.trace} />}

        {/* Acknowledge & Act */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
            <input
              id="chk-acknowledge"
              type="checkbox"
              checked={acknowledged}
              onChange={e => setAcknowledged(e.target.checked)}
              style={{ width: 20, height: 20, accentColor: 'var(--red)', cursor: 'pointer' }}
            />
            <span style={{ fontSize: '0.875rem', color: 'var(--text-2)' }}>
              I acknowledge this alert and have initiated emergency response
            </span>
          </label>

          <ActionButton
            id="btn-proceed-referral"
            variant="danger"
            size="lg"
            fullWidth
            disabled={!acknowledged}
            onClick={handleDismiss}
          >
            Generate Referral Letter →
          </ActionButton>
        </div>
      </div>
    </div>
  );
}
