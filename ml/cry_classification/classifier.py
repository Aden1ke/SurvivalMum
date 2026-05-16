# SurviveMum — Cry Classification Pipeline
# Run this entire file to produce cry_patterns.json for Android

# ─────────────────────────────────────────────────────────────────────────────
# CELL 1 — Install dependencies
# ─────────────────────────────────────────────────────────────────────────────

# !pip install -q librosa numpy scipy

import json
import numpy as np

try:
    import librosa
    LIBROSA_AVAILABLE = True
    print("✅ librosa available — full audio analysis enabled")
except ImportError:
    LIBROSA_AVAILABLE = False
    print("⚠️  librosa not available — using pattern rules only")

print("✅ Dependencies ready")

# ─────────────────────────────────────────────────────────────────────────────
# CELL 2 — Clinical cry patterns
# Based on published neonatal cry research and WHO guidelines
# ─────────────────────────────────────────────────────────────────────────────

CRY_PATTERNS = {
    "DISTRESS": {
        "description": "High-pitched short bursts — possible meningitis or neurological irritability",
        "clinical_source": "WHO Pocket Book: High-pitched cry = neurological distress flag",
        "severity": "CRITICAL",
        "pitch_hz_min": 400,
        "pitch_hz_max": 800,
        "burst_duration_max_seconds": 0.5,
        "action": "EMERGENCY. Possible meningitis. Refer to facility immediately. Check fontanelle.",
        "alert_type": "NEUROLOGICAL_DISTRESS"
    },
    "RESPIRATORY_DISTRESS": {
        "description": "Weak breathy cry — respiratory distress at birth",
        "clinical_source": "WHO Essential Newborn Care 2024: Weak cry at birth = immediate assessment",
        "severity": "CRITICAL",
        "pitch_hz_min": 100,
        "pitch_hz_max": 250,
        "burst_duration_min_seconds": 2.0,
        "amplitude_threshold": "LOW",
        "action": "EMERGENCY. Begin newborn resuscitation. Refer immediately.",
        "alert_type": "RESPIRATORY_DISTRESS"
    },
    "PAIN": {
        "description": "Intense sustained cry — pain response",
        "clinical_source": "Neonatal cry research: Sustained high cry = pain or discomfort",
        "severity": "HIGH",
        "pitch_hz_min": 300,
        "pitch_hz_max": 500,
        "burst_duration_min_seconds": 1.0,
        "burst_duration_max_seconds": 3.0,
        "action": "Assess for injury, infection, or feeding difficulty. Monitor closely.",
        "alert_type": "PAIN_CRY"
    },
    "HUNGER": {
        "description": "Rhythmic repetitive cry — hunger or discomfort",
        "clinical_source": "Neonatal cry research: Rhythmic pattern = hunger or basic need",
        "severity": "LOW",
        "pitch_hz_min": 250,
        "pitch_hz_max": 380,
        "rhythmic": True,
        "action": "Feed the baby. Check nappy. Monitor for escalation.",
        "alert_type": "HUNGER_CRY"
    },
    "NORMAL": {
        "description": "Normal healthy vigorous cry",
        "clinical_source": "WHO Essential Newborn Care: Vigorous cry within 30 seconds = healthy",
        "severity": "LOW",
        "pitch_hz_min": 250,
        "pitch_hz_max": 400,
        "action": "No action required. Continue routine monitoring.",
        "alert_type": "NORMAL"
    }
}

print(f"✅ {len(CRY_PATTERNS)} cry patterns defined")
for label, pattern in CRY_PATTERNS.items():
    print(f"  {label}: severity={pattern['severity']}, pitch={pattern.get('pitch_hz_min', '?')}-{pattern.get('pitch_hz_max', '?')}Hz")

# ─────────────────────────────────────────────────────────────────────────────
# CELL 3 — Classification function
# This is the logic CryClassifier.kt replicates on Android
# ─────────────────────────────────────────────────────────────────────────────

def classify_cry(
    pitch_hz: float,
    burst_duration_seconds: float,
    amplitude: float = 0.5,
    is_rhythmic: bool = False
) -> dict:
    """
    Classify a baby cry based on extracted audio features.

    Args:
        pitch_hz: Fundamental frequency of the cry in Hz
        burst_duration_seconds: Duration of a single cry burst
        amplitude: Normalised amplitude 0.0 to 1.0
        is_rhythmic: Whether the cry follows a repetitive pattern

    Returns:
        Classification result with label, severity, and action
    """

    # DISTRESS — high pitched, short bursts (meningitis/neurological)
    if pitch_hz >= 400 and burst_duration_seconds <= 0.5:
        label = "DISTRESS"

    # RESPIRATORY — weak, low pitched, long duration
    elif pitch_hz <= 250 and burst_duration_seconds >= 2.0 and amplitude < 0.3:
        label = "RESPIRATORY_DISTRESS"

    # PAIN — sustained medium-high pitch
    elif 300 <= pitch_hz <= 500 and 1.0 <= burst_duration_seconds <= 3.0:
        label = "PAIN"

    # HUNGER — rhythmic, mid pitch
    elif is_rhythmic and 250 <= pitch_hz <= 380:
        label = "HUNGER"

    # NORMAL — vigorous, healthy range
    else:
        label = "NORMAL"

    pattern = CRY_PATTERNS[label]
    return {
        "label": label,
        "severity": pattern["severity"],
        "description": pattern["description"],
        "action": pattern["action"],
        "alert_type": pattern["alert_type"],
        "clinical_source": pattern["clinical_source"],
        "features_used": {
            "pitch_hz": round(pitch_hz, 1),
            "burst_duration_seconds": round(burst_duration_seconds, 2),
            "amplitude": round(amplitude, 2),
            "is_rhythmic": is_rhythmic
        }
    }


# ─────────────────────────────────────────────────────────────────────────────
# CELL 4 — Extract features from audio file (if librosa available)
# ─────────────────────────────────────────────────────────────────────────────

def extract_features(audio_path: str) -> dict:
    """
    Extract pitch, duration, amplitude from a .wav file.
    Use this when you have a real audio sample to test.
    """
    if not LIBROSA_AVAILABLE:
        print("librosa not installed — cannot extract from audio file")
        return {}

    audio, sr = librosa.load(audio_path, sr=16000)

    # Extract pitch
    pitches, magnitudes = librosa.piptrack(y=audio, sr=sr)
    pitch_values = pitches[magnitudes > np.median(magnitudes)]
    mean_pitch = float(np.mean(pitch_values)) if len(pitch_values) > 0 else 0.0

    # Estimate burst duration using onset detection
    onsets = librosa.onset.onset_detect(y=audio, sr=sr, units='time')
    if len(onsets) > 1:
        burst_duration = float(np.mean(np.diff(onsets)))
    else:
        burst_duration = float(len(audio) / sr)

    # Amplitude
    amplitude = float(np.mean(np.abs(audio)))

    return {
        "pitch_hz": round(mean_pitch, 1),
        "burst_duration_seconds": round(burst_duration, 2),
        "amplitude": round(amplitude, 4)
    }


# ─────────────────────────────────────────────────────────────────────────────
# CELL 5 — Test the classifier with synthetic scenarios
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "═"*50)
print("TESTING CRY CLASSIFIER")
print("═"*50)

test_cases = [
    {
        "scenario": "Meningitis — high pitched short bursts",
        "pitch_hz": 550.0,
        "burst_duration_seconds": 0.3,
        "amplitude": 0.8,
        "expected": "DISTRESS"
    },
    {
        "scenario": "Respiratory distress — weak breathy cry at birth",
        "pitch_hz": 180.0,
        "burst_duration_seconds": 3.5,
        "amplitude": 0.15,
        "expected": "RESPIRATORY_DISTRESS"
    },
    {
        "scenario": "Pain cry — sustained medium pitch",
        "pitch_hz": 380.0,
        "burst_duration_seconds": 2.0,
        "amplitude": 0.6,
        "expected": "PAIN"
    },
    {
        "scenario": "Hunger — rhythmic mid pitch",
        "pitch_hz": 300.0,
        "burst_duration_seconds": 1.2,
        "amplitude": 0.5,
        "expected": "HUNGER"
    },
    {
        "scenario": "Normal healthy cry",
        "pitch_hz": 320.0,
        "burst_duration_seconds": 1.5,
        "amplitude": 0.7,
        "expected": "NORMAL"
    }
]

all_passed = True
for test in test_cases:
    result = classify_cry(
        pitch_hz=test["pitch_hz"],
        burst_duration_seconds=test["burst_duration_seconds"],
        amplitude=test["amplitude"]
    )
    passed = result["label"] == test["expected"]
    if not passed:
        all_passed = False
    status = "✅" if passed else "❌"
    print(f"\n{status} {test['scenario']}")
    print(f"   Expected: {test['expected']} | Got: {result['label']}")
    print(f"   Severity: {result['severity']}")
    print(f"   Action: {result['action'][:60]}...")

print(f"\n{'✅ All tests passed' if all_passed else '❌ Some tests failed — review classification logic'}")

# ─────────────────────────────────────────────────────────────────────────────
# CELL 6 — Export for Android
# Produces cry_patterns.json → copy to app/src/main/assets/
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "═"*50)
print("EXPORTING FOR ANDROID")
print("═"*50)

# Full patterns file — used by CryClassifier.kt
with open("cry_patterns.json", "w") as f:
    json.dump(CRY_PATTERNS, f, indent=2)
print("✅ Saved: cry_patterns.json")

# Thresholds only — lightweight version for on-device logic
THRESHOLDS = {
    "DISTRESS":             {"pitch_min": 400, "burst_max": 0.5},
    "RESPIRATORY_DISTRESS": {"pitch_max": 250, "burst_min": 2.0, "amplitude_max": 0.3},
    "PAIN":                 {"pitch_min": 300, "pitch_max": 500, "burst_min": 1.0, "burst_max": 3.0},
    "HUNGER":               {"pitch_min": 250, "pitch_max": 380, "rhythmic": True},
    "NORMAL":               {"pitch_min": 250, "pitch_max": 400}
}

with open("cry_thresholds.json", "w") as f:
    json.dump(THRESHOLDS, f, indent=2)
print("✅ Saved: cry_thresholds.json")

print("\n" + "═"*50)
print("COPY THESE FILES TO ANDROID:")
print("═"*50)
print("")
print("  cry_patterns.json    → app/src/main/assets/")
print("  cry_thresholds.json  → app/src/main/assets/")
print("")
print("Then CryClassifier.kt loads them at runtime.")
print("═"*50)