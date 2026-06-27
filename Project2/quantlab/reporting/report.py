from __future__ import annotations

from pathlib import Path

import pandas as pd
from jinja2 import Template

from quantlab.backtest.engine import BacktestResult


REPORT_TEMPLATE = """<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <title>{{ name }} QuantLab Report</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 32px; color: #1f2933; }
    h1, h2 { margin-bottom: 8px; }
    table { border-collapse: collapse; width: 100%; margin: 16px 0 28px; }
    th, td { border: 1px solid #d6dde5; padding: 8px 10px; text-align: right; }
    th:first-child, td:first-child { text-align: left; }
    th { background: #f4f7fb; }
    .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
    .metric { border: 1px solid #d6dde5; padding: 14px; border-radius: 6px; }
    .label { color: #637083; font-size: 12px; }
    .value { font-size: 20px; font-weight: 700; margin-top: 6px; }
  </style>
</head>
<body>
  <h1>{{ name }} 回测报告</h1>
  <p>本报告由 QuantLab 自动生成，覆盖收益、回撤、换手率、因子 IC 与交易样本。</p>
  <div class="grid">
    {% for key, value in metrics.items() %}
    <div class="metric">
      <div class="label">{{ key }}</div>
      <div class="value">{{ "%.4f"|format(value) }}</div>
    </div>
    {% endfor %}
  </div>
  <h2>因子 RankIC</h2>
  {{ ic_table }}
  <h2>最近净值</h2>
  {{ equity_tail }}
  <h2>最近交易</h2>
  {{ trades_tail }}
</body>
</html>
"""


def write_html_report(
    output_dir: Path,
    experiment_name: str,
    result: BacktestResult,
    ic_table: pd.DataFrame,
) -> Path:
    template = Template(REPORT_TEMPLATE)
    html = template.render(
        name=experiment_name,
        metrics=result.metrics,
        ic_table=ic_table.round(4).to_html(index=False),
        equity_tail=_round_numeric(result.equity_curve.tail(10)).to_html(index=False),
        trades_tail=_round_numeric(result.trades.tail(10)).to_html(index=False),
    )
    path = output_dir / f"{experiment_name}_report.html"
    path.write_text(html, encoding="utf-8")
    return path


def _round_numeric(frame: pd.DataFrame) -> pd.DataFrame:
    rounded = frame.copy()
    numeric_columns = rounded.select_dtypes(include="number").columns
    rounded[numeric_columns] = rounded[numeric_columns].round(4)
    return rounded
