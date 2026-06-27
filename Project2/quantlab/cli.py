from __future__ import annotations

import argparse
import json

from quantlab.experiment import load_config, run_experiment


def main() -> None:
    parser = argparse.ArgumentParser(description="Run a QuantLab factor research experiment.")
    parser.add_argument("--config", default="configs/sample_strategy.yml")
    parser.add_argument("--output", default="outputs")
    args = parser.parse_args()

    summary = run_experiment(load_config(args.config), args.output)
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
