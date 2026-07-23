from __future__ import annotations

import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DOCS_LIBRARY = Path(__file__).resolve().parent / "submission_docs"
sys.path.insert(0, str(DOCS_LIBRARY))

from build_report import build_report  # noqa: E402
from build_slides import build_slides  # noqa: E402


def main() -> None:
    report_path = PROJECT_ROOT / "docs" / "report.docx"
    slides_path = PROJECT_ROOT / "docs" / "slide.pptx"
    build_report(PROJECT_ROOT, report_path)
    build_slides(PROJECT_ROOT, slides_path)
    print(f"Created: {report_path.relative_to(PROJECT_ROOT)}")
    print(f"Created: {slides_path.relative_to(PROJECT_ROOT)}")


if __name__ == "__main__":
    main()
