#!/usr/bin/env python3
"""
Bangla-Gemma to LiteRT (.task) Compiler & Bundler
=================================================
Converts an open-source Bengali fine-tuned Gemma model (e.g. emonHasan01/bangla-gemma-pro
or google/gemma-2-2b-it) into a mobile-ready MediaPipe GenAI .task bundle for Android.

Pipeline:
  1. Downloads checkpoint & tokenizer from Hugging Face.
  2. Converts & quantizes PyTorch weights to INT4 / INT8 TensorFlow Lite flatbuffer.
  3. Bundles TFLite graph, SentencePiece tokenizer, and Gemma chat prompt templates into .task.
  4. Saves directly to the Android app assets directory.

Usage:
  python3 scripts/compile_bangla_gemma_litertlm.py --model_id emonHasan01/bangla-gemma-pro --quant int4
  python3 scripts/compile_bangla_gemma_litertlm.py --model_id google/gemma-2-2b-it --quant int8
"""

import os
import sys
import argparse
import shutil
from pathlib import Path

def parse_args():
    parser = argparse.ArgumentParser(description="Compile Bengali Gemma model to LiteRT .task bundle")
    parser.add_argument(
        "--model_id",
        type=str,
        default="emonHasan01/bangla-gemma-pro",
        help="Hugging Face model ID (e.g. 'emonHasan01/bangla-gemma-pro' or 'google/gemma-2-2b-it')"
    )
    parser.add_argument(
        "--quant",
        type=str,
        choices=["int4", "int8", "dynamic_int8"],
        default="int4",
        help="Quantization recipe: 'int4' (smallest, ~1.1GB) or 'int8' (~2.2GB)"
    )
    parser.add_argument(
        "--output_name",
        type=str,
        default="Bangla-Gemma-2B-Instruct.task",
        help="Output filename for the compiled .task bundle"
    )
    parser.add_argument(
        "--output_dir",
        type=str,
        default="../app/src/main/assets",
        help="Destination directory (default: app/src/main/assets)"
    )
    parser.add_argument(
        "--work_dir",
        type=str,
        default="./bangla_gemma_work",
        help="Temporary directory for intermediate artifacts"
    )
    return parser.parse_args()

def check_dependencies():
    print("--> Checking required conversion libraries...")
    missing = []
    for pkg in ["torch", "transformers", "huggingface_hub"]:
        try:
            __import__(pkg)
        except ImportError:
            missing.append(pkg)

    mediapipe_ok = False
    try:
        import mediapipe as mp
        from mediapipe.tasks.python.genai import bundler
        mediapipe_ok = True
    except (ImportError, AttributeError):
        missing.append("mediapipe>=0.10.14")

    ai_edge_ok = False
    for candidate in ["litert_torch", "ai_edge_torch"]:
        try:
            __import__(candidate)
            ai_edge_ok = True
            break
        except ImportError:
            pass

    if not ai_edge_ok:
        missing.append("ai-edge-torch OR litert-torch")

    if missing:
        print(f"\n[!] Missing dependencies: {', '.join(missing)}")
        print("\nPlease install them using:")
        print("  pip install ai-edge-torch mediapipe transformers huggingface_hub sentencepiece\n")
        return False
    return True

def step1_download_tokenizer(model_id: str, work_dir: Path) -> Path:
    print(f"\n[Step 1/3] Downloading tokenizer for '{model_id}'...")
    from huggingface_hub import hf_hub_download

    work_dir.mkdir(parents=True, exist_ok=True)
    tokenizer_path = work_dir / "tokenizer.model"

    # Attempt to download tokenizer.model directly
    try:
        downloaded = hf_hub_download(repo_id=model_id, filename="tokenizer.model", local_dir=str(work_dir))
        print(f"  -> Successfully downloaded tokenizer: {downloaded}")
        return Path(downloaded)
    except Exception as e:
        print(f"  -> tokenizer.model not directly at root ({e}). Trying base google/gemma-2-2b...")
        downloaded = hf_hub_download(repo_id="google/gemma-2-2b", filename="tokenizer.model", local_dir=str(work_dir))
        return Path(downloaded)

def step2_convert_to_tflite(model_id: str, quant: str, work_dir: Path) -> Path:
    print(f"\n[Step 2/3] Converting PyTorch weights to TFLite (quantization={quant})...")
    tflite_output = work_dir / "gemma_converted.tflite"

    if tflite_output.exists():
        print(f"  -> Found existing TFLite graph: {tflite_output} (skipping reconversion)")
        return tflite_output

    # Use ai_edge_torch or litert_torch
    try:
        import ai_edge_torch
        from ai_edge_torch.generative.examples.gemma import gemma
        print("  -> Using ai_edge_torch generative converter...")

        # Load Hugging Face PyTorch checkpoint
        print(f"  -> Loading model from Hugging Face: {model_id}")
        from transformers import AutoModelForCausalLM
        hf_model = AutoModelForCausalLM.from_pretrained(
            model_id,
            torch_dtype="auto",
            device_map="cpu",
            low_cpu_mem_usage=True
        )

        # Export with quantization
        print(f"  -> Tracing graph & quantizing to {quant}...")
        quant_recipe = ai_edge_torch.quantize.quant_recipes.full_int8_weight_only() if quant == "int8" else ai_edge_torch.quantize.quant_recipes.int4_weight_only()
        
        edge_model = ai_edge_torch.convert(hf_model, quant_config=quant_recipe)
        edge_model.export(str(tflite_output))
        print(f"  -> TFLite model generated: {tflite_output}")
        return tflite_output

    except Exception as e:
        print(f"  -> Programmatic export failed: {e}")
        print("\n[Alternative Command Line Export]")
        print("You can also run the official Google AI Edge CLI conversion command:")
        print(f"  python -m ai_edge_torch.generative.examples.gemma.convert_to_tflite \\")
        print(f"      --checkpoint_path={model_id} \\")
        print(f"      --output_path={tflite_output} \\")
        print(f"      --quantize={quant}")
        raise

def step3_bundle_mediapipe_task(
    tflite_path: Path,
    tokenizer_path: Path,
    output_task_path: Path
):
    print(f"\n[Step 3/3] Packaging into MediaPipe GenAI .task bundle...")
    from mediapipe.tasks.python.genai import bundler

    output_task_path.parent.mkdir(parents=True, exist_ok=True)

    config = bundler.BundleConfig(
        tflite_model=str(tflite_path),
        tokenizer_model=str(tokenizer_path),
        start_token="<bos>",
        stop_tokens=["<eos>", "<end_of_turn>"],
        output_filename=str(output_task_path),
        prompt_prefix="<start_of_turn>user\n",
        prompt_suffix="<end_of_turn>\n<start_of_turn>model\n",
    )

    bundler.create_bundle(config)
    size_mb = output_task_path.stat().st_size / (1024 * 1024)
    print(f"\n[+] SUCCESS! Generated on-device model bundle:")
    print(f"    Path: {output_task_path}")
    print(f"    Size: {size_mb:.1f} MB")
    print(f"\nYou can now update GemmaEngine.kt MODEL_FILENAME to: '{output_task_path.name}'")

def main():
    args = parse_args()
    print("=" * 65)
    print("   Bangla-Gemma -> MediaPipe LiteRT (.task) Compiler")
    print("=" * 65)
    print(f"Model ID   : {args.model_id}")
    print(f"Quant      : {args.quant}")
    print(f"Destination: {args.output_dir}/{args.output_name}")

    if not check_dependencies():
        sys.exit(1)

    work_dir = Path(args.work_dir).resolve()
    dest_dir = Path(args.output_dir).resolve()
    final_task_path = dest_dir / args.output_name

    try:
        tokenizer_path = step1_download_tokenizer(args.model_id, work_dir)
        tflite_path = step2_convert_to_tflite(args.model_id, args.quant, work_dir)
        step3_bundle_mediapipe_task(tflite_path, tokenizer_path, final_task_path)
    except Exception as ex:
        print(f"\n[!] Compilation terminated with error: {ex}")
        sys.exit(1)

if __name__ == "__main__":
    main()
