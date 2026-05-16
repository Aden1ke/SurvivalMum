import { useState, useCallback } from 'react';
import { scanANCCard } from '../ai/scanANCCard';
import ActionButton from '../components/ActionButton';
import StatusPill from '../components/StatusPill';

const FIELDS = [
  { key: 'name',       label: 'Patient Name',      type: 'text' },
  { key: 'age',        label: 'Age',                type: 'number' },
  { key: 'lmp',        label: 'Last Menstrual Period', type: 'text' },
  { key: 'ga',         label: 'Gestational Age',   type: 'text' },
  { key: 'gravida',    label: 'Gravida',            type: 'number' },
  { key: 'para',       label: 'Para',               type: 'number' },
  { key: 'bp_history', label: 'BP History',         type: 'text' },
];

export default function ANCScanner() {
  const [scanning, setScanning] = useState(false);
  const [data, setData] = useState(null);
  const [saved, setSaved] = useState(false);
  const [toast, setToast] = useState(null);

  const handleScan = useCallback(async () => {
    setScanning(true);
    setSaved(false);
    try {
      const result = await scanANCCard();
      setData(result);
    } finally {
      setScanning(false);
    }
  }, []);

  const handleChange = useCallback((key, val) => {
    setData(d => ({ ...d, [key]: val }));
  }, []);

  const handleSave = useCallback(() => {
    if (!data) return;
    localStorage.setItem('anc_patient', JSON.stringify(data));
    setSaved(true);
    setToast('Patient record saved ✓');
    setTimeout(() => setToast(null), 2500);
  }, [data]);

  return (
    <div className="animate-slide-up" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div>
        <h1 style={{ fontSize: '1.2rem' }}>ANC Card Scanner</h1>
        <p style={{ fontSize: '0.8rem', color: 'var(--text-3)', marginTop: 2 }}>Scan antenatal card with AI</p>
      </div>

      {/* Capture Zone */}
      <div style={{
        border: '2px dashed var(--border)', borderRadius: 'var(--radius-lg)',
        padding: '32px 16px', textAlign: 'center',
        background: scanning ? 'rgba(59,130,246,0.05)' : 'transparent',
        transition: 'background 300ms',
      }}>
        {scanning ? (
          <>
            <div style={{ fontSize: '2.5rem', marginBottom: 8 }}>🔍</div>
            <StatusPill status="SCANNING" label="AI Reading Card..." />
            <div style={{ fontSize: '0.8rem', color: 'var(--text-4)', marginTop: 10 }}>
              Extracting fields with Gemma 4...
            </div>
          </>
        ) : (
          <>
            <div style={{ fontSize: '3rem', marginBottom: 12 }}>📋</div>
            <p style={{ color: 'var(--text-3)', fontSize: '0.875rem', marginBottom: 16 }}>
              Point camera at ANC card or upload image
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap' }}>
              <ActionButton id="btn-scan-camera" variant="primary" onClick={handleScan}>
                📸 Scan Card
              </ActionButton>
              <ActionButton id="btn-scan-upload" variant="ghost" onClick={handleScan}>
                📁 Upload
              </ActionButton>
            </div>
          </>
        )}
      </div>

      {/* Editable Fields */}
      {data && (
        <div className="card animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <h3>Extracted Fields</h3>
            <StatusPill status="ACTIVE" label="AI FILLED" />
          </div>

          {FIELDS.map(f => (
            <div key={f.key}>
              <label className="label" style={{ display: 'block', marginBottom: 4 }}>{f.label}</label>
              <input
                id={`anc-${f.key}`}
                className="input"
                type={f.type}
                value={data[f.key] ?? ''}
                onChange={e => handleChange(f.key, e.target.value)}
              />
            </div>
          ))}

          {/* Flags */}
          {data.flags && data.flags.length > 0 && data.flags[0] !== 'None' && (
            <div style={{ background: 'rgba(245,158,11,0.1)', border: '1px solid rgba(245,158,11,0.3)', borderRadius: 'var(--radius-sm)', padding: '10px 14px' }}>
              <div className="label" style={{ marginBottom: 6 }}>⚠️ AI Flags</div>
              {data.flags.map((f, i) => (
                <div key={i} style={{ fontSize: '0.875rem', color: 'var(--amber)' }}>• {f}</div>
              ))}
            </div>
          )}

          <ActionButton id="btn-save-anc" variant="success" fullWidth onClick={handleSave}>
            {saved ? '✓ Saved' : '💾 Confirm & Save'}
          </ActionButton>
        </div>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
