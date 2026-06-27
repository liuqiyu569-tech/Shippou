from __future__ import annotations

from dataclasses import dataclass

import pandas as pd

from quantlab.backtest.metrics import performance_metrics


@dataclass(frozen=True)
class BacktestConfig:
    start_cash: float = 1_000_000
    rebalance_frequency: int = 20
    top_k: int = 5
    commission_rate: float = 0.0003
    slippage_rate: float = 0.0005
    max_weight_per_asset: float = 0.25
    score_column: str = "research_score"


@dataclass
class BacktestResult:
    equity_curve: pd.DataFrame
    positions: pd.DataFrame
    trades: pd.DataFrame
    metrics: dict[str, float]


class EventDrivenBacktester:
    def __init__(self, config: BacktestConfig):
        self.config = config

    def run(self, data: pd.DataFrame) -> BacktestResult:
        market = data.dropna(subset=[self.config.score_column]).copy()
        market["date"] = pd.to_datetime(market["date"])
        market = market.sort_values(["date", "symbol"])
        dates = list(market["date"].drop_duplicates())
        if not dates:
            raise ValueError("No valid signal rows for backtesting.")

        cash = self.config.start_cash
        shares: dict[str, float] = {}
        last_weights: dict[str, float] = {}
        equity_rows = []
        position_rows = []
        trade_rows = []

        for step, date in enumerate(dates):
            today = market[market["date"] == date].set_index("symbol")
            turnover = 0.0

            if step % self.config.rebalance_frequency == 0:
                target_weights = self._select_target_weights(today)
                turnover = self._weight_turnover(last_weights, target_weights)
                cash, shares, trades = self._rebalance(date, today, cash, shares, target_weights)
                trade_rows.extend(trades)
                last_weights = target_weights

            equity = cash + sum(
                quantity * today.loc[symbol, "close"]
                for symbol, quantity in shares.items()
                if symbol in today.index
            )
            equity_rows.append(
                {
                    "date": date,
                    "equity": equity,
                    "cash": cash,
                    "turnover": turnover,
                    "gross_exposure": self._gross_exposure(today, shares, equity),
                }
            )

            for symbol, quantity in shares.items():
                if symbol in today.index and abs(quantity) > 1e-9:
                    position_rows.append(
                        {
                            "date": date,
                            "symbol": symbol,
                            "shares": quantity,
                            "market_value": quantity * today.loc[symbol, "close"],
                            "weight": quantity * today.loc[symbol, "close"] / equity if equity else 0,
                        }
                    )

        equity_curve = pd.DataFrame(equity_rows)
        positions = pd.DataFrame(position_rows)
        trades = pd.DataFrame(trade_rows)
        return BacktestResult(
            equity_curve=equity_curve,
            positions=positions,
            trades=trades,
            metrics=performance_metrics(equity_curve),
        )

    def _select_target_weights(self, today: pd.DataFrame) -> dict[str, float]:
        ranked = today.sort_values(self.config.score_column, ascending=False).head(self.config.top_k)
        if ranked.empty:
            return {}
        weight = min(1 / len(ranked), self.config.max_weight_per_asset)
        return {symbol: weight for symbol in ranked.index}

    def _rebalance(
        self,
        date: pd.Timestamp,
        today: pd.DataFrame,
        cash: float,
        shares: dict[str, float],
        target_weights: dict[str, float],
    ) -> tuple[float, dict[str, float], list[dict[str, object]]]:
        equity = cash + sum(
            quantity * today.loc[symbol, "close"]
            for symbol, quantity in shares.items()
            if symbol in today.index
        )
        target_symbols = set(target_weights)
        all_symbols = set(shares) | target_symbols
        trades = []

        for symbol in sorted(all_symbols):
            if symbol not in today.index:
                continue
            close = today.loc[symbol, "close"]
            target_value = equity * target_weights.get(symbol, 0.0)
            current_value = shares.get(symbol, 0.0) * close
            diff_value = target_value - current_value
            if abs(diff_value) < 1:
                continue
            side = "BUY" if diff_value > 0 else "SELL"
            fill_price = close * (
                1 + self.config.slippage_rate if side == "BUY" else 1 - self.config.slippage_rate
            )
            quantity = diff_value / fill_price
            commission = abs(quantity * fill_price) * self.config.commission_rate
            cash -= quantity * fill_price + commission
            shares[symbol] = shares.get(symbol, 0.0) + quantity
            if abs(shares[symbol]) < 1e-9:
                shares.pop(symbol, None)
            trades.append(
                {
                    "date": date,
                    "symbol": symbol,
                    "side": side,
                    "shares": quantity,
                    "price": fill_price,
                    "commission": commission,
                    "notional": quantity * fill_price,
                }
            )

        return cash, shares, trades

    @staticmethod
    def _weight_turnover(old: dict[str, float], new: dict[str, float]) -> float:
        symbols = set(old) | set(new)
        return 0.5 * sum(abs(new.get(symbol, 0.0) - old.get(symbol, 0.0)) for symbol in symbols)

    @staticmethod
    def _gross_exposure(today: pd.DataFrame, shares: dict[str, float], equity: float) -> float:
        if not equity:
            return 0.0
        exposure = sum(
            abs(quantity * today.loc[symbol, "close"])
            for symbol, quantity in shares.items()
            if symbol in today.index
        )
        return exposure / equity
