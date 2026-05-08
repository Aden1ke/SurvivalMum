
# SurviveMum — Unsloth Fine-Tuning
# Run this on Colab with GPU runtime (T4 or better)
# Runtime → Change runtime type → T4 GPU


# CELL 12 — Install Unsloth
# Change to GPU runtime first before running this cell
!pip install "unsloth[colab-new] @ git+https://github.com/unslothai/unsloth.git"
!pip install --no-deps xformers trl peft accelerate bitsandbytes

print("✅ Unsloth installed")

# 
# CELL 13 — Load model and prepare for training
# 

from unsloth import FastLanguageModel
from datasets import Dataset
import json
import torch

MAX_SEQ_LENGTH = 2048

print("Loading Gemma 4 with Unsloth...")
model, tokenizer = FastLanguageModel.from_pretrained(
    model_name = "google/gemma-2-2b-it",
    max_seq_length = MAX_SEQ_LENGTH,
    dtype = None,
    load_in_4bit = True,
)

model = FastLanguageModel.get_peft_model(
    model,
    r = 16,
    target_modules = [
        "q_proj", "k_proj", "v_proj", "o_proj",
        "gate_proj", "up_proj", "down_proj"
    ],
    lora_alpha = 32,
    lora_dropout = 0.05,
    bias = "none",
    use_gradient_checkpointing = "unsloth",
    random_state = 42,
)

print("✅ Model loaded with LoRA adapters")
print(f"   Max sequence length: {MAX_SEQ_LENGTH}")

# 
# CELL 14 — Prepare dataset
# 

def format_for_training(record):
    """
    Format each training record into the
    instruction-following chat format Gemma expects.
    """
    messages = record["messages"]
    
    # Format as chat template
    text = tokenizer.apply_chat_template(
        messages,
        tokenize = False,
        add_generation_prompt = False
    )
    return {"text": text}


# Load training data
training_records = []
with open("/content/survivemum_training_data.jsonl") as f:
    for line in f:
        training_records.append(json.loads(line))

# Convert to dataset
dataset = Dataset.from_list(training_records)
dataset = dataset.map(format_for_training)

print(f"✅ Dataset prepared: {len(dataset)} training records")
print(f"   Sample formatted text (first 200 chars):")
print(dataset[0]["text"][:200] + "...")

# 
# CELL 15 — Train the model
# 

from trl import SFTTrainer
from transformers import TrainingArguments

trainer = SFTTrainer(
    model = model,
    tokenizer = tokenizer,
    train_dataset = dataset,
    dataset_text_field = "text",
    max_seq_length = MAX_SEQ_LENGTH,
    dataset_num_proc = 2,
    args = TrainingArguments(
        per_device_train_batch_size = 2,
        gradient_accumulation_steps = 4,
        warmup_steps = 5,
        num_train_epochs = 3,
        learning_rate = 2e-4,
        fp16 = not torch.cuda.is_bf16_supported(),
        bf16 = torch.cuda.is_bf16_supported(),
        logging_steps = 1,
        optim = "adamw_8bit",
        weight_decay = 0.01,
        lr_scheduler_type = "linear",
        seed = 42,
        output_dir = "/content/survivemum_checkpoints",
    ),
)

print("Starting fine-tuning...")
print("This takes approximately 5-10 minutes on T4 GPU")
trainer_stats = trainer.train()

print(f"\n✅ Fine-tuning complete")
print(f"   Training loss: {trainer_stats.training_loss:.4f}")
print(f"   Runtime: {trainer_stats.metrics['train_runtime']:.0f} seconds")

# 
# CELL 16 — Evaluate fine-tuned model
# 

FastLanguageModel.for_inference(model)

def test_model(prompt: str) -> str:
    """Test the fine-tuned model on a clinical scenario."""
    messages = [{"role": "user", "content": prompt}]
    inputs = tokenizer.apply_chat_template(
        messages,
        tokenize = True,
        add_generation_prompt = True,
        return_tensors = "pt"
    ).to("cuda")
    
    outputs = model.generate(
        input_ids = inputs,
        max_new_tokens = 512,
        temperature = 0.1,
        do_sample = True
    )
    
    response = tokenizer.decode(outputs[0], skip_special_tokens = True)
    return response.split("model\n")[-1] if "model\n" in response else response


# Test on preeclampsia scenario
test_prompt = (
    "A 24-year-old woman at 36 weeks has blood pressure 155/100 today. "
    "Previous visits showed 115/75 at week 12 and 130/85 at week 24. "
    "She has a headache and swollen feet. What should the TBA do?"
)

print("\n" + "="*50)
print("EVALUATING FINE-TUNED MODEL")
print("="*50)
print(f"\nTest prompt: {test_prompt}")
print("\nModel response:")
response = test_model(test_prompt)
print(response)

# Check response quality
has_risk_level = "RISK_LEVEL:" in response
has_alert_type = "ALERT_TYPE:" in response
has_action = "ACTION:" in response
has_thinking = "<thinking>" in response

print("\n" + "="*50)
print("EVALUATION RESULTS")
print("="*50)
print(f"✅ Has RISK_LEVEL:  {has_risk_level}")
print(f"✅ Has ALERT_TYPE:  {has_alert_type}")
print(f"✅ Has ACTION:      {has_action}")
print(f"✅ Has thinking:    {has_thinking}")

quality_score = sum([has_risk_level, has_alert_type, has_action, has_thinking])
print(f"\nOutput quality score: {quality_score}/4")
print("✅ Fine-tuning evaluation complete" if quality_score >= 3 else "⚠️ Review model output")

# 
# CELL 17 — Save fine-tuned model
# 

from google.colab import drive
drive.mount('/content/drive')

save_path = "/content/drive/MyDrive/SurviveMum/models/survivemum_finetuned"
model.save_pretrained(save_path)
tokenizer.save_pretrained(save_path)

print(f"✅ Fine-tuned model saved to Google Drive")
print(f"   Path: {save_path}")
print(f"\nShare this folder with BE-1 for LiteRT conversion")
