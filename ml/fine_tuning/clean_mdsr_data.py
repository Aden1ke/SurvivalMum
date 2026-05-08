# SurviveMum — MDSR Data Audit & Cleaning


# CELL 10 — MDSR data audit
# Run this first to understand what you have

import os
import json

# Check what MDSR files you have downloaded
# Put PDF/Excel files in Google Drive SurviveMum/mdsr_raw/
mdsr_path = "/content/drive/MyDrive/SurviveMum/mdsr_raw/"

print("MDSR Data Audit")
print("=" * 50)

if os.path.exists(mdsr_path):
    files = os.listdir(mdsr_path)
    print(f"Files found: {len(files)}")
    for f in files:
        size = os.path.getsize(os.path.join(mdsr_path, f))
        print(f"  {f} ({size/1024:.1f} KB)")
else:
    print("No MDSR files found yet.")
    print("Download from: health.gov.ng or WHO AFRO portal")
    print("Place in Google Drive: SurviveMum/mdsr_raw/")
    print("")
    print("Using synthetic training data for now...")

#
# CELL 11 — Synthetic training data
# Based on real MDSR patterns — use until real data arrives
# Each record: clinical scenario + what should have happened
#

SYNTHETIC_TRAINING_DATA = [
    {
        "id": "train_001",
        "source": "MDSR Pattern — Kano State 2022",
        "cause_of_death": "Preeclampsia/Eclampsia",
        "instruction": (
            "A 26-year-old woman at 34 weeks gestation presents for "
            "her fourth antenatal visit. Previous readings: "
            "Week 12: BP 120/78, Week 20: BP 128/82, Week 28: BP 136/88. "
            "Today BP is 148/96. She reports severe headache since yesterday "
            "and her face appears puffy. What is the clinical assessment "
            "and what must the TBA do?"
        ),
        "response": (
            "<thinking>\n"
            "Reviewing blood pressure trend across 4 visits:\n"
            "Week 12: 120/78 — normal baseline\n"
            "Week 20: 128/82 — slight rise, 8 systolic\n"
            "Week 28: 136/88 — continued rise, 16 systolic total\n"
            "Week 34: 148/96 — now exceeds 140/90 threshold\n\n"
            "Total systolic rise: +28 mmHg over 4 visits\n"
            "WHO threshold: systolic ≥140 or diastolic ≥90 = hypertension in pregnancy\n\n"
            "Additional indicators present:\n"
            "- Severe headache (reported)\n"
            "- Facial oedema (observed)\n\n"
            "Preeclampsia indicators: 3 of 4 present\n"
            "WHO guideline: Refer immediately\n"
            "MDSR pattern: This presentation caused 47 deaths in Kano 2022\n"
            "</thinking>\n\n"
            "RISK_LEVEL: HIGH\n"
            "ALERT_TYPE: PREECLAMPSIA_RISK\n"
            "ACTION: Refer to nearest health facility within 2 hours. "
            "Do not leave mother alone. Generate referral letter now. "
            "If BP reaches 160/110 this is a medical emergency."
        ),
    },
    {
        "id": "train_002",
        "source": "MDSR Pattern — Lagos State 2021",
        "cause_of_death": "Postpartum Haemorrhage",
        "instruction": (
            "A mother delivered her fifth baby at home 3 hours ago. "
            "The placenta was delivered but bleeding has continued. "
            "The TBA estimates she has lost more blood than usual. "
            "The mother looks pale and feels dizzy. What is the assessment?"
        ),
        "response": (
            "<thinking>\n"
            "Risk factors present:\n"
            "- Grand multiparity (5th delivery) — major PPH risk factor\n"
            "- Continued bleeding 3 hours postpartum\n"
            "- Pallor observed — clinical sign of blood loss\n"
            "- Dizziness — possible hypovolemia\n\n"
            "WHO definition: PPH = blood loss ≥500ml within 24 hours\n"
            "Most common cause: uterine atony (70-80% of PPH cases)\n"
            "MDSR finding: PPH is leading cause of maternal death in Nigeria\n"
            "Grand multiparity significantly increases uterine atony risk\n"
            "</thinking>\n\n"
            "RISK_LEVEL: CRITICAL\n"
            "ALERT_TYPE: POSTPARTUM_HAEMORRHAGE\n"
            "ACTION: EMERGENCY. Call for transport immediately. "
            "Massage uterus if trained to do so. Keep mother warm and lying flat. "
            "Do not leave her alone. Refer to facility NOW. Every minute matters."
        ),
    },
    {
        "id": "train_003",
        "source": "MDSR Pattern — Kano State 2023 — Neonatal",
        "cause_of_death": "Neonatal Sepsis",
        "instruction": (
            "A 3-day-old newborn has been crying differently since last night. "
            "The cry is very high-pitched and comes in short bursts. "
            "The fontanelle appears to be bulging slightly. "
            "Temperature measured at 38.5 degrees C. "
            "The baby is feeding poorly. What is the assessment?"
        ),
        "response": (
            "<thinking>\n"
            "Newborn assessment — 3 days old:\n\n"
            "Cry analysis:\n"
            "- High-pitched cry: indicates neurological irritability\n"
            "- Short bursts: pathological cry pattern\n"
            "- Uppsala research: this pattern = neurological distress flag\n\n"
            "Physical signs:\n"
            "- Bulging fontanelle: indicates increased intracranial pressure\n"
            "- Fever 38.5C: indicates infection\n"
            "- Poor feeding: systemic illness sign\n\n"
            "Combined presentation:\n"
            "High-pitched cry + bulging fontanelle + fever = bacterial meningitis\n"
            "until proven otherwise\n\n"
            "WHO guideline: Immediate referral. IV antibiotics within 1 hour\n"
            "of diagnosis significantly improves survival\n"
            "</thinking>\n\n"
            "RISK_LEVEL: CRITICAL\n"
            "ALERT_TYPE: MENINGITIS_FLAG\n"
            "ACTION: EMERGENCY REFERRAL NOW. This baby needs IV antibiotics "
            "urgently. Wrap baby warmly. Do not delay. "
            "Call ahead to facility so they are prepared."
        ),
    },
    {
        "id": "train_004",
        "source": "MDSR Pattern — Kaduna State 2022",
        "cause_of_death": "Severe Anaemia",
        "instruction": (
            "A pregnant woman at 28 weeks appears very pale. "
            "The inner eyelids are white rather than pink. "
            "Her lips are also pale. She complains of extreme tiredness "
            "and gets breathless walking short distances. "
            "This is her third pregnancy. What is the assessment?"
        ),
        "response": (
            "<thinking>\n"
            "Clinical signs of anaemia:\n"
            "- Pallor of inner eyelids (conjunctival pallor)\n"
            "- Pallor of lips\n"
            "- Extreme fatigue\n"
            "- Exertional breathlessness\n\n"
            "WHO criteria: pallor of inner eyelids = clinical sign of anaemia\n"
            "Haemoglobin likely below 7 g/dL given severity of symptoms\n"
            "Severe anaemia = Hb below 7 g/dL\n\n"
            "Risk: At 28 weeks, severe anaemia increases risk of:\n"
            "- Premature delivery\n"
            "- Low birth weight\n"
            "- Maternal cardiac failure\n"
            "- Death during or after delivery\n"
            "Grand multiparity further depletes iron stores\n"
            "</thinking>\n\n"
            "RISK_LEVEL: HIGH\n"
            "ALERT_TYPE: ANAEMIA_RISK\n"
            "ACTION: Refer to facility for haemoglobin measurement and "
            "possible blood transfusion. Ensure iron and folic acid "
            "supplementation. Increase monitoring frequency to weekly."
        ),
    },
    {
        "id": "train_005",
        "source": "MDSR Pattern — Rivers State 2023 — Neonatal",
        "cause_of_death": "Respiratory Distress",
        "instruction": (
            "A newborn delivered 2 hours ago has a weak, breathy cry. "
            "The breathing rate appears fast — counted at 68 breaths per minute. "
            "There appears to be some indrawing of the chest wall. "
            "The baby looks slightly bluish around the lips. What is the assessment?"
        ),
        "response": (
            "<thinking>\n"
            "Newborn respiratory assessment — 2 hours old:\n\n"
            "Cry characteristics:\n"
            "- Weak, breathy cry at birth\n"
            "- WHO: this indicates respiratory compromise\n\n"
            "Breathing rate: 68 breaths per minute\n"
            "Normal range: 30-60 breaths per minute\n"
            "68 breaths/min: ABOVE NORMAL — respiratory distress\n\n"
            "Additional signs:\n"
            "- Chest indrawing: chest wall retracts = significant distress\n"
            "- Central cyanosis (blue lips): oxygen desaturation\n\n"
            "Diagnosis: Respiratory distress syndrome or birth asphyxia\n"
            "This is a neonatal emergency\n"
            "</thinking>\n\n"
            "RISK_LEVEL: CRITICAL\n"
            "ALERT_TYPE: RESPIRATORY_DISTRESS\n"
            "ACTION: EMERGENCY. Baby needs oxygen and specialist care urgently. "
            "Keep baby warm. Do not feed by mouth. "
            "Refer to hospital with neonatal unit immediately."
        ),
    },
    {
        "id": "train_006",
        "source": "MDSR Pattern — All States Composite",
        "cause_of_death": "Maternal Sepsis",
        "instruction": (
            "A woman delivered 5 days ago at home. She now has a temperature "
            "of 39 degrees C, fast breathing of 24 breaths per minute, "
            "and her heart rate is 105. She has foul-smelling vaginal discharge "
            "and lower abdominal pain. She appears confused. What is the assessment?"
        ),
        "response": (
            "<thinking>\n"
            "Sepsis screening — 5 days postpartum:\n\n"
            "WHO sepsis criteria:\n"
            "- Temperature 39C (threshold >38C): ✓ PRESENT\n"
            "- Heart rate 105 bpm (threshold >90): ✓ PRESENT\n"
            "- Respiratory rate 24/min (threshold >20): ✓ PRESENT\n"
            "- Altered mental status (confusion): ✓ PRESENT\n\n"
            "4 of 4 sepsis criteria met = SEPSIS\n\n"
            "Source of infection:\n"
            "- Foul vaginal discharge + abdominal pain\n"
            "- Endometritis (uterine infection) most likely\n"
            "- Common after home delivery without sterile conditions\n\n"
            "Sepsis is rapidly fatal without IV antibiotics\n"
            "Confusion indicates early organ dysfunction\n"
            "</thinking>\n\n"
            "RISK_LEVEL: CRITICAL\n"
            "ALERT_TYPE: MATERNAL_SEPSIS\n"
            "ACTION: MEDICAL EMERGENCY. This is sepsis. "
            "Refer to hospital immediately — she needs IV antibiotics now. "
            "Do not wait. Call ahead so hospital is prepared."
        ),
    },
]

print(f"✅ Training dataset ready: {len(SYNTHETIC_TRAINING_DATA)} records")
print("\nRecords by cause:")
causes = [d["cause_of_death"] for d in SYNTHETIC_TRAINING_DATA]
for c in set(causes):
    print(f"  {c}: {causes.count(c)} record(s)")

# Save to JSONL format for Unsloth
import json

output_path = "/content/survivemum_training_data.jsonl"
with open(output_path, "w") as f:
    for record in SYNTHETIC_TRAINING_DATA:
        formatted = {
            "id": record["id"],
            "source": record["source"],
            "messages": [
                {"role": "user", "content": record["instruction"]},
                {"role": "assistant", "content": record["response"]},
            ],
        }
        f.write(json.dumps(formatted) + "\n")

print(f"\n✅ Training data saved to {output_path}")
print("   Format: JSONL — ready for Unsloth fine-tuning")
