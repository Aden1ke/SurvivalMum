import { memo, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

const tabs = [
  { path: '/',          label: 'Monitor',  Icon: CameraIcon },
  { path: '/anc',       label: 'ANC',      Icon: ScanIcon },
  { path: '/alert',     label: 'Alert',    Icon: AlertIcon },
  { path: '/referral',  label: 'Referral', Icon: ReferralIcon },
  { path: '/newborn',   label: 'Newborn',  Icon: BabyIcon },
  { path: '/qr',        label: 'QR',       Icon: QRIcon },
];

function NavBar() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const go = useCallback((path) => navigate(path), [navigate]);

  return (
    <nav className="nav-bar" role="navigation" aria-label="Main navigation">
      {tabs.map(({ path, label, Icon }) => (
        <button
          key={path}
          id={`nav-${label.toLowerCase().replace(/\s/g, '-')}`}
          className={`nav-item${pathname === path ? ' active' : ''}`}
          onClick={() => go(path)}
          aria-label={label}
          aria-current={pathname === path ? 'page' : undefined}
        >
          <Icon />
          <span className="nav-label">{label}</span>
        </button>
      ))}
    </nav>
  );
}

/* ── SVG Icons ─────────────────────────────────────────────── */
function CameraIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
      <circle cx="12" cy="13" r="4"/>
    </svg>
  );
}

function ScanIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2"/>
      <line x1="3" y1="12" x2="21" y2="12"/>
    </svg>
  );
}

function AlertIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
      <line x1="12" y1="9" x2="12" y2="13"/>
      <line x1="12" y1="17" x2="12.01" y2="17"/>
    </svg>
  );
}

function ReferralIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
      <polyline points="14 2 14 8 20 8"/>
      <line x1="16" y1="13" x2="8" y2="13"/>
      <line x1="16" y1="17" x2="8" y2="17"/>
      <polyline points="10 9 9 9 8 9"/>
    </svg>
  );
}

function BabyIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="7" r="4"/>
      <path d="M5.5 21a8.38 8.38 0 0 1 13 0"/>
      <circle cx="8" cy="14" r="1" fill="currentColor"/>
      <circle cx="16" cy="14" r="1" fill="currentColor"/>
    </svg>
  );
}

function QRIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
      <rect x="3" y="14" width="7" height="7"/>
      <path d="M14 14h3v3h-3zM17 17h3v3h-3zM14 20h3"/>
    </svg>
  );
}

export default memo(NavBar);
