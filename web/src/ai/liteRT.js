/** liteRT — Simulated LiteRT model loader for on-device AI */

export function loadLiteRTModel(modelName, onProgress) {
  return new Promise((resolve, reject) => {
    let progress = 0;
    const totalSize = 12.4; // MB (simulated)
    
    const interval = setInterval(() => {
      progress += Math.random() * 1.5;
      if (progress >= totalSize) {
        progress = totalSize;
        clearInterval(interval);
        
        // 5% chance of failure to demonstrate fallback UI
        if (Math.random() < 0.05) {
          reject(new Error(`Failed to initialize ${modelName}: Checksum mismatch.`));
        } else {
          resolve({
            name: modelName,
            version: '1.4.2',
            ready: true,
            ts: Date.now()
          });
        }
      }
      if (onProgress) onProgress((progress / totalSize) * 100);
    }, 150);
  });
}
