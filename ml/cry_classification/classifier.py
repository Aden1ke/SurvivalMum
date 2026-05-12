# SurviveMum — Cry Classification Pipeline


# CELL 1 — Install dependencies
!pip install -q librosa soundfile numpy scipy

import librosa
import numpy as np
import json
import uuid
from datetime import datetime, timezone

print("✅ Cry classification dependencies ready")

# 
# CELL 2 — Clinical constants from research
# Uppsala University + MIT cry analysis
# 

CRY_CLASSES = [
    "HUNGER",
    "PAIN",
    "NEUROLOGICAL_DISTRESS",
    "RESPIRATORY_DISTRESS",
    "DISCOMFORT",
    "NORMAL_FUSSING",
    "SILENT_WHEN_EXPECTED",
    "UNKNOWN"
]

CRY_CLINICAL_FLAGS = {
    "NEUROLOGICAL_DISTRESS": "POSSIBLE_MENINGITIS",
    "RESPIRATORY_DISTRESS":  "RESPIRATORY_DISTRESS",
    "PAIN":                  "PAIN_INVESTIGATION_NEEDED",
    "SILENT_WHEN_EXPECTED":  "NEUROLOGICAL_CONCERN",
    "HUNGER":                None,
    "NORMAL_FUSSING":        None,
    "DISCOMFORT":            None,
    "UNKNOWN":               None
}

THRESHOLDS = {
    "min_audio_seconds":     3.0,
    "flag_confidence_min":   0.65,
    "neurological_pitch_hz": 500,
    "burst_duration_max":    1.5,
    "weak_cry_intensity":    0.3,
}

print("✅ Clinical constants loaded")
print(f"   {len(CRY_CLASSES)} cry classes defined")
print(f"   {len([v for v in CRY_CLINICAL_FLAGS.values() if v])} clinical flags")

# 
# CELL 3 — Audio feature extractor
# 

def extract_cry_features(audio_path: str) -> dict:
    """
    Extract acoustic features from a cry audio file.
    These features are what Gemma 4 audio multimodal
    uses to classify the cry type.
    
    Args:
        audio_path: Path to WAV file (minimum 3 seconds)
    
    Returns:
        Dictionary of extracted acoustic features
    """
    try:
        # Load audio
        y, sr = librosa.load(audio_path, sr=22050)
        duration = len(y) / sr
        
        if duration < THRESHOLDS["min_audio_seconds"]:
            return {
                "error": f"Audio too short: {duration:.1f}s. Minimum 3s required.",
                "duration_seconds": duration
            }
        
        #  PITCH ANALYSIS 
        f0, voiced_flag, voiced_probs = librosa.pyin(
            y,
            fmin=librosa.note_to_hz('C2'),
            fmax=librosa.note_to_hz('C7')
        )
        
        valid_f0 = f0[voiced_flag]
        mean_pitch = float(np.nanmean(valid_f0)) if len(valid_f0) > 0 else 0
        max_pitch = float(np.nanmax(valid_f0)) if len(valid_f0) > 0 else 0
        
        #  INTENSITY / ENERGY 
        rms = librosa.feature.rms(y=y)[0]
        mean_intensity = float(np.mean(rms))
        max_intensity = float(np.max(rms))
        
        #  DURATION PATTERN 
        # Detect onset events (cry bursts)
        onsets = librosa.onset.onset_detect(
            y=y, sr=sr, units='time'
        )
        burst_count = len(onsets)
        
        # Calculate average burst duration
        if len(onsets) > 1:
            burst_durations = np.diff(onsets)
            avg_burst_duration = float(np.mean(burst_durations))
        else:
            avg_burst_duration = duration
        
        #  SPECTRAL FEATURES 
        spectral_centroid = librosa.feature.spectral_centroid(y=y, sr=sr)[0]
        mean_spectral_centroid = float(np.mean(spectral_centroid))
        
        # Breathiness — measure of noise in signal
        spectral_flatness = librosa.feature.spectral_flatness(y=y)[0]
        breathiness = float(np.mean(spectral_flatness))
        
        return {
            "duration_seconds": round(duration, 2),
            "pitch": {
                "mean_hz": round(mean_pitch, 1),
                "max_hz": round(max_pitch, 1),
                "category": (
                    "VERY_HIGH" if mean_pitch > 600 else
                    "HIGH"      if mean_pitch > THRESHOLDS["neurological_pitch_hz"] else
                    "NORMAL"    if mean_pitch > 200 else
                    "LOW"
                )
            },
            "intensity": {
                "mean": round(mean_intensity, 4),
                "max": round(max_intensity, 4),
                "category": (
                    "WEAK"   if mean_intensity < THRESHOLDS["weak_cry_intensity"] else
                    "NORMAL" if mean_intensity < 0.7 else
                    "STRONG"
                )
            },
            "duration_pattern": {
                "burst_count": burst_count,
                "avg_burst_duration_sec": round(avg_burst_duration, 2),
                "category": (
                    "SHORT_BURSTS"  if avg_burst_duration < THRESHOLDS["burst_duration_max"] and burst_count > 3 else
                    "SUSTAINED"     if avg_burst_duration > 3.0 else
                    "INTERMITTENT"  if burst_count > 1 else
                    "CONTINUOUS"
                )
            },
            "breathiness": {
                "score": round(breathiness, 4),
                "category": (
                    "SIGNIFICANT" if breathiness > 0.5 else
                    "MILD"        if breathiness > 0.3 else
                    "NONE"
                )
            },
            "spectral_centroid_hz": round(mean_spectral_centroid, 1)
        }
        
    except Exception as e:
        return {"error": str(e), "duration_seconds": 0}


print("✅ Feature extractor ready")
print("   Extracts: pitch, intensity, duration pattern, breathiness")

# 
# CELL 4 — Rule-based classifier
# Used when Gemma API unavailable (offline fallback)
# 

def classify_cry_rules(features: dict) -> tuple:
    """
    Rule-based cry classification based on clinical research.
    This is the offline fallback when Gemma 4 is not available.
    
    Returns:
        (cry_type, confidence, thinking_trace)
    """
    if "error" in features:
        return "UNKNOWN", 0.3, f"Feature extraction failed: {features['error']}"
    
    pitch_cat = features["pitch"]["category"]
    intensity_cat = features["intensity"]["category"]
    duration_cat = features["duration_pattern"]["category"]
    breathiness_cat = features["breathiness"]["category"]
    burst_count = features["duration_pattern"]["burst_count"]
    
    trace = []
    trace.append("Rule-based cry analysis (offline mode):")
    trace.append(f"Pitch: {pitch_cat} ({features['pitch']['mean_hz']:.0f} Hz)")
    trace.append(f"Intensity: {intensity_cat}")
    trace.append(f"Pattern: {duration_cat} ({burst_count} bursts)")
    trace.append(f"Breathiness: {breathiness_cat}")
    trace.append("")
    
    #  NEUROLOGICAL DISTRESS 
    # High pitch + short bursts = meningitis indicator
    # Source: Uppsala University neonatal cry research
    if (pitch_cat in ["HIGH", "VERY_HIGH"] and
        duration_cat == "SHORT_BURSTS" and
        burst_count >= 3):
        
        confidence = 0.82 if pitch_cat == "VERY_HIGH" else 0.72
        trace.append("PATTERN MATCH: High-pitched short bursts")
        trace.append("Clinical reference: Uppsala University — pathological cry acoustics")
        trace.append("Flag: POSSIBLE_MENINGITIS")
        trace.append(f"Confidence: {confidence}")
        return "NEUROLOGICAL_DISTRESS", confidence, "\n".join(trace)
    
    #  RESPIRATORY DISTRESS 
    # Weak + breathy = respiratory compromise
    if (intensity_cat == "WEAK" and
        breathiness_cat in ["MILD", "SIGNIFICANT"]):
        
        confidence = 0.78 if breathiness_cat == "SIGNIFICANT" else 0.68
        trace.append("PATTERN MATCH: Weak breathy cry")
        trace.append("Clinical reference: WHO Essential Newborn Care — respiratory signs")
        trace.append("Flag: RESPIRATORY_DISTRESS")
        trace.append(f"Confidence: {confidence}")
        return "RESPIRATORY_DISTRESS", confidence, "\n".join(trace)
    
    #  PAIN CRY 
    # High pitch + sustained = pain indicator
    if (pitch_cat in ["HIGH", "VERY_HIGH"] and
        duration_cat in ["SUSTAINED", "CONTINUOUS"]):
        
        confidence = 0.71
        trace.append("PATTERN MATCH: High-pitched sustained cry")
        trace.append("Flag: PAIN_INVESTIGATION_NEEDED")
        return "PAIN", confidence, "\n".join(trace)
    
    #  HUNGER CRY 
    # Normal pitch + rhythmic = hunger
    if (pitch_cat == "NORMAL" and
        duration_cat in ["SHORT_BURSTS", "INTERMITTENT"] and
        intensity_cat in ["NORMAL", "STRONG"]):
        
        confidence = 0.75
        trace.append("PATTERN MATCH: Rhythmic normal-pitch cry")
        trace.append("Classification: HUNGER (no clinical flag)")
        return "HUNGER", confidence, "\n".join(trace)
    
    #  NORMAL FUSSING 
    if intensity_cat in ["WEAK", "NORMAL"] and pitch_cat == "NORMAL":
        confidence = 0.65
        trace.append("PATTERN MATCH: Low intensity normal pitch")
        trace.append("Classification: NORMAL_FUSSING (no clinical flag)")
        return "NORMAL_FUSSING", confidence, "\n".join(trace)
    
    #  UNKNOWN 
    trace.append("No pattern match found")
    trace.append("Recommend: Record longer audio sample for better classification")
    return "UNKNOWN", 0.40, "\n".join(trace)


print("✅ Rule-based classifier ready")

# 
# CELL 5 — Output contract builder
# Produces cry_classification_schema.json
# 

def make_cry_result(
    patient_id: str,
    cry_type: str,
    confidence: float,
    audio_seconds: float,
    features: dict = None,
    thinking_trace: str = "",
    layer: str = "NEWBORN"
) -> dict:
    """
    Builds the cry_classification_schema.json contract output.
    This is exactly what FE displays and BE-2 consumes.
    Never change field names without team agreement.
    """
    clinical_flag = CRY_CLINICAL_FLAGS.get(cry_type) is not None
    clinical_concern = CRY_CLINICAL_FLAGS.get(cry_type)
    
    action_map = {
        "POSSIBLE_MENINGITIS": (
            "High-pitched short cries detected. Possible neurological distress. "
            "Assess fontanelle for bulging. Check temperature. "
            "Consider immediate referral."
        ),
        "RESPIRATORY_DISTRESS": (
            "Weak or breathy cry detected. Possible respiratory distress. "
            "Check breathing rate. Look for chest indrawing. "
            "Refer immediately if breathing rate above 60/min."
        ),
        "PAIN_INVESTIGATION_NEEDED": (
            "Sustained pain cry detected. "
            "Investigate source of pain. Check for injuries or infection."
        ),
        "NEUROLOGICAL_CONCERN": (
            "Expected cry absent after stimulus. "
            "Neurological assessment needed immediately."
        )
    }
    
    return {
        "classification_id": str(uuid.uuid4()),
        "patient_id": patient_id,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "layer": layer,
        "cry_type": cry_type,
        "cry_characteristics": features or {},
        "clinical_flag": clinical_flag,
        "clinical_concern": clinical_concern,
        "recommended_action": action_map.get(clinical_concern),
        "confidence": round(confidence, 3),
        "audio_duration_seconds": round(audio_seconds, 1),
        "gemma_thinking_trace": thinking_trace
    }


#  TEST THE FULL PIPELINE 
print("\n" + "═"*50)
print("TESTING CRY CLASSIFICATION PIPELINE")
print("═"*50)

# Simulate features of a neurological distress cry
test_features_neuro = {
    "duration_seconds": 8.5,
    "pitch": {
        "mean_hz": 580.0,
        "max_hz": 720.0,
        "category": "VERY_HIGH"
    },
    "intensity": {
        "mean": 0.55,
        "max": 0.89,
        "category": "NORMAL"
    },
    "duration_pattern": {
        "burst_count": 6,
        "avg_burst_duration_sec": 0.9,
        "category": "SHORT_BURSTS"
    },
    "breathiness": {
        "score": 0.12,
        "category": "NONE"
    },
    "spectral_centroid_hz": 1850.0
}

cry_type, confidence, trace = classify_cry_rules(test_features_neuro)
result = make_cry_result(
    patient_id="patient_test_001_newborn",
    cry_type=cry_type,
    confidence=confidence,
    audio_seconds=8.5,
    features=test_features_neuro,
    thinking_trace=trace
)

print(f"\nTest 1 — Neurological Distress Cry:")
print(f"  Cry type:        {result['cry_type']}")
print(f"  Clinical flag:   {result['clinical_flag']}")
print(f"  Concern:         {result['clinical_concern']}")
print(f"  Confidence:      {result['confidence']}")
print(f"\nThinking trace:")
print(result['gemma_thinking_trace'])

# Test 2 — Hunger cry
test_features_hunger = {
    "duration_seconds": 5.0,
    "pitch": {"mean_hz": 320.0, "max_hz": 450.0, "category": "NORMAL"},
    "intensity": {"mean": 0.55, "max": 0.75, "category": "NORMAL"},
    "duration_pattern": {"burst_count": 4, "avg_burst_duration_sec": 1.1, "category": "SHORT_BURSTS"},
    "breathiness": {"score": 0.15, "category": "NONE"},
    "spectral_centroid_hz": 1200.0
}

cry_type2, confidence2, trace2 = classify_cry_rules(test_features_hunger)
result2 = make_cry_result(
    patient_id="patient_test_002_newborn",
    cry_type=cry_type2,
    confidence=confidence2,
    audio_seconds=5.0,
    features=test_features_hunger,
    thinking_trace=trace2
)

print(f"\nTest 2 — Hunger Cry:")
print(f"  Cry type:        {result2['cry_type']}")
print(f"  Clinical flag:   {result2['clinical_flag']}")
print(f"  Confidence:      {result2['confidence']}")

print(f"\n{'✅' if result['clinical_flag'] else '❌'} Neurological distress flagged correctly")
print(f"{'✅' if not result2['clinical_flag'] else '❌'} Hunger cry correctly not flagged")
print("\n✅ Cry classification pipeline complete")
