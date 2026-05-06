/** generateVitals — simulates Gemma 4 on-device vitals extraction */
const ranges = {
  hr:   { base: 82, variance: 18 },
  spo2: { base: 97, variance: 3 },
  rr:   { base: 17, variance: 5 },
  sys:  { base: 118, variance: 22 },
  dia:  { base: 78, variance: 14 },
  temp: { base: 36.8, variance: 0.9 },
};

function rand(base, variance) {
  return +(base + (Math.random() - 0.5) * variance).toFixed(1);
}

export function generateVitals(risk = 'NORMAL') {
  const multiplier = risk === 'HIGH' ? 1.3 : risk === 'MEDIUM' ? 1.1 : 1;
  return {
    hr:   Math.round(rand(ranges.hr.base * multiplier, ranges.hr.variance)),
    spo2: Math.min(100, Math.round(rand(ranges.spo2.base / multiplier, ranges.spo2.variance))),
    rr:   Math.round(rand(ranges.rr.base * multiplier, ranges.rr.variance)),
    bp:   `${Math.round(rand(ranges.sys.base * multiplier, ranges.sys.variance))}/${Math.round(rand(ranges.dia.base, ranges.dia.variance))}`,
    temp: rand(ranges.temp.base * (risk === 'HIGH' ? 1.02 : 1), ranges.temp.variance),
    ts:   Date.now(),
  };
}
