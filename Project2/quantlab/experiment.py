from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml

from quantlab.backtest.engine import BacktestConfig, EventDrivenBacktester
from quantlab.data.pipeline import clean_market_data
from quantlab.data.synthetic import SyntheticMarketConfig, generate_market_data
from quantlab.reporting.report import write_html_report
from quantlab.research.factors import add_factor_columns, build_composite_signal, factor_ic_table
from quantlab.research.model import add_rolling_ml_signal


def load_config(path: str | Path) -> dict[str, Any]:
    with Path(path).open("r", encoding="utf-8") as file:
        return yaml.safe_load(file)


def run_experiment(config: dict[str, Any], output_dir: str | Path = "outputs") -> dict[str, Any]:
    output = Path(output_dir)
    output.mkdir(parents=True, exist_ok=True)

    data_config = SyntheticMarketConfig(**config["data"])
    market_data = clean_market_data(generate_market_data(data_config))
    factors = build_composite_signal(add_factor_columns(market_data))

    model_config = config.get("model", {})
    if model_config.get("enabled", False):
        factors = add_rolling_ml_signal(
            factors,
            train_window=int(model_config.get("train_window", 180)),
            prediction_horizon=int(model_config.get("prediction_horizon", 5)),
        )
    else:
        factors["research_score"] = factors["factor_score"]

    experiment_config = config["experiment"]
    backtest = EventDrivenBacktester(
        BacktestConfig(
            start_cash=float(experiment_config.get("start_cash", 1_000_000)),
            rebalance_frequency=int(experiment_config.get("rebalance_frequency", 20)),
            top_k=int(experiment_config.get("top_k", 5)),
            commission_rate=float(experiment_config.get("commission_rate", 0.0003)),
            slippage_rate=float(experiment_config.get("slippage_rate", 0.0005)),
            max_weight_per_asset=float(experiment_config.get("max_weight_per_asset", 0.25)),
        )
    )
    result = backtest.run(factors)
    ic_table = factor_ic_table(factors)

    experiment_name = experiment_config.get("name", "quantlab_experiment")
    result.equity_curve.to_csv(output / f"{experiment_name}_equity.csv", index=False)
    result.trades.to_csv(output / f"{experiment_name}_trades.csv", index=False)
    ic_table.to_csv(output / f"{experiment_name}_factor_ic.csv", index=False)
    report_path = write_html_report(output, experiment_name, result, ic_table)

    return {
        "experiment": experiment_name,
        "metrics": result.metrics,
        "equity_rows": len(result.equity_curve),
        "trade_rows": len(result.trades),
        "report": str(report_path),
    }
