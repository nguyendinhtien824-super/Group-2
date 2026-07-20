from __future__ import annotations

import csv
import statistics
from dataclasses import dataclass
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


EXPECTED_STOCKS = (100, 2000)
EXPECTED_LOCKS = ("NO_LOCK", "FILE_LOCK", "SYNCHRONIZED", "OPTIMISTIC")
LOCK_ALIASES = {"OPTIMISTIC_LOCK": "OPTIMISTIC"}
LOCK_LABELS = {
    "NO_LOCK": "No Lock",
    "FILE_LOCK": "NIO FileLock",
    "SYNCHRONIZED": "synchronized",
    "OPTIMISTIC": "Optimistic",
}
LOCK_COLORS = {
    "NO_LOCK": "#D94F4F",
    "FILE_LOCK": "#2E74B5",
    "SYNCHRONIZED": "#2A9D8F",
    "OPTIMISTIC": "#F4A261",
}


@dataclass(frozen=True)
class BenchmarkRow:
    stock: int
    run: int
    lock_type: str
    success: int
    failed: int
    final_stock: int
    negative_stock: int
    retries: int
    retry_rate: float
    tps: float
    consistent: bool
    vs_baseline: float
    target: str


@dataclass(frozen=True)
class BenchmarkSummary:
    lock_type: str
    average_tps: float
    median_tps: float
    average_drop: float
    negative_stock_total: int
    retry_total: int
    consistent_runs: int
    target_runs: int


def load_benchmarks(data_dir: Path) -> dict[int, list[BenchmarkRow]]:
    grouped: dict[int, list[BenchmarkRow]] = {stock: [] for stock in EXPECTED_STOCKS}
    candidates = sorted(data_dir.glob("simulation*.csv"))
    for csv_path in candidates:
        with csv_path.open("r", encoding="utf-8", newline="") as stream:
            reader = csv.DictReader(stream)
            if not reader.fieldnames or "initialStock" not in reader.fieldnames:
                continue
            for raw in reader:
                stock = int(raw["initialStock"])
                if stock not in grouped:
                    continue
                grouped[stock].append(_parse_row(stock, raw))

    for stock, rows in grouped.items():
        unique = {(row.run, row.lock_type): row for row in rows}
        grouped[stock] = sorted(unique.values(), key=lambda row: (row.run, EXPECTED_LOCKS.index(row.lock_type)))
        expected_keys = {(run, lock_type) for run in range(1, 4) for lock_type in EXPECTED_LOCKS}
        if set(unique) != expected_keys:
            missing = sorted(expected_keys - set(unique))
            raise RuntimeError(
                f"Benchmark stock={stock} phải có đúng 12 dòng (3 lần x 4 cơ chế); thiếu {missing}."
            )
    return grouped


def _parse_row(stock: int, raw: dict[str, str]) -> BenchmarkRow:
    raw_lock_type = raw["lockType"].split("(", 1)[0].strip()
    lock_type = LOCK_ALIASES.get(raw_lock_type, raw_lock_type)
    if lock_type not in EXPECTED_LOCKS:
        raise RuntimeError(f"Cơ chế khóa không hợp lệ trong benchmark: {lock_type}")
    return BenchmarkRow(
        stock=stock,
        run=int(raw["runNumber"]),
        lock_type=lock_type,
        success=int(raw["successCount"]),
        failed=int(raw["failedCount"]),
        final_stock=int(raw["finalStock"]),
        negative_stock=int(raw["negativeStock"]),
        retries=int(raw["retryCount"]),
        retry_rate=float(raw["retryRatePercent"]),
        tps=float(raw["tps"]),
        consistent=raw["dataConsistent"].lower() == "true",
        vs_baseline=float(raw["vsBaselinePercent"]),
        target=raw["target"],
    )


def summarize(rows: list[BenchmarkRow]) -> list[BenchmarkSummary]:
    result: list[BenchmarkSummary] = []
    for lock_type in EXPECTED_LOCKS:
        selected = [row for row in rows if row.lock_type == lock_type]
        result.append(
            BenchmarkSummary(
                lock_type=lock_type,
                average_tps=statistics.fmean(row.tps for row in selected),
                median_tps=statistics.median(row.tps for row in selected),
                average_drop=statistics.fmean(row.vs_baseline for row in selected),
                negative_stock_total=sum(row.negative_stock for row in selected),
                retry_total=sum(row.retries for row in selected),
                consistent_runs=sum(row.consistent for row in selected),
                target_runs=sum(row.target == "DAT" for row in selected),
            )
        )
    return result


def create_charts(benchmarks: dict[int, list[BenchmarkRow]], chart_dir: Path) -> dict[str, Path]:
    chart_dir.mkdir(parents=True, exist_ok=True)
    plt.rcParams.update({"font.family": "DejaVu Sans", "axes.titleweight": "bold"})
    paths: dict[str, Path] = {}
    for stock in EXPECTED_STOCKS:
        summary = summarize(benchmarks[stock])
        path = chart_dir / f"throughput_stock_{stock}.png"
        _bar_chart(
            summary,
            [item.average_tps for item in summary],
            f"TPS trung bình — tồn kho {stock}",
            "Giao dịch thành công/giây",
            path,
        )
        paths[f"throughput_{stock}"] = path

    low_stock_summary = summarize(benchmarks[100])
    safety_path = chart_dir / "safety_retry_stock_100.png"
    _safety_chart(low_stock_summary, safety_path)
    paths["safety_100"] = safety_path

    target_path = chart_dir / "paired_throughput_drop_stock_2000.png"
    _target_chart(summarize(benchmarks[2000]), target_path)
    paths["target_2000"] = target_path
    return paths


def _bar_chart(
    summary: list[BenchmarkSummary], values: list[float], title: str, ylabel: str, destination: Path
) -> None:
    labels = [LOCK_LABELS[item.lock_type] for item in summary]
    colors = [LOCK_COLORS[item.lock_type] for item in summary]
    figure, axis = plt.subplots(figsize=(10.5, 5.2), dpi=180)
    bars = axis.bar(labels, values, color=colors, width=0.62)
    axis.set_title(title, fontsize=15)
    axis.set_ylabel(ylabel)
    axis.grid(axis="y", alpha=0.25)
    axis.bar_label(bars, fmt="%.2f", padding=3, fontsize=9)
    figure.tight_layout()
    figure.savefig(destination, bbox_inches="tight", facecolor="white")
    plt.close(figure)


def _safety_chart(summary: list[BenchmarkSummary], destination: Path) -> None:
    labels = [LOCK_LABELS[item.lock_type] for item in summary]
    negatives = [item.negative_stock_total for item in summary]
    retries = [item.retry_total for item in summary]
    figure, left = plt.subplots(figsize=(10.5, 5.2), dpi=180)
    right = left.twinx()
    positions = range(len(labels))
    negative_bars = left.bar([x - 0.18 for x in positions], negatives, 0.36, color="#D94F4F", label="Tổng âm kho")
    retry_bars = right.bar([x + 0.18 for x in positions], retries, 0.36, color="#F4A261", label="Tổng retry")
    left.set_xticks(list(positions), labels)
    left.set_ylabel("Âm kho (đơn vị)")
    right.set_ylabel("Số lần retry")
    left.set_title("An toàn dữ liệu và retry — tồn kho 100", fontsize=15)
    left.grid(axis="y", alpha=0.2)
    left.bar_label(negative_bars, padding=3, fontsize=9)
    right.bar_label(retry_bars, padding=3, fontsize=9)
    figure.legend(loc="upper right", bbox_to_anchor=(0.88, 0.88))
    figure.tight_layout()
    figure.savefig(destination, bbox_inches="tight", facecolor="white")
    plt.close(figure)


def _target_chart(summary: list[BenchmarkSummary], destination: Path) -> None:
    safe = [item for item in summary if item.lock_type != "NO_LOCK"]
    labels = [LOCK_LABELS[item.lock_type] for item in safe]
    drops = [item.average_drop for item in safe]
    figure, axis = plt.subplots(figsize=(10.5, 5.2), dpi=180)
    bars = axis.bar(labels, drops, color=[LOCK_COLORS[item.lock_type] for item in safe], width=0.58)
    axis.axhline(-30.0, color="#D94F4F", linestyle="--", linewidth=2, label="Ngưỡng -30%")
    axis.set_title("Mức giảm TPS trung bình ghép cặp — tồn kho 2000", fontsize=15)
    axis.set_ylabel("So với No Lock cùng lần chạy (%)")
    axis.grid(axis="y", alpha=0.25)
    axis.bar_label(bars, fmt="%.1f%%", padding=3, fontsize=9)
    axis.legend()
    figure.tight_layout()
    figure.savefig(destination, bbox_inches="tight", facecolor="white")
    plt.close(figure)
