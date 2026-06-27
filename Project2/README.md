# QuantLab - 多因子量化研究与事件驱动回测平台

QuantLab 是一个面向算法、量化和后端岗位展示的研究型项目。项目覆盖行情数据清洗、因子计算、机器学习打分、事件驱动回测、风险指标评估和实验报告生成，重点展示如何把量化研究流程工程化、可复现化。

## 项目亮点

- **因子研究**：实现动量、反转、波动率、成交量异动、均线偏离、RSI 等因子，并输出 RankIC、ICIR 等有效性指标。
- **机器学习建模**：基于滚动训练窗口构建横截面收益预测模型，避免未来函数，支持特征工程与模型信号融合。
- **事件驱动回测**：显式处理调仓周期、手续费、滑点、持仓上限、现金、持仓、成交记录和净值曲线。
- **风险评估**：输出年化收益、年化波动率、Sharpe、最大回撤、Calmar、换手率等指标。
- **工程化接口**：提供命令行入口和 FastAPI 接口，支持自动生成 CSV 结果与 HTML 实验报告。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 语言 | Python 3.10+ |
| 数据处理 | pandas, numpy |
| 机器学习 | scikit-learn RandomForestRegressor |
| API | FastAPI, Uvicorn |
| 配置 | YAML |
| 报告 | Jinja2 HTML |
| 测试 | pytest |

## 快速启动

```bash
python -m venv .venv
.\.venv\Scripts\activate
pip install -e ".[dev]"
python -m quantlab.cli --config configs/sample_strategy.yml --output outputs
```

运行成功后会在 `outputs/` 下生成：

- `momentum_quality_demo_equity.csv`：净值曲线
- `momentum_quality_demo_trades.csv`：交易明细
- `momentum_quality_demo_factor_ic.csv`：因子 IC 分析
- `momentum_quality_demo_report.html`：实验报告

## 启动 API

```bash
uvicorn quantlab.api.main:app --reload --port 8000
```

接口：

- `GET /health`
- `POST /experiments/demo`

## 项目结构

```text
Project2
├── configs
│   └── sample_strategy.yml
├── docs
│   └── architecture.md
├── quantlab
│   ├── api
│   ├── backtest
│   ├── data
│   ├── reporting
│   ├── research
│   ├── cli.py
│   └── experiment.py
└── tests
```

## 简历描述建议

**QuantLab 多因子量化研究与事件驱动回测平台**

- 构建行情数据清洗与因子计算流水线，实现动量、反转、波动率、成交量异动、均线偏离、RSI 等多类因子，并通过 RankIC、ICIR 评估因子有效性。
- 设计事件驱动回测引擎，支持手续费、滑点、调仓周期、持仓上限、现金与成交记录管理，输出年化收益、Sharpe、最大回撤、Calmar、换手率等指标。
- 基于滚动训练窗口构建横截面收益预测模型，融合机器学习信号与传统因子信号，避免训练验证中的未来函数问题。
- 使用 FastAPI 提供实验运行接口，基于 YAML 管理策略参数，自动生成净值曲线、交易明细、因子 IC 和 HTML 实验报告。

## 后续可增强方向

- 接入 AKShare/Tushare 或本地真实行情 CSV。
- 增加行业/市值中性化、Barra 风格风险暴露。
- 增加 LightGBM/XGBoost 排序模型和 Walk-forward validation。
- 使用 PostgreSQL/ClickHouse 存储行情、因子和实验结果。
