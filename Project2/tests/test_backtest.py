from quantlab.backtest.engine import BacktestConfig, EventDrivenBacktester
from quantlab.data.synthetic import SyntheticMarketConfig, generate_market_data
from quantlab.research.factors import add_factor_columns, build_composite_signal


def test_backtest_outputs_equity_and_metrics() -> None:
    data = generate_market_data(
        SyntheticMarketConfig(symbols=["AAA", "BBB", "CCC", "DDD", "EEE"], periods=120, seed=11)
    )
    factors = build_composite_signal(add_factor_columns(data))
    factors["research_score"] = factors["factor_score"]

    result = EventDrivenBacktester(
        BacktestConfig(rebalance_frequency=10, top_k=2, max_weight_per_asset=0.5)
    ).run(factors)

    assert not result.equity_curve.empty
    assert not result.trades.empty
    assert "sharpe" in result.metrics
