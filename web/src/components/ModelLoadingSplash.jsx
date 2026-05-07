import { memo } from 'react';
import ActionButton from './ActionButton';

const ModelLoadingSplash = memo(({ progress, error, onRetry }) => {
  return (
    <div style={{
      position: 'fixed', inset: 0,
      background: 'var(--bg)',
      zIndex: 1000,
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      padding: 24, textAlign: 'center',
    }}>
      <div style={{ fontSize: '4rem', marginBottom: 24, animation: 'pulse-ring 2s infinite' }}>🫀</div>
      
      <h1 style={{ fontSize: '1.5rem', marginBottom: 8 }}>SurviveMum AI</h1>
      <p style={{ color: 'var(--text-3)', fontSize: '0.9rem', marginBottom: 32 }}>
        Initializing on-device LiteRT models...
      </p>

      {error ? (
        <div className="animate-fade-in" style={{ maxWidth: 300 }}>
          <div style={{ color: 'var(--red)', marginBottom: 16, fontSize: '0.875rem' }}>
            ⚠️ {error}
          </div>
          <ActionButton variant="primary" onClick={onRetry} fullWidth>
            Retry Initialization
          </ActionButton>
        </div>
      ) : (
        <div style={{ width: '100%', maxWidth: 240 }}>
          <div style={{ 
            height: 4, background: 'var(--surface-2)', 
            borderRadius: 2, overflow: 'hidden', marginBottom: 12 
          }}>
            <div style={{ 
              height: '100%', background: 'var(--blue)', 
              width: `${progress}%`, transition: 'width 200ms ease-out',
              boxShadow: '0 0 10px var(--blue-glow)'
            }} />
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-4)', fontWeight: 600 }}>
            {Math.round(progress)}% LOADED
          </div>
        </div>
      )}

      <div style={{ position: 'absolute', bottom: 40, color: 'var(--text-4)', fontSize: '0.7rem', letterSpacing: '0.05em' }}>
        POWERED BY GEMMA 4 & LITERT
      </div>
    </div>
  );
});

export default ModelLoadingSplash;
