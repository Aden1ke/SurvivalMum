/** generateReferral — multilingual referral letter generator */
const facilities = [
  'General Hospital Kano — Obstetrics Ward',
  'Federal Medical Centre — Labour & Delivery',
  'State Teaching Hospital — Emergency Obs Unit',
  'PHC Secondary Referral — District Hospital',
];

function enTemplate(p, facility, urgency) {
  return `URGENT REFERRAL LETTER
Date: ${new Date().toLocaleDateString('en-GB')}
From: Community Birth Attendant — SurviveMum AI

To: Medical Officer In-Charge
${facility}

RE: ${urgency} PRIORITY REFERRAL

Patient: ${p.name || 'Unknown'} | Age: ${p.age || '—'} | G${p.gravida||'?'}P${p.para||'?'}
Time: ${new Date().toLocaleTimeString()}

CLINICAL FINDINGS:
• Blood Pressure: ${p.bp || '—'}
• Heart Rate: ${p.hr || '—'} bpm
• SpO2: ${p.spo2 || '—'}%
• Temperature: ${p.temp || '—'}°C
• LMP: ${p.lmp || 'Unknown'} | GA: ${p.ga || 'Unknown'}

AI ASSESSMENT: ${p.riskLevel || 'HIGH'} RISK
Condition: ${p.condition || 'Obstetric Emergency'}
Confidence: ${p.confidence ? Math.round(p.confidence*100)+'%' : '91%'}

ACTIONS TAKEN:
• Patient stabilised, left lateral position
• IV access established where available
• Uterotonic given if indicated

Please provide emergency obstetric care immediately.

Signed: TBA via SurviveMum AI`;
}

function haTemplate(p, facility) {
  return `WASIKAR KOMA-GAGGAWA
Ranar: ${new Date().toLocaleDateString('en-GB')}
Daga: Ma'aikaciyar Haihuwa — SurviveMum AI

Zuwa ga: Likita — ${facility}

GAME DA: ${p.name || 'Majiyyaci'} — GAGGAWA

Alamun: ${p.condition || 'haɗarin haihuwa'}
BP: ${p.bp||'—'} | HR: ${p.hr||'—'} bpm | SpO2: ${p.spo2||'—'}%

Don Allah a ba da kulawa nan take.
SurviveMum AI`;
}

function yoTemplate(p, facility) {
  return `LETA ITOKASI PAJAWIRI
Ojo: ${new Date().toLocaleDateString('en-GB')}
Lati: Oluranlowo Ibimo — SurviveMum AI

Si: Dokita — ${facility}

NIPA: ${p.name || 'Alaisan'} — PAJAWIRI

Ipo: ${p.condition || 'isoro ibimo'}
BP: ${p.bp||'—'} | HR: ${p.hr||'—'} bpm | SpO2: ${p.spo2||'—'}%

Jowo pese itoju lesekese.
SurviveMum AI`;
}

function igTemplate(p, facility) {
  return `LETA NTUZI IHE MBEREDE
Ubochi: ${new Date().toLocaleDateString('en-GB')}
Site na: Onye enyemaka amumunwa — SurviveMum AI

Nye: Dokita — ${facility}

MAKA: ${p.name || 'Onye oria'} — IHE MBEREDE

Odi ike: ${p.condition || 'ogwugwo amumunwa'}
BP: ${p.bp||'—'} | HR: ${p.hr||'—'} bpm | SpO2: ${p.spo2||'—'}%

Biko nye ogwugwo ozugbo.
SurviveMum AI`;
}

export function generateReferral(patient = {}, lang = 'EN') {
  const facility = facilities[Math.floor(Math.random() * facilities.length)];
  const urgency = patient.riskLevel === 'HIGH' ? 'CRITICAL' : 'URGENT';
  const map = { EN: enTemplate, HA: haTemplate, YO: yoTemplate, IG: igTemplate };
  const fn = map[lang] || enTemplate;
  return { text: fn(patient, facility, urgency), facility, urgency, lang, ts: Date.now() };
}
