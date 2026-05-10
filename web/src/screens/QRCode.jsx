import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import QRCode from 'qrcode';
import ActionButton from '../components/ActionButton';

function getPatient() {
  try { return JSON.parse(localStorage.getItem('anc_patient') || '{}'); }
  catch { return {}; }
}

const DEMO_PATIENT = {
  name: 'Fatima Musa', age: 23, lmp: '14/01/2026', ga: '28 weeks',
  gravida: 3, para: 2, condition: 'Pre-eclampsia', riskLevel: 'HIGH',
};

export default function QRCodeScreen() {
  const [tab, setTab] = useState('generate');
  const [qrUrl, setQrUrl] = useState('');
  const [scanInput, setScanInput] = useState('');
  const [decoded, setDecoded] = useState(null);
  const [toast, setToast] = useState(null);
  const canvasRef = useRef(null);

  const patient = useMemo(() => {
    const p = getPatient();
    return Object.keys(p).length > 0 ? p : DEMO_PATIENT;
  }, []);

  const qrPayload = useMemo(() => JSON.stringify({
    n: patient.name,
    a: patient.age,
    lmp: patient.lmp,
    ga: patient.ga,
    g: patient.gravida,
    p: patient.para,
    cond: patient.condition,
    risk: patient.riskLevel,
    ts: Date.now(),
  }), [patient]);

  useEffect(() => {
    if (tab !== 'generate') return;
    QRCode.toDataURL(qrPayload, { width: 240, margin: 2, color: { dark: '#000', light: '#fff' } })
      .then(setQrUrl)
      .catch(console.error);
  }, [qrPayload, tab]);

  const handleDecode = useCallback(() => {
    try {
      const obj = JSON.parse(scanInput);
      setDecoded({
        name: obj.n, age: obj.a, lmp: obj.lmp, ga: obj.ga,
        gravida: obj.g, para: obj.p, condition: obj.cond, riskLevel: obj.risk,
      });
    } catch {
      setToast('Invalid QR data — please paste valid JSON');
      setTimeout(() => setToast(null), 2500);
    }
  }, [scanInput]);

  const handleCopy = useCallback(() => {
    navigator.clipboard?.writeText(qrPayload);
    setToast('Patient data copied ✓');
    setTimeout(() => setToast(null), 2500);
  }, [qrPayload]);

  const riskColor = decoded?.riskLevel === 'HIGH' ? 'var(--red)' : decoded?.riskLevel === 'MEDIUM' ? 'var(--amber)' : 'var(--green)';

  return (
    <div className="animate-slide-up" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div>
        <h1 style={{ fontSize: '1.2rem' }}>QR Patient Card</h1>
        <p style={{ fontSize: '0.8rem', color: 'var(--text-3)', marginTop: 2 }}>Share & receive patient data</p>
      </div>

      {/* Tab Switcher */}
      <div className="lang-tabs">
        <button id="tab-generate" className={`lang-tab${tab === 'generate' ? ' active' : ''}`} onClick={() => setTab('generate')}>
          📲 Generate
        </button>
        <button id="tab-scan" className={`lang-tab${tab === 'scan' ? ' active' : ''}`} onClick={() => setTab('scan')}>
          🔍 Decode
        </button>
      </div>

      {tab === 'generate' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {/* Patient Summary */}
          <div className="card">
            <div className="label" style={{ marginBottom: 10 }}>Patient</div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, fontSize: '0.875rem' }}>
              {[
                ['Name', patient.name || '—'],
                ['Age', patient.age || '—'],
                ['LMP', patient.lmp || '—'],
                ['GA', patient.ga || '—'],
                ['Gravida', patient.gravida || '—'],
                ['Para', patient.para || '—'],
              ].map(([k, v]) => (
                <div key={k}>
                  <div style={{ color: 'var(--text-4)', fontSize: '0.72rem', fontWeight: 600, textTransform: 'uppercase' }}>{k}</div>
                  <div style={{ color: 'var(--text)', marginTop: 2 }}>{v}</div>
                </div>
              ))}
            </div>
          </div>

          {/* QR Code */}
          {qrUrl && (
            <div className="qr-container animate-fade-in">
              <img src={qrUrl} alt="Patient QR Code" width={220} height={220} style={{ display: 'block' }} />
            </div>
          )}

          <div style={{ display: 'flex', gap: 10 }}>
            <ActionButton id="btn-copy-qr" variant="ghost" fullWidth onClick={handleCopy}>
              📋 Copy Data
            </ActionButton>
            {navigator.share && (
              <ActionButton id="btn-share-qr" variant="primary" fullWidth onClick={() => navigator.share({ title: 'Patient QR', text: qrPayload })}>
                📤 Share
              </ActionButton>
            )}
          </div>
        </div>
      )}

      {tab === 'scan' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div style={{ textAlign: 'center', padding: 16, background: 'var(--surface)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: 8 }}>📷</div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-3)' }}>Camera QR scanning coming in v2. Paste QR data below to decode:</p>
          </div>

          <textarea
            id="qr-paste-input"
            className="input"
            rows={5}
            value={scanInput}
            onChange={e => setScanInput(e.target.value)}
            placeholder='Paste QR JSON data here...'
            style={{ resize: 'vertical', fontFamily: 'monospace', fontSize: '0.8rem' }}
          />

          <ActionButton id="btn-decode" variant="primary" fullWidth onClick={handleDecode} disabled={!scanInput.trim()}>
            🔍 Decode Patient Card
          </ActionButton>

          {decoded && (
            <div className="card animate-fade-in">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <h3>Decoded Patient</h3>
                {decoded.riskLevel && (
                  <span className="pill" style={{ background: `${riskColor}20`, color: riskColor, border: `1px solid ${riskColor}50` }}>
                    {decoded.riskLevel} RISK
                  </span>
                )}
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, fontSize: '0.875rem' }}>
                {Object.entries(decoded).filter(([k]) => k !== 'riskLevel').map(([k, v]) => v && (
                  <div key={k}>
                    <div style={{ color: 'var(--text-4)', fontSize: '0.72rem', fontWeight: 600, textTransform: 'uppercase' }}>{k}</div>
                    <div style={{ marginTop: 2 }}>{v}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
