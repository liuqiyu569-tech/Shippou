from __future__ import annotations

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor

from quantlab.research.factors import FACTOR_COLUMNS


MODEL_FEATURES = [f"{column}_z" for column in FACTOR_COLUMNS]


def add_rolling_ml_signal(
    data: pd.DataFrame,
    train_window: int = 180,
    prediction_horizon: int = 5,
) -> pd.DataFrame:
    """Train a rolling cross-sectional return model without using future rows."""
    df = data.copy()
    df["target"] = df.groupby("symbol")["close"].pct_change(prediction_horizon).shift(-prediction_horizon)
    df["ml_score"] = np.nan
    dates = sorted(df["date"].unique())

    for index in range(train_window, len(dates) - prediction_horizon):
        train_dates = dates[index - train_window : index]
        predict_date = dates[index]
        train = df[df["date"].isin(train_dates)].dropna(subset=MODEL_FEATURES + ["target"])
        predict = df[df["date"] == predict_date].dropna(subset=MODEL_FEATURES)
        if len(train) < 200 or predict.empty:
            continue

        model = RandomForestRegressor(
            n_estimators=80,
            max_depth=5,
            min_samples_leaf=10,
            random_state=17,
            n_jobs=-1,
        )
        model.fit(train[MODEL_FEATURES], train["target"])
        df.loc[predict.index, "ml_score"] = model.predict(predict[MODEL_FEATURES])

    df["research_score"] = df["ml_score"].fillna(df["factor_score"])
    return df
