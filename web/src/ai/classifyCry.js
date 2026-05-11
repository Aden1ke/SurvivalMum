/** classifyCry — mock newborn cry + breathing classifier */
const cryTypes = [
  { type: 'Normal', confidence: 0.92, flag: null, color: '#10B981' },
  { type: 'Hunger', confidence: 0.87, flag: 'Feed within 30 min', color: '#F59E0B' },
  { type: 'Pain', confidence: 0.83, flag: 'Assess for injury or infection', color: '#EF4444' },
  { type: 'Respiratory Distress', confidence: 0.79, flag: 'URGENT: Check airway', color: '#EF4444' },
];

const jaundiceStates = [
  { level: 'None', color: '#F5CBA7', risk: 'LOW', advice: 'Normal skin tone. Monitor.' },
  { level: 'Mild', color: '#F0B27A', risk: 'LOW-MEDIUM', advice: 'Expose to indirect sunlight. Recheck in 6h.' },
  { level: 'Moderate', color: '#E59866', risk: 'MEDIUM', advice: 'Phototherapy recommended. Refer if worsening.' },
  { level: 'Severe', color: '#CA6F1E', risk: 'HIGH', advice: 'URGENT: Immediate phototherapy or exchange transfusion.' },
];

export function classifyCry() {
  return new Promise(resolve => {
    setTimeout(() => {
      const cry = cryTypes[Math.floor(Math.random() * cryTypes.length)];
      const jaundice = jaundiceStates[Math.floor(Math.random() * jaundiceStates.length)];
      resolve({
        cry,
        breathingRate: Math.round(30 + Math.random() * 30),
        jaundice,
        ts: Date.now(),
      });
    }, 700 + Math.random() * 500);
  });
}
