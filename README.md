<div align="center">

```
 ____                  _           __  __
/ ___| _   _ _ ____  _(_)_   ____  |  \/  |_   _ _ __ ___
\___ \| | | | '__\ \/ / \ \ / / _ \ | |\/| | | | | '_ ` _ \
 ___) | |_| | |   >  <| |\ V /  __/ | |  | | |_| | | | | | |
|____/ \__,_|_|  /_/\_\_| \_/ \___| |_|  |_|\__,_|_| |_| |_|
```

### *The Silent Guardian*

> **An AI health guardian built for patients who cannot speak.**

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Gemma 4](https://img.shields.io/badge/Powered%20by-Gemma%204%20E4B-blue.svg)](https://ai.google.dev/gemma)
[![Offline](https://img.shields.io/badge/Works-100%25%20Offline-green.svg)]()
[![Hackathon](https://img.shields.io/badge/Gemma%204%20Good%20Hackathon-Google%20DeepMind-orange.svg)](https://kaggle.com/competitions/gemma-4-good-hackathon)

</div>

---

## The Problem

Every health AI ever built waits for the patient to speak.

But there is a window of human life where death is most likely and communication is impossible:

**Pregnancy → Birth → 0 to 5 years old**

- A mother cannot always say what feels wrong
- A newborn cannot speak at all
- A toddler cannot describe pain

> 2.3 million newborns die in their first 28 days every year.
> Most were never watched by anything intelligent.

---

## What SurviveMum Does

Runs 100% offline on a **$80 Android phone.** No internet. No cloud. No server. Just Gemma 4 watching silently.

### Layer 1 — The Mother
Camera reads her vital signs without touching her. Gemma reads her handwritten ANC card. Tracks blood pressure trends across months. Alerts before she knows something is wrong.

### Layer 2 — The Newborn
Phone listens to the cry. High-pitched short bursts = possible meningitis flag. Weak breathy cry = respiratory distress. Camera watches skin colour for jaundice. Breathing rate from chest movement. **The baby never needs to be examined. The phone just watches.**

### Layer 3 — The Toddler
Detects movement asymmetry. Monitors speech patterns for developmental delays. Notices behavioural changes since the last visit through memory.

---

## Gemma 4 Capabilities Used

**12 of 12.** Every capability is load-bearing. Remove Gemma 4 and the system collapses.

| # | Capability | What It Does |
|---|-----------|-------------|
| 1 | Multimodal Vision | Vital signs from camera. Handwriting from ANC cards. |
| 2 | Audio Multimodal | Classifies baby cry in real time. On-device. |
| 3 | Interleaved Multimodal | Multiple ANC cards + face + audio in one prompt. |
| 4 | 128K Long Context | Holds entire pregnancy in memory. Sees trends. |
| 5 | Thinking Traces | Every alert shows exactly why it was fired. |
| 6 | Native Function Calling | Auto-generates referral letters and QR records. |
| 7 | On-Device RAG | Grounds every recommendation in WHO guidelines. |
| 8 | Model Routing E2B/E4B | Saves battery. Switches model by complexity. |
| 9 | ShieldGemma | Safety screens every input and output. |
| 10 | Federated Learning | Two phones learn together. Zero patient data shared. |
| 11 | Unsloth Fine-Tuning | Trained on real Nigeria MDSR maternal death data. |
| 12 | LiteRT Edge Deploy | Gemma 4 on $80 Android. No infrastructure needed. |

---

## Project Structure

```
survivemum/
│
├── app/                        Android application
│   └── src/main/
│       ├── java/com/survivemum/
│       │   ├── ui/                FE owns — 7 demo screens
│       │   ├── data/              FS owns — Room database + DAOs
│       │   ├── ml/                BE-1 owns — LiteRT + rPPG + camera
│       │   └── security/          BE-2 owns — ShieldGemma + routing
│       ├── res/
│       │   ├── values/            strings.xml  colors.xml
│       │   └── xml/               network security  file paths
│       └── AndroidManifest.xml   All permissions declared Day 1
│
├── ml/                         Python AI pipeline
│   ├── cry_classification/        Neonatal cry analysis (FS)
│   ├── rag_pipeline/              FAISS + WHO guidelines (FS)
│   ├── fine_tuning/               Unsloth + MDSR data (FS)
│   ├── data/
│   │   ├── sample/                Safe test data — in repo
│   │   ├── raw/                   Real MDSR data — gitignored
│   │   └── processed/             Cleaned data — gitignored
│   ├── models/                    Weights — gitignored
│   ├── config.py                  All shared constants
│   └── requirements.txt           All Python packages pinned
│
├── contracts/                  Data format agreements between members
│   ├── alert_schema.json          BE-2 sends → FE + FS receive
│   ├── vital_signs_schema.json    BE-1 sends → FE + FS receive
│   ├── cry_classification_schema.json   FS sends → FE + BE-2 receive
│   ├── patient_record_schema.json FS sends → FE + BE-1 receive
│   └── anc_card_schema.json       FS sends → FE + BE-1 receive
│
├── database/                   SQLite
│   ├── schema.sql                 11 tables — agreed Day 2, frozen
│   └── seed_data.sql              Fake test patients — safe to use
│
├──  video/                      Video production plan (FE)
├──  submission/                 Kaggle writeup + links (FS)
├──  docs/                       Setup guide + architecture
│
├── .github/workflows/             CI — Android build + Python lint
├── .gitignore                     Blocks weights, MDSR data, secrets
├── CONTRIBUTING.md                Branch rules + contract policy
└── LICENSE                        Apache 2.0
```

---

## Team

| | Member | Role | Owns |
|---|--------|------|------|
| **FS — Team Lead** | Data Engineer · AI Trainer · Submission | `data/` · `ml/` · 8 screens · writeup |
|  **BE-1** | AI Scientist | rPPG · Interleaved Multimodal · Long Context |
|  **BE-2** | Infrastructure & Security | ShieldGemma · Routing · Federated Learning |
|  **FE** | Product Builder & Storyteller | 7 demo screens · 3-minute video |

---

## Quick Start

```bash
# Clone
git clone https://github.com/[your-org]/survivemum.git
cd survivemum

# Python setup
cd ml && pip install -r requirements.txt

# Android — open root folder in Android Studio
# Connect a physical Android device. Run the app.

# Load test data
sqlite3 app/src/main/assets/test.db < database/schema.sql
sqlite3 app/src/main/assets/test.db < database/seed_data.sql
```

Full instructions → [docs/setup_guide.md](docs/setup_guide.md)

---

## Branch Rules

```
Never push to main.
Branch name: feature/[your-initials]-[what-you-are-building]
One reviewer per merge. Never merge your own PR.
Contract files in contracts/ — never changed without team agreement.
```

Full rules → [CONTRIBUTING.md](CONTRIBUTING.md)

---

## Submission Links

| Item | Link | Status |
|------|------|--------|
| Kaggle Writeup | *Added May 16* | 🔲 |
| Demo Video | *Added May 15* | 🔲 |
| Live Demo | *Added May 15* | 🔲 |
| Hackathon Page | [Gemma 4 Good](https://kaggle.com/competitions/gemma-4-good-hackathon) | ✅ |

**Deadline: May 18, 2026 · 11:59 PM UTC**

---

<div align="center">

*Built for the Gemma 4 Good Hackathon · Google DeepMind · 2026*

*Apache 2.0 · Gemma 4 by Google DeepMind*

</div>
