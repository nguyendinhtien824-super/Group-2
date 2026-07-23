from __future__ import annotations

import csv
import zipfile
from datetime import datetime
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TEXT_SUFFIXES = {".java", ".md", ".csv", ".xml", ".bat", ".ps1", ".py", ".mmd", ".example", ".gitignore"}
SKIP_DIRECTORIES = {"target", "scratch", "bin", "bin-test", "lib", "test_data", "__pycache__"}
SKIP_FILES = {"phan_cong_thuyet_trinh.md", "AGENT_LESSONS.md", "sources.txt"}
EXPECTED_HEADERS = {
    "products.csv": "productId,name,brand,category,price,stock,description,version",
    "customers.csv": "customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance",
    "vouchers.csv": "voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses",
    "flash_events.csv": "eventId,name,startTime,endTime,status,unlockTime",
    "flash_items.csv": "itemId,productId,eventId,productName,originalPrice,salePrice,initialStock,soldQty,remainingStock,version",
    "orders.csv": "orderId,customerId,customerName,orderDate,totalAmount,status,eventId",
    "order_details.csv": "detailId,orderId,productId,quantity,unitPrice,subtotal",
    "transactions.csv": "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp",
}


def main() -> None:
    row_counts = _validate_domain_csv()
    _validate_benchmarks()
    _validate_documents()
    _validate_text_files()
    _validate_java_file_sizes()
    print(f"Domain CSV rows: {sum(row_counts.values())}")
    print(f"CSV detail: {row_counts}")
    print("Benchmark: 24/24 raw rows valid")
    print("Documents, UTF-8, portability and Java file limits: PASS")


def _validate_domain_csv() -> dict[str, int]:
    rows_by_file: dict[str, list[list[str]]] = {}
    for file_name, expected_header in EXPECTED_HEADERS.items():
        path = PROJECT_ROOT / "data" / file_name
        with path.open("r", encoding="utf-8", newline="") as stream:
            reader = csv.reader(stream)
            header = next(reader)
            if ",".join(header) != expected_header:
                raise RuntimeError(f"Sai header {file_name}: {header}")
            rows_by_file[file_name] = list(reader)

    if len(rows_by_file["products.csv"]) != 5000 or len(rows_by_file["customers.csv"]) != 2000:
        raise RuntimeError("Product/Customer seed count không đúng 5000/2000")
    if len(rows_by_file["flash_events.csv"]) != 10 or len(rows_by_file["flash_items.csv"]) != 500:
        raise RuntimeError("Event/FlashItem seed count không đúng 10/500")
    if len(rows_by_file["orders.csv"]) != 2500 or len(rows_by_file["order_details.csv"]) != 2500:
        raise RuntimeError("Order/OrderDetail seed count không đúng 2500/2500")

    for row in rows_by_file["customers.csv"]:
        if len(row) != 10 or not row[8].startswith("$argon2id$"):
            raise RuntimeError(f"Customer CSV/Argon2 round-trip lỗi tại {row[:2]}")
        float(row[9])

    for row in rows_by_file["flash_events.csv"]:
        duration_hours = (datetime.fromisoformat(row[3]) - datetime.fromisoformat(row[2])).total_seconds() / 3600
        if not 1.0 <= duration_hours <= 2.0:
            raise RuntimeError(f"Event duration ngoài 1–2 giờ: {row[0]}")

    for row in rows_by_file["flash_items.csv"]:
        original = float(row[4])
        sale = float(row[5])
        initial = int(row[6])
        sold = int(row[7])
        remaining = int(row[8])
        discount = (original - sale) * 100.0 / original
        if not 30.0 <= discount <= 70.0:
            raise RuntimeError(f"Discount ngoài 30–70%: {row[0]}")
        if sold > initial or remaining != initial - sold:
            raise RuntimeError(f"Inventory invariant lỗi: {row[0]}")

    if any(len(row) != 7 or not row[6] for row in rows_by_file["orders.csv"]):
        raise RuntimeError("orders.csv phải có 7 cột và eventId")
    if sum(len(rows) for rows in rows_by_file.values()) < 10000:
        raise RuntimeError("Tổng dữ liệu nghiệp vụ dưới 10.000 dòng")
    return {name: len(rows) for name, rows in rows_by_file.items()}


def _validate_benchmarks() -> None:
    locks = {"NO_LOCK", "FILE_LOCK", "SYNCHRONIZED", "OPTIMISTIC_LOCK"}
    for stock in (100, 2000):
        path = PROJECT_ROOT / "data" / f"simulation_stock_{stock}.csv"
        with path.open("r", encoding="utf-8", newline="") as stream:
            rows = list(csv.DictReader(stream))
        keys = {(int(row["runNumber"]), row["lockType"]) for row in rows}
        expected = {(run, lock_type) for run in range(1, 4) for lock_type in locks}
        if len(rows) != 12 or keys != expected:
            raise RuntimeError(f"Benchmark stock={stock} không đủ 3×4 raw rows")
        if any(int(row["initialStock"]) != stock or int(row["totalThreads"]) != 1000 for row in rows):
            raise RuntimeError(f"Benchmark stock={stock} sai workload")


def _validate_documents() -> None:
    required = [
        "docs/report.docx", "docs/slide.pptx", "docs/class_diagram.png", "docs/use_case_diagram.png",
        "docs/flowcharts/order_flow.png", "docs/flowcharts/race_condition_flow.png",
        "docs/flowcharts/simulator_flow.png", "docs/flowcharts/data_generator_flow.png",
    ]
    for relative in required:
        path = PROJECT_ROOT / relative
        if not path.is_file() or path.stat().st_size == 0:
            raise RuntimeError(f"Thiếu hoặc rỗng: {relative}")
    for relative in ("docs/report.docx", "docs/slide.pptx"):
        with zipfile.ZipFile(PROJECT_ROOT / relative) as archive:
            corrupted = archive.testzip()
            if corrupted:
                raise RuntimeError(f"Office document lỗi ZIP part: {relative}:{corrupted}")


def _validate_text_files() -> None:
    for path in PROJECT_ROOT.rglob("*"):
        if not path.is_file() or path.name in SKIP_FILES or any(part in SKIP_DIRECTORIES for part in path.relative_to(PROJECT_ROOT).parts):
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES and path.name not in {"README.md", ".gitignore"}:
            continue
        raw = path.read_bytes()
        if raw.startswith(b"\xef\xbb\xbf"):
            raise RuntimeError(f"UTF-8 BOM không được phép: {path.relative_to(PROJECT_ROOT)}")
        text = raw.decode("utf-8")
        if "\ufffd" in text:
            raise RuntimeError(f"Replacement character trong: {path.relative_to(PROJECT_ROOT)}")
        local_windows_path = "C:" + "\\Users\\"
        file_uri = "file:" + "///"
        if local_windows_path in text or file_uri in text:
            raise RuntimeError(f"Đường dẫn máy cá nhân trong file đóng gói: {path.relative_to(PROJECT_ROOT)}")


def _validate_java_file_sizes() -> None:
    limits = {"view": 250, "service": 300}
    for directory_name, limit in limits.items():
        for path in (PROJECT_ROOT / "src" / directory_name).glob("*.java"):
            line_count = len(path.read_text(encoding="utf-8").splitlines())
            if line_count > limit:
                raise RuntimeError(f"{path.relative_to(PROJECT_ROOT)} có {line_count} dòng, vượt {limit}")


if __name__ == "__main__":
    main()
