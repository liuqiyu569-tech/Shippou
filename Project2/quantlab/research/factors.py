from __future__ import annotations

import numpy as np
import pandas as pd


FACTOR_COLUMNS = [
    "momentum_20",
    "reversal_5",
    "volatility_20",
    "volume_zscore_20",
    "ma_gap_20",
    "rsi_14",
]


def add_factor_columns(data: pd.DataFrame) -> pd.DataFrame:
    df = data.sort_values(["symbol", "date"]).copy()
    grouped = df.groupby("symbol", group_keys=False)
    df["return_1d"] = grouped["close"].pct_change()
    df["future_return_5d"] = grouped["close"].pct_change(5).shift(-5)
    df["momentum_20"] = grouped["close"].pct_change(20)
    df["reversal_5"] = -grouped["close"].pct_change(5)
    df["volatility_20"] = grouped["return_1d"].rolling(20).std().reset_index(level=0, drop=True)

    volume_mean = grouped["volume"].rolling(20).mean().reset_index(level=0, drop=True)
    volume_std = grouped["volume"].rolling(20).std().reset_index(level=0, drop=True)
    df["volume_zscore_20"] = (df["volume"] - volume_mean) / volume_std.replace(0, np.nan)

    moving_average = grouped["close"].rolling(20).mean().reset_index(level=0, drop=True)
    df["ma_gap_20"] = df["close"] / moving_average - 1
    df["rsi_14"] = grouped.apply(_rsi_14).reset_index(level=0, drop=True)
    return cross_sectional_standardize(df, FACTOR_COLUMNS)


def cross_sectional_standardize(data: pd.DataFrame, columns: list[str]) -> pd.DataFrame:
    df = data.copy()
    for column in columns:
        ranks = df.groupby("date")[column].rank(pct=True)
        df[f"{column}_rank"] = ranks
        mean = df.groupby("date")[column].transform("mean")
        std = df.groupby("date")[column].transform("std").replace(0, np.nan)
        df[f"{column}_z"] = ((df[column] - mean) / std).clip(-3, 3)
    return df


def build_composite_signal(data: pd.DataFrame) -> pd.DataFrame:
    df = data.copy()
    signal_columns = [
        "momentum_20_z",
        "reversal_5_z",
        "volume_zscore_20_z",
        "ma_gap_20_z",
        "rsi_14_z",
    ]
    df["factor_score"] = df[signal_columns].mean(axis=1) - 0.35 * df["volatility_20_z"]
    return df


def factor_ic_table(data: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for factor in FACTOR_COLUMNS + ["factor_score"]:
        daily_ic = (
            data.dropna(subset=[factor, "future_return_5d"])
            .groupby("date")
            .apply(lambda x: x[factor].corr(x["future_return_5d"], method="spearman"))
            .dropna()
        )
        rows.append(
            {
                "factor": factor,
                "mean_rank_ic": daily_ic.mean(),
                "ic_std": daily_ic.std(),
                "ic_ir": daily_ic.mean() / daily_ic.std() if daily_ic.std() else np.nan,
                "observations": int(daily_ic.count()),
            }
        )
    return pd.DataFrame(rows).sort_values("mean_rank_ic", ascending=False)


def _rsi_14(frame: pd.DataFrame) -> pd.Series:
    delta = frame["close"].diff()
    gain = delta.clip(lower=0).rolling(14).mean()
    loss = (-delta.clip(upper=0)).rolling(14).mean()
    rs = gain / loss.replace(0, np.nan)
    return 100 - 100 / (1 + rs)
