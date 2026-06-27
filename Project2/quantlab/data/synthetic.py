from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import pandas as pd


@dataclass(frozen=True)
class SyntheticMarketConfig:
    symbols: list[str]
    start: str = "2022-01-03"
    periods: int = 520
    seed: int = 42


def generate_market_data(config: SyntheticMarketConfig) -> pd.DataFrame:
    """Generate deterministic OHLCV data with asset-specific drift and volatility."""
    rng = np.random.default_rng(config.seed)
    dates = pd.bdate_range(config.start, periods=config.periods)
    frames: list[pd.DataFrame] = []

    for index, symbol in enumerate(config.symbols):
        drift = rng.normal(0.00025, 0.00012) + index * 0.000015
        volatility = rng.uniform(0.012, 0.028)
        shock = rng.normal(drift, volatility, len(dates))

        market_cycle = 0.004 * np.sin(np.linspace(0, 8 * np.pi, len(dates)) + index / 3)
        returns = shock + market_cycle / 20
        close = 100 * np.exp(np.cumsum(returns))
        open_ = close * (1 + rng.normal(0, 0.002, len(dates)))
        high = np.maximum(open_, close) * (1 + rng.uniform(0.001, 0.012, len(dates)))
        low = np.minimum(open_, close) * (1 - rng.uniform(0.001, 0.012, len(dates)))
        volume = rng.lognormal(mean=13.2 + index * 0.03, sigma=0.35, size=len(dates)).astype(int)

        frames.append(
            pd.DataFrame(
                {
                    "date": dates,
                    "symbol": symbol,
                    "open": open_,
                    "high": high,
                    "low": low,
                    "close": close,
                    "volume": volume,
                }
            )
        )

    data = pd.concat(frames, ignore_index=True)
    return data.sort_values(["date", "symbol"]).reset_index(drop=True)
