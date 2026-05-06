/** detectRisk — simulates AI risk classification with chain-of-thought trace */
const conditions = [
  {
    level: 'HIGH',
    condition: 'Pre-eclampsia',
    confidence: 0.94,
    symptoms: ['BP ≥ 140/90', 'Severe headache', 'SpO₂ drop'],
    pattern: 'Hypertensive disorder of pregnancy — Pattern H3',
    who: 'WHO ANC 2016, recommendation B2.3 — Severe pre-eclampsia management',
  },
  {
    level: 'HIGH',
    condition: 'Postpartum Haemorrhage',
    confidence: 0.91,
    symptoms: ['Rapid HR > 110', 'Pallor detected', 'BP drop'],
    pattern: 'PPH Pattern — Primary, atonic uterus (P1-A)',
    who: 'WHO 2023 PPH guidelines — Uterotonics first-line',
  },
  {
    level: 'MEDIUM',
    condition: 'Fetal Distress',
    confidence: 0.78,
    symptoms: ['Irregular fetal HR', 'Meconium staining risk', 'Reduced movement'],
    pattern: 'Non-reassuring fetal status — Intermittent pattern IF2',
    who: 'WHO Labour Care Guide 2020 — Intrapartum fetal monitoring',
  },
];

export function detectRisk() {
  return new Promise(resolve => {
    // Simulate 800-1200ms inference delay
    setTimeout(() => {
      const picked = conditions[Math.floor(Math.random() * conditions.length)];
      resolve({
        ...picked,
        trace: {
          inputSignals: picked.symptoms,
          patternMatched: picked.pattern,
          riskClassification: `${picked.level} RISK — ${picked.condition} (${(picked.confidence * 100).toFixed(0)}% confidence)`,
          evidenceBase: picked.who,
        },
        ts: Date.now(),
      });
    }, 900 + Math.random() * 400);
  });
}
