from __future__ import annotations

import argparse
import math
from pathlib import Path

import fitz
from PIL import Image, ImageDraw


def render_contact_sheets(pdf_path: Path, output_dir: Path, columns: int, rows: int) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    document = fitz.open(pdf_path)
    rendered: list[Image.Image] = []
    for page_number, page in enumerate(document, start=1):
        pixmap = page.get_pixmap(matrix=fitz.Matrix(1.5, 1.5), alpha=False)
        image = Image.frombytes("RGB", (pixmap.width, pixmap.height), pixmap.samples)
        rendered.append(_label(image, f"Trang {page_number}"))

    per_sheet = columns * rows
    cell_width = max(image.width for image in rendered)
    cell_height = max(image.height for image in rendered)
    for sheet_index in range(math.ceil(len(rendered) / per_sheet)):
        sheet = Image.new("RGB", (cell_width * columns, cell_height * rows), "#D8DDE5")
        for local_index, image in enumerate(rendered[sheet_index * per_sheet : (sheet_index + 1) * per_sheet]):
            x = (local_index % columns) * cell_width
            y = (local_index // columns) * cell_height
            sheet.paste(image, (x, y))
        sheet.save(output_dir / f"sheet_{sheet_index + 1:02d}.png", quality=95)


def _label(image: Image.Image, label: str) -> Image.Image:
    result = Image.new("RGB", (image.width, image.height + 42), "white")
    result.paste(image, (0, 42))
    draw = ImageDraw.Draw(result)
    draw.rectangle((0, 0, result.width, 41), fill="#17324D")
    draw.text((14, 10), label, fill="white")
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description="Render every PDF page into labeled contact sheets.")
    parser.add_argument("pdf", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--columns", type=int, default=2)
    parser.add_argument("--rows", type=int, default=1)
    args = parser.parse_args()
    render_contact_sheets(args.pdf, args.output, args.columns, args.rows)


if __name__ == "__main__":
    main()
