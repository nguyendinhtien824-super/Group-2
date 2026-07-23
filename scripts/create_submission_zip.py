from __future__ import annotations

import zipfile
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
ARCHIVE_NAME = "NHOM_01_LAB211_FlashSale"
OUTPUT_PATH = PROJECT_ROOT.parent / f"{ARCHIVE_NAME}.zip"
INCLUDED_DIRECTORIES = ("src", "test", "data", "docs", "scripts")
INCLUDED_ROOT_FILES = (
    ".env.example",
    ".gitignore",
    "README.md",
    "pom.xml",
    "run_app_tuan65.bat",
    "run_junit.bat",
    "run_tests_tuan65.bat",
    "run_tuan4.bat",
)
EXCLUDED_PARTS = {"__pycache__", "scratch", "target", "bin", "bin-test", "lib", "test_data"}
EXCLUDED_SUFFIXES = {".pyc", ".class", ".tmp", ".lock"}
REQUIRED_FILES = (
    "README.md",
    "pom.xml",
    "docs/report.docx",
    "docs/slide.pptx",
    "docs/class_diagram.png",
    "docs/use_case_diagram.png",
    "docs/flowcharts/order_flow.png",
    "docs/flowcharts/race_condition_flow.png",
    "docs/flowcharts/simulator_flow.png",
    "docs/flowcharts/data_generator_flow.png",
    "data/simulation_stock_100.csv",
    "data/simulation_stock_2000.csv",
)


def main() -> None:
    missing = [path for path in REQUIRED_FILES if not (PROJECT_ROOT / path).is_file()]
    if missing:
        raise RuntimeError(f"Không thể đóng gói; thiếu deliverable: {missing}")

    files = _collect_files()
    if OUTPUT_PATH.exists():
        OUTPUT_PATH.unlink()
    with zipfile.ZipFile(OUTPUT_PATH, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for source in files:
            relative = source.relative_to(PROJECT_ROOT)
            archive.write(source, Path(ARCHIVE_NAME) / relative)

    print(f"Created: {OUTPUT_PATH}")
    print(f"Files: {len(files)}")
    print(f"Size: {OUTPUT_PATH.stat().st_size} bytes")


def _collect_files() -> list[Path]:
    files: list[Path] = []
    for file_name in INCLUDED_ROOT_FILES:
        candidate = PROJECT_ROOT / file_name
        if candidate.is_file():
            files.append(candidate)
    for directory_name in INCLUDED_DIRECTORIES:
        directory = PROJECT_ROOT / directory_name
        for candidate in directory.rglob("*"):
            if not candidate.is_file():
                continue
            relative = candidate.relative_to(PROJECT_ROOT)
            if any(part in EXCLUDED_PARTS for part in relative.parts):
                continue
            if candidate.suffix.lower() in EXCLUDED_SUFFIXES:
                continue
            files.append(candidate)
    return sorted(set(files), key=lambda path: path.as_posix().lower())


if __name__ == "__main__":
    main()
