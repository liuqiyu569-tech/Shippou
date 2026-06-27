from __future__ import annotations

import numpy as np
import pandas as pd


def performance_metrics(equity_curve: pd.DataFrame) -> dict[str, float]:
    if equity_curve.empty:
        return {}

    curve = equity_curve.sort_values("date").copy()
    daily_returns = curve["equity"].pct_change().dropna()
    total_return = curve["equity"].iloc[-1] / curve["equity"].iloc[0] - 1
    years = max((curve["date"].iloc[-1] - curve["date"].iloc[0]).days / 365.25, 1 / 252)
    annual_return = (1 + total_return) ** (1 / years) - 1
    annual_volatility = daily_returns.std() * np.sqrt(252)
    sharpe = annual_return / annual_volatility if annual_volatility else np.nan
    drawdown = max_drawdown(curve["equity"])
    calmar = annual_return / abs(drawdown) if drawdown else np.nan

    return {
        "total_return": float(total_return),
        "annual_return": float(annual_return),
        "annual_volatility": float(annual_volatility),
        "sharpe": float(sharpe),
        "max_drawdown": float(drawdown),
        "calmar": float(calmar),
        "turnover": float(curve["turnover"].mean()) if "turnover" in curve else 0.0,
    }


def max_drawdown(equity: pd.Series) -> float:
    running_max = equity.cummax()
    drawdown = equity / running_max - 1
    return float(drawdown.min())
