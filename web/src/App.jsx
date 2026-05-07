import { Routes, Route, useLocation } from 'react-router-dom'
import { memo, useMemo } from 'react'
import { LiteRTProvider, useLiteRT } from './context/LiteRTContext'
import NavBar from './components/NavBar'
import ModelLoadingSplash from './components/ModelLoadingSplash'
import CameraMonitor from './screens/CameraMonitor'
import ANCScanner from './screens/ANCScanner'
import AlertScreen from './screens/AlertScreen'
import ReferralLetter from './screens/ReferralLetter'
import EmergencyConfirm from './screens/EmergencyConfirm'
import NewbornMonitor from './screens/NewbornMonitor'
import QRCodeScreen from './screens/QRCode'

const BARE_ROUTES = ['/alert']

// Memoized Header for Performance
const AppHeader = memo(({ isBare }) => {
  if (isBare) return null;
  return (
    <header style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '12px 16px', borderBottom: '1px solid var(--border)',
      background: 'rgba(17,24,39,0.95)', backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)', flexShrink: 0, zIndex: 50,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <span style={{ fontSize: '1.4rem' }}>🫀</span>
        <div>
          <div style={{ fontWeight: 700, fontSize: '1rem', lineHeight: 1.2 }}>SurviveMum</div>
          <div style={{ fontSize: '0.63rem', color: 'var(--text-4)', letterSpacing: '0.07em', fontWeight: 500 }}>
            MATERNAL AI SYSTEM
          </div>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: '0.62rem', color: 'var(--text-4)' }}>Powered by</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--blue)', fontWeight: 600 }}>LiteRT + Gemma 4</div>
        </div>
        <div style={{
          width: 8, height: 8, borderRadius: '50%',
          background: 'var(--green)', boxShadow: '0 0 8px var(--green)',
          animation: 'blink 2s infinite',
        }} />
      </div>
    </header>
  );
});

function AppContent() {
  const { pathname } = useLocation();
  const { status, progress, error, retry } = useLiteRT();
  const isBare = useMemo(() => BARE_ROUTES.includes(pathname), [pathname]);

  if (status === 'loading' || status === 'error') {
    return <ModelLoadingSplash progress={progress} error={error} onRetry={retry} />;
  }

  return (
    <div className="app-shell">
      <AppHeader isBare={isBare} />
      <main className="screen-content" style={isBare ? { paddingBottom: 0 } : {}}>
        <Routes>
          <Route path="/"          element={<CameraMonitor />} />
          <Route path="/anc"       element={<ANCScanner />} />
          <Route path="/alert"     element={<AlertScreen />} />
          <Route path="/referral"  element={<ReferralLetter />} />
          <Route path="/emergency" element={<EmergencyConfirm />} />
          <Route path="/newborn"   element={<NewbornMonitor />} />
          <Route path="/qr"        element={<QRCodeScreen />} />
        </Routes>
      </main>
      {!isBare && <NavBar />}
    </div>
  );
}

export default function App() {
  return (
    <LiteRTProvider>
      <AppContent />
    </LiteRTProvider>
  );
}
