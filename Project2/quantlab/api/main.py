from __future__ import annotations

from pathlib import Path

from fastapi import FastAPI

from quantlab.experiment import load_config, run_experiment

app = FastAPI(title="QuantLab API", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/experiments/demo")
def run_demo_experiment() -> dict[str, object]:
    config_path = Path("configs/sample_strategy.yml")
    return run_experiment(load_config(config_path), Path("outputs") / "api_demo")
