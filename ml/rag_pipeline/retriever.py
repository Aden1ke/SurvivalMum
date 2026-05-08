# SurviveMum — RAG Pipeline
# CELL 6 — Install RAG dependencies
!pip install -q faiss-cpu sentence-transformers PyPDF2

from sentence_transformers import SentenceTransformer
import faiss
import numpy as np
import json

print("✅ RAG dependencies ready")

# 
# CELL 7 — Full clinical knowledge base
# Replace sample texts with real WHO PDF content
# 

KNOWLEDGE_BASE = [
    #  PREECLAMPSIA / HYPERTENSION 
    {
        "id": "who_bp_threshold",
        "source": "WHO ANC Guidelines 2022 — Section 4.3",
        "topic": "preeclampsia",
        "text": (
            "Systolic blood pressure at or above 140 mmHg or diastolic "
            "at or above 90 mmHg after 20 weeks of gestation, measured "
            "on two occasions 4 hours apart, constitutes hypertension "
            "in pregnancy. Combined with proteinuria or end-organ damage "
            "this constitutes preeclampsia. Immediate referral is required."
        )
    },
    {
        "id": "who_bp_trend",
        "source": "WHO ANC Guidelines 2022 — Section 4.3",
        "topic": "preeclampsia",
        "text": (
            "A rise in systolic blood pressure of 30 mmHg or diastolic "
            "of 15 mmHg above baseline values across consecutive visits "
            "is clinically significant even if absolute thresholds are "
            "not yet reached. This trend warrants increased monitoring "
            "frequency and early referral planning."
        )
    },
    {
        "id": "who_preeclampsia_signs",
        "source": "WHO ANC Guidelines 2022 — Section 4.3",
        "topic": "preeclampsia",
        "text": (
            "Warning signs of preeclampsia include severe headache, "
            "visual disturbances such as blurred vision or flashing lights, "
            "upper abdominal or epigastric pain, facial or hand oedema, "
            "and rapid weight gain due to fluid retention. Any combination "
            "of these with elevated blood pressure requires immediate assessment."
        )
    },
    {
        "id": "mdsr_preeclampsia_nigeria",
        "source": "Nigeria MDSR Annual Report — Kano State Summary",
        "topic": "preeclampsia",
        "text": (
            "Hypertensive disorders of pregnancy including preeclampsia "
            "and eclampsia account for approximately 11 percent of maternal "
            "deaths in Nigeria. The majority of deaths occur at community "
            "level before referral. Early warning signs including rising "
            "blood pressure trend and facial oedema are frequently missed "
            "during antenatal visits by community health workers."
        )
    },
    
    #  POSTPARTUM HAEMORRHAGE 
    {
        "id": "who_pph",
        "source": "WHO Guidelines for Management of PPH 2022",
        "topic": "haemorrhage",
        "text": (
            "Postpartum haemorrhage is defined as blood loss of 500 mL or "
            "more within 24 hours after birth. Severe PPH involves blood "
            "loss of 1000 mL or more. Risk factors include uterine atony, "
            "retained placenta, lacerations, and coagulation disorders. "
            "Uterine atony is the most common cause accounting for 70-80% of cases. "
            "Immediate intervention and referral are required."
        )
    },
    {
        "id": "mdsr_pph_nigeria",
        "source": "Nigeria MDSR — Leading Causes of Maternal Death",
        "topic": "haemorrhage",
        "text": (
            "Postpartum haemorrhage is the leading cause of maternal death "
            "in Nigeria, accounting for approximately 23 percent of all "
            "maternal deaths. Grand multiparity defined as five or more "
            "previous deliveries is a significant risk factor in Nigerian "
            "communities. Traditional birth attendants must refer any case "
            "of excessive bleeding immediately without delay."
        )
    },
    
    #  ANAEMIA 
    {
        "id": "who_anaemia",
        "source": "WHO ANC Guidelines 2022 — Section 3.1",
        "topic": "anaemia",
        "text": (
            "Anaemia in pregnancy is defined as haemoglobin concentration "
            "below 11 g/dL in the first and third trimesters. Severe anaemia "
            "is haemoglobin below 7 g/dL and requires immediate intervention. "
            "Clinical signs include pallor of the inner eyelids, lips, and "
            "palms, fatigue, and shortness of breath. Iron and folic acid "
            "supplementation is recommended for all pregnant women."
        )
    },
    
    #  NEWBORN RESPIRATORY 
    {
        "id": "who_newborn_breathing",
        "source": "WHO Essential Newborn Care 2024",
        "topic": "newborn_respiratory",
        "text": (
            "Normal newborn breathing rate is 30 to 60 breaths per minute "
            "in the first hours of life. A rate above 60 breaths per minute "
            "indicates respiratory distress. Additional signs include chest "
            "indrawing where the chest wall sucks inward with each breath, "
            "grunting sounds on expiration, nasal flaring, and central cyanosis. "
            "Any newborn with respiratory distress requires immediate referral."
        )
    },
    {
        "id": "who_first_cry",
        "source": "WHO Essential Newborn Care 2024",
        "topic": "newborn_cry",
        "text": (
            "A healthy newborn should cry vigorously within 30 seconds of birth. "
            "Absence of cry or a weak cry requires immediate assessment. "
            "Begin newborn resuscitation if no cry within 60 seconds. "
            "High-pitched cry or abnormal cry may indicate neurological "
            "irritability, intracranial pathology, or systemic illness "
            "and requires urgent clinical evaluation."
        )
    },
    
    #  NEWBORN JAUNDICE 
    {
        "id": "who_jaundice",
        "source": "WHO Newborn Care Guidelines",
        "topic": "newborn_jaundice",
        "text": (
            "Physiological jaundice in newborns typically appears after "
            "24 hours and peaks between days 3 to 5. Jaundice appearing "
            "within 24 hours of birth is pathological and requires urgent "
            "assessment for haemolytic disease. Clinical assessment includes "
            "yellowing of the sclera and skin. Jaundice extending to the palms "
            "and soles indicates severe hyperbilirubinaemia requiring immediate "
            "phototherapy or exchange transfusion."
        )
    },
    
    #  NEWBORN INFECTION / MENINGITIS 
    {
        "id": "who_neonatal_infection",
        "source": "WHO Pocket Book of Hospital Care — Neonatal Infections",
        "topic": "neonatal_infection",
        "text": (
            "Signs of serious bacterial infection in newborns include "
            "high-pitched cry, bulging fontanelle, seizures, temperature "
            "instability, poor feeding, and decreased activity. "
            "Bacterial meningitis may present with high-pitched cry, "
            "bulging anterior fontanelle, and neck stiffness in older infants. "
            "Any newborn with these signs requires immediate referral for "
            "blood culture and intravenous antibiotic therapy."
        )
    },
    
    #  SEPSIS 
    {
        "id": "who_maternal_sepsis",
        "source": "WHO Recommendations for Prevention and Treatment of Maternal Sepsis",
        "topic": "sepsis",
        "text": (
            "Maternal sepsis is a life-threatening organ dysfunction caused "
            "by infection during pregnancy, childbirth, or after delivery. "
            "Signs include temperature above 38 degrees C or below 36, "
            "heart rate above 90 beats per minute, respiratory rate above 20, "
            "and altered mental status. Sepsis following delivery is a common "
            "cause of maternal death and requires immediate IV antibiotics "
            "and urgent referral."
        )
    },
    
    #  REFERRAL 
    {
        "id": "who_emergency_referral",
        "source": "WHO Emergency Triage Assessment — Obstetric Emergencies",
        "topic": "referral",
        "text": (
            "Immediate referral is indicated for any of the following: "
            "blood pressure at or above 160/110 mmHg, severe headache "
            "with visual disturbance, heavy vaginal bleeding, absent fetal "
            "movement, convulsions, loss of consciousness, or severe breathing "
            "difficulty. Transfer should not be delayed for treatment that "
            "can be initiated at the referral facility."
        )
    },
    
    #  NIGERIA MDSR PATTERNS 
    {
        "id": "mdsr_delay_factors",
        "source": "Nigeria MDSR — Three Delays Analysis",
        "topic": "access",
        "text": (
            "The three delays model explains most maternal deaths in Nigeria. "
            "First delay: failure to recognise danger signs and decide to seek care. "
            "Second delay: inability to reach healthcare facility due to distance, "
            "transport, or cost. Third delay: failure to receive adequate care "
            "at the facility. Community-based monitoring and early alert systems "
            "directly address the first delay which accounts for the majority "
            "of preventable maternal deaths."
        )
    },
]

print(f"✅ Knowledge base loaded: {len(KNOWLEDGE_BASE)} clinical documents")
print("\nTopics covered:")
topics = list(set([d["topic"] for d in KNOWLEDGE_BASE]))
for t in sorted(topics):
    count = len([d for d in KNOWLEDGE_BASE if d["topic"] == t])
    print(f"  {t}: {count} document(s)")

# 
# CELL 8 — Build FAISS index
# 

print("\nLoading embedding model...")
embedder = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
print("✅ Embedding model loaded")

texts = [doc["text"] for doc in KNOWLEDGE_BASE]
print(f"\nEmbedding {len(texts)} clinical documents...")
embeddings = embedder.encode(texts, show_progress_bar=True)
embeddings = embeddings.astype(np.float32)

# Build FAISS index
dimension = embeddings.shape[1]
index = faiss.IndexFlatL2(dimension)
index.add(embeddings)

print(f"\n✅ FAISS index built")
print(f"   {index.ntotal} documents indexed")
print(f"   Embedding dimension: {dimension}")

# 
# CELL 9 — Retriever function
# 

def retrieve_guidelines(query: str, top_k: int = 3) -> list:
    """
    Retrieve the most relevant clinical guidelines
    for a given clinical situation.
    
    This runs BEFORE every Gemma 4 assessment.
    Stops hallucination. Grounds every recommendation
    in verifiable, retrievable clinical evidence.
    
    Args:
        query: Clinical situation description
        top_k: Number of guidelines to retrieve
    
    Returns:
        List of relevant guideline documents with scores
    """
    query_embedding = embedder.encode(
        [query], show_progress_bar=False
    ).astype(np.float32)
    
    distances, indices = index.search(query_embedding, k=top_k)
    
    results = []
    for i, idx in enumerate(indices[0]):
        doc = KNOWLEDGE_BASE[idx]
        similarity = float(1 / (1 + distances[0][i]))
        results.append({
            "rank": i + 1,
            "id": doc["id"],
            "source": doc["source"],
            "topic": doc["topic"],
            "text": doc["text"],
            "similarity_score": round(similarity, 3)
        })
    
    return results


def format_for_gemma(
    clinical_query: str,
    retrieved_docs: list,
    patient_name: str = "the patient"
) -> str:
    """
    Format retrieved guidelines + clinical situation
    into a Gemma 4 prompt that produces:
    - Visible thinking traces
    - Grounded clinical recommendations
    - Risk level assessment
    - Specific action for TBA
    """
    guidelines_text = "\n\n".join([
        f"[Guideline {doc['rank']} — {doc['source']}]\n{doc['text']}"
        for doc in retrieved_docs
    ])
    
    return f"""You are SurviveMum, a clinical AI guardian for patients 
who cannot speak. You monitor mothers, newborns, and toddlers in 
rural Nigeria where no doctor is present.

RETRIEVED CLINICAL GUIDELINES:
{guidelines_text}

CLINICAL SITUATION FOR {patient_name.upper()}:
{clinical_query}

Using ONLY the guidelines retrieved above, provide your assessment.
Show your reasoning step by step inside <thinking> tags.
Then provide your final assessment in this exact format:

<thinking>
[Your step-by-step clinical reasoning here]
[Reference which specific guideline you are applying]
[State what indicators are present]
[State what threshold or pattern is matched]
</thinking>

RISK_LEVEL: [CRITICAL / HIGH / MEDIUM / LOW]
ALERT_TYPE: [specific condition detected]
GUIDELINE_CITED: [source of the primary guideline used]
ACTION: [specific instruction for the TBA in plain language]"""


#  TEST THE RAG PIPELINE 
print("\n" + "═"*50)
print("TESTING RAG PIPELINE")
print("═"*50)

test_queries = [
    (
        "Pregnant woman at 32 weeks. BP readings across 4 visits: "
        "118/76, 128/84, 138/90, 145/95. Facial swelling detected "
        "by camera. Patient reports persistent headache.",
        "Preeclampsia scenario"
    ),
    (
        "Newborn baby 6 hours old. Very high-pitched cry in short "
        "bursts. Fontanelle appears slightly bulging. Temperature 38.2C.",
        "Meningitis scenario"
    ),
    (
        "Mother delivered 2 hours ago. Heavy vaginal bleeding continuing. "
        "Estimated blood loss appears more than 500ml.",
        "PPH scenario"
    )
]

for query, scenario in test_queries:
    results = retrieve_guidelines(query)
    print(f"\n{scenario}:")
    print(f"  Query: {query[:60]}...")
    print(f"  Top retrieved guideline:")
    print(f"    Source: {results[0]['source']}")
    print(f"    Topic:  {results[0]['topic']}")
    print(f"    Score:  {results[0]['similarity_score']}")
    
    # Verify correct guideline retrieved
    expected_topics = {
        "Preeclampsia scenario": "preeclampsia",
        "Meningitis scenario": "neonatal_infection",
        "PPH scenario": "haemorrhage"
    }
    correct = results[0]['topic'] == expected_topics[scenario]
    print(f"  {'✅' if correct else '❌'} Correct guideline topic: {results[0]['topic']}")

print("\n✅ RAG pipeline complete")
