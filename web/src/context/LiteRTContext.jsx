import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { loadLiteRTModel } from '../ai/liteRT';

const LiteRTContext = createContext();

export function LiteRTProvider({ children }) {
  const [status, setStatus] = useState('idle'); // idle, loading, ready, error
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState(null);

  const initEngine = useCallback(async () => {
    setStatus('loading');
    setProgress(0);
    setError(null);
    
    try {
      // Simulate loading multiple models (Vitals, Risk, Cry)
      await loadLiteRTModel('VisionVitals_v1', setProgress);
      setStatus('ready');
    } catch (err) {
      setError(err.message);
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    initEngine();
  }, [initEngine]);

  return (
    <LiteRTContext.Provider value={{ status, progress, error, retry: initEngine }}>
      {children}
    </LiteRTContext.Provider>
  );
}

export function useLiteRT() {
  const context = useContext(LiteRTContext);
  if (!context) throw new Error('useLiteRT must be used within LiteRTProvider');
  return context;
}
