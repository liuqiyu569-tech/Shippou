from __future__ import annotations

from pathlib import Path

import pandas as pd


REQUIRED_COLUMNS = {"date", "symbol", "open", "high", "low", "close", "volume"}


def load_ohlcv_csv(path: str | Path) -> pd.DataFrame:
    data = pd.read_csv(path)
    missing = REQUIRED_COLUMNS.difference(data.columns)
    if missing:
        raise ValueError(f"Missing required columns: {sorted(missing)}")
    data["date"] = pd.to_datetime(data["date"])
    return clean_market_data(data)


def clean_market_data(data: pd.DataFrame) -> pd.DataFrame:
    data = data.copy()
    data["date"] = pd.to_datetime(data["date"])
    data = data.drop_duplicates(["date", "symbol"]).sort_values(["symbol", "date"])
    numeric_columns = ["open", "high", "low", "close", "volume"]
    data[numeric_columns] = data.groupby("symbol")[numeric_columns].ffill()
    data = data.dropna(subset=["open", "high", "low", "close"])
    data = data[data["close"] > 0]
    data = data[data["volume"] >= 0]
    return data.sort_values(["date", "symbol"]).reset_index(drop=True)
