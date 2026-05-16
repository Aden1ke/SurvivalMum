/** scanANCCard — mock OCR + field extraction from ANC card image */
const mockPatients = [
  { name: 'Fatima Musa', age: 23, lmp: '14/01/2026', ga: '28 weeks', gravida: 3, para: 2, bp_history: '110/70, 118/74', flags: ['None'] },
  { name: 'Aisha Bello', age: 19, lmp: '02/02/2026', ga: '22 weeks', gravida: 1, para: 0, bp_history: '108/68', flags: ['Anaemia suspected'] },
  { name: 'Halima Yusuf', age: 31, lmp: '28/12/2025', ga: '32 weeks', gravida: 4, para: 3, bp_history: '140/92, 138/90', flags: ['Hypertension', 'Prev C-section'] },
  { name: 'Ngozi Obi', age: 27, lmp: '05/01/2026', ga: '29 weeks', gravida: 2, para: 1, bp_history: '116/76', flags: ['None'] },
];

export function scanANCCard() {
  return new Promise(resolve => {
    setTimeout(() => {
      const p = mockPatients[Math.floor(Math.random() * mockPatients.length)];
      resolve({ ...p, ts: Date.now(), scanned: true });
    }, 1200 + Math.random() * 600);
  });
}
