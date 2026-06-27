from quantlab.data.synthetic import SyntheticMarketConfig, generate_market_data
from quantlab.research.factors import add_factor_columns, build_composite_signal, factor_ic_table


def test_factor_pipeline_produces_scores() -> None:
    data = generate_market_data(
        SyntheticMarketConfig(symbols=["AAA", "BBB", "CCC", "DDD"], periods=80, seed=7)
    )
    factors = build_composite_signal(add_factor_columns(data))

    assert "factor_score" in factors.columns
    assert factors["factor_score"].notna().sum() > 0


def test_factor_ic_table_contains_core_factors() -> None:
    data = generate_market_data(
        SyntheticMarketConfig(symbols=["AAA", "BBB", "CCC", "DDD"], periods=100, seed=9)
    )
    factors = build_composite_signal(add_factor_columns(data))
    table = factor_ic_table(factors)

    assert {"factor", "mean_rank_ic", "observations"}.issubset(table.columns)
    assert len(table) >= 6
