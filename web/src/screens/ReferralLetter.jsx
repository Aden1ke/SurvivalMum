import { useState, useMemo, useCallback } from 'react';
import { generateReferral } from '../ai/generateReferral';

const LANGS = ['EN', 'HA', 'YO', 'IG'];
const LANG_LABELS = {
  EN: '🇬🇧 English',
  HA: '🇳🇬 Hausa',
  YO: '🇳🇬 Yoruba',
  IG: '🇳🇬 Igbo',
};

/** Fallback demo patient shown when no real ANC data exists */
const DEMO_PATIENT = {
  name: 'Aisha Musa',
  age: '27',
  gravida: '2',
  para: '1',
  bp: '160/110',
  hr: '102',
  spo2: '94',
  temp: '37.8',
  lmp: '12/08/2024',
  ga: '34 weeks',
  riskLevel: 'HIGH',
  condition: 'Severe Pre-eclampsia',
  confidence: 0.91,
};

function getPatient() {
  try {
    const stored = localStorage.getItem('anc_patient');
    if (stored) {
      const parsed = JSON.parse(stored);
      // Only use stored data if it has meaningful content
      if (parsed && Object.keys(parsed).length > 0 && parsed.name) return { data: parsed, isDemo: false };
    }
  } catch { /* ignore */ }
  return { data: DEMO_PATIENT, isDemo: true };
}

/* ── SVG Icons ──────────────────────────────────────────── */
function WhatsAppIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893A11.821 11.821 0 0020.885 3.49"/>
    </svg>
  );
}

function SMSIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
    </svg>
  );
}

function CopyIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
    </svg>
  );
}

function PrintIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" width="18" height="18">
      <polyline points="6 9 6 2 18 2 18 9"/>
      <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/>
      <rect x="6" y="14" width="12" height="8"/>
    </svg>
  );
}

function DocumentIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" width="20" height="20">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
      <polyline points="14 2 14 8 20 8"/>
      <line x1="16" y1="13" x2="8" y2="13"/>
      <line x1="16" y1="17" x2="8" y2="17"/>
      <polyline points="10 9 9 9 8 9"/>
    </svg>
  );
}

export default function ReferralLetter() {
  const [lang, setLang] = useState('EN');
  const [toast, setToast] = useState(null);
  const [copyDone, setCopyDone] = useState(false);

  const { data: patient, isDemo } = useMemo(() => getPatient(), []);
  const referral = useMemo(() => generateReferral(patient, lang), [patient, lang]);

  const showToast = useCallback((msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  }, []);

  const handleShare = useCallback(() => {
    if (navigator.share) {
      navigator.share({ title: 'SurviveMum Referral', text: referral.text });
    } else {
      navigator.clipboard?.writeText(referral.text).then(() => {
        setCopyDone(true);
        setTimeout(() => setCopyDone(false), 2000);
        showToast('Letter copied to clipboard ✓');
      });
    }
  }, [referral.text, showToast]);

  const handleWhatsApp = useCallback(() => {
    const encoded = encodeURIComponent(referral.text);
    window.open(`https://wa.me/?text=${encoded}`, '_blank');
  }, [referral.text]);

  const handleSMS = useCallback(() => {
    const encoded = encodeURIComponent(referral.text.slice(0, 500));
    window.open(`sms:?body=${encoded}`, '_blank');
  }, [referral.text]);

  const handlePrint = useCallback(() => {
    const w = window.open('', '_blank');
    if (!w) return;
    w.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>SurviveMum Referral Letter</title>
          <style>
            body { font-family: 'Georgia', serif; padding: 40px; max-width: 640px; margin: 0 auto; color: #111; }
            pre { white-space: pre-wrap; font-family: inherit; font-size: 14px; line-height: 1.8; }
            header { border-bottom: 2px solid #333; padding-bottom: 16px; margin-bottom: 24px; }
            .badge { display: inline-block; background: #fee2e2; color: #991b1b; padding: 4px 12px; border-radius: 4px; font-size: 13px; font-weight: 700; }
          </style>
        </head>
        <body>
          <header>
            <div style="font-size:12px;color:#666;margin-bottom:4px">🫀 SurviveMum AI — Maternal Health System</div>
            <div class="badge">${referral.urgency} PRIORITY</div>
          </header>
          <pre>${referral.text}</pre>
        </body>
      </html>
    `);
    w.document.close();
    w.print();
    showToast('Print dialog opened');
  }, [referral.text, referral.urgency, showToast]);

  const urgencyColor = referral.urgency === 'CRITICAL' ? 'var(--red)' : 'var(--amber)';
  const urgencyBg = referral.urgency === 'CRITICAL'
    ? 'rgba(239,68,68,0.12)'
    : 'rgba(245,158,11,0.12)';
  const urgencyBorder = referral.urgency === 'CRITICAL'
    ? 'rgba(239,68,68,0.3)'
    : 'rgba(245,158,11,0.3)';

  return (
    <div className="animate-slide-up" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
        <div>
          <h1 style={{ fontSize: '1.25rem', marginBottom: 2 }}>Referral Letter</h1>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-3)' }}>AI-generated · {new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}</p>
        </div>
        <span style={{
          display: 'inline-flex', alignItems: 'center', gap: 6,
          fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.06em',
          background: urgencyBg, color: urgencyColor,
          border: `1px solid ${urgencyBorder}`,
          padding: '5px 12px', borderRadius: '999px', whiteSpace: 'nowrap', flexShrink: 0,
        }}>
          ⚡ {referral.urgency}
        </span>
      </div>

      {/* Demo data notice */}
      {isDemo && (
        <div style={{
          background: 'rgba(59,130,246,0.08)',
          border: '1px solid rgba(59,130,246,0.25)',
          borderRadius: 'var(--radius-sm)',
          padding: '10px 14px',
          display: 'flex', gap: 10, alignItems: 'flex-start',
        }}>
          <span style={{ fontSize: '1rem', flexShrink: 0 }}>ℹ️</span>
          <div>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--blue)', marginBottom: 2 }}>Demo Patient Data</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-3)', lineHeight: 1.5 }}>
              No patient scanned yet. Go to <strong>ANC</strong> tab to scan a card and save real patient data.
            </div>
          </div>
        </div>
      )}

      {/* Patient Summary Strip */}
      <div className="card" style={{ padding: '12px 14px' }}>
        <div className="label" style={{ marginBottom: 8 }}>Patient Summary</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 12px' }}>
          {[
            ['Name', patient.name || 'Unknown'],
            ['Age', patient.age ? `${patient.age} yrs` : '—'],
            ['G/P', `G${patient.gravida || '?'}P${patient.para || '?'}`],
            ['GA', patient.ga || '—'],
            ['BP', patient.bp || '—'],
            ['SpO₂', patient.spo2 ? `${patient.spo2}%` : '—'],
          ].map(([label, value]) => (
            <div key={label} style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              <span style={{ fontSize: '0.65rem', color: 'var(--text-4)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' }}>{label}</span>
              <span style={{ fontSize: '0.85rem', color: 'var(--text)', fontWeight: 500 }}>{value}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Language Selector */}
      <div>
        <div className="label" style={{ marginBottom: 8 }}>Letter Language</div>
        <div className="lang-tabs">
          {LANGS.map(l => (
            <button
              key={l}
              id={`lang-tab-${l}`}
              className={`lang-tab${lang === l ? ' active' : ''}`}
              onClick={() => setLang(l)}
            >
              {LANG_LABELS[l]}
            </button>
          ))}
        </div>
      </div>

      {/* Letter Body */}
      <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
        {/* Letterhead */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
          <div style={{
            width: 40, height: 40, borderRadius: 10,
            background: 'linear-gradient(135deg, var(--blue), var(--purple))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
          }}>
            <DocumentIcon />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text)' }}>Medical Referral Document</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-4)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {referral.facility}
            </div>
          </div>
          <span style={{
            fontSize: '0.68rem', fontWeight: 700, padding: '3px 8px',
            borderRadius: 6, flexShrink: 0,
            background: urgencyBg, color: urgencyColor, border: `1px solid ${urgencyBorder}`,
          }}>
            {referral.urgency}
          </span>
        </div>

        <div className="divider" />

        {/* Letter text */}
        <pre className="letter-body" style={{
          fontSize: '0.825rem',
          lineHeight: 1.85,
          color: 'var(--text-2)',
          whiteSpace: 'pre-wrap',
          fontFamily: "'Inter', system-ui, monospace",
          userSelect: 'text',
          WebkitUserSelect: 'text',
        }}>
          {referral.text}
        </pre>

        {/* Decorative corner accent */}
        <div style={{
          position: 'absolute', bottom: 0, right: 0,
          width: 60, height: 60,
          background: `radial-gradient(circle at bottom right, ${urgencyBg}, transparent 70%)`,
          pointerEvents: 'none',
        }} />
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {/* Primary: WhatsApp */}
        <button
          id="btn-whatsapp"
          onClick={handleWhatsApp}
          className="btn btn-full"
          style={{
            background: 'linear-gradient(135deg, #25D366, #128C7E)',
            color: '#fff',
            boxShadow: '0 4px 16px rgba(37,211,102,0.3)',
            fontSize: '0.95rem',
            padding: '14px 20px',
            borderRadius: 'var(--radius-md)',
          }}
        >
          <WhatsAppIcon />
          Send via WhatsApp
        </button>

        {/* Secondary row */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
          <button
            id="btn-sms"
            onClick={handleSMS}
            className="btn btn-ghost"
            style={{ flexDirection: 'column', gap: 5, padding: '12px 8px', fontSize: '0.78rem' }}
          >
            <SMSIcon />
            SMS
          </button>
          <button
            id="btn-copy"
            onClick={handleShare}
            className="btn btn-ghost"
            style={{
              flexDirection: 'column', gap: 5, padding: '12px 8px', fontSize: '0.78rem',
              ...(copyDone ? { color: 'var(--green)', borderColor: 'rgba(16,185,129,0.4)' } : {}),
            }}
          >
            <CopyIcon />
            {copyDone ? 'Copied!' : 'Copy'}
          </button>
          <button
            id="btn-print"
            onClick={handlePrint}
            className="btn btn-ghost"
            style={{ flexDirection: 'column', gap: 5, padding: '12px 8px', fontSize: '0.78rem' }}
          >
            <PrintIcon />
            Print
          </button>
        </div>
      </div>

      {/* Confidence / AI tag */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '10px 14px',
        background: 'rgba(139,92,246,0.08)',
        border: '1px solid rgba(139,92,246,0.2)',
        borderRadius: 'var(--radius-sm)',
      }}>
        <span style={{ fontSize: '0.85rem' }}>🤖</span>
        <div style={{ flex: 1 }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-4)' }}>AI Confidence: </span>
          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--purple)' }}>
            {patient.confidence ? `${Math.round(patient.confidence * 100)}%` : '91%'}
          </span>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-4)', marginLeft: 8 }}>· Generated by Gemma 4 via SurviveMum AI</span>
        </div>
      </div>

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
