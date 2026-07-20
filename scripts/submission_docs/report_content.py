TRACEABILITY_ROWS = [
    ["R1", "Java OOP + MVC + CSV", "src/model, repository, service, controller, view", "Đạt"],
    ["R2", "Tổng dữ liệu CSV ≥ 10.000", "data/*.csv; kiểm tra sau sinh dữ liệu", "Đạt"],
    ["R3", "Flash Sale 1–2 giờ; giảm 30–70%", "FlashSaleEventService, FlashItemValidator", "Đạt"],
    ["R4", "soldQty ≤ limited; tối đa 2 SP", "OrderPlacementService trong vùng tới hạn", "Đạt"],
    ["R5", "FIFO + ưu tiên VIP/PREMIUM", "OrderRequestQueue; FIFO theo sequence", "Đạt"],
    ["R6", "4 cơ chế đồng bộ", "NO_LOCK, NIO FileLock, synchronized, optimistic", "Đạt"],
    ["R7", "CountDownLatch và TPS", "SimulationExecutor; success / elapsedSeconds", "Đạt"],
    ["R8", "1000 × 4 × 3 lần", "CLI --benchmark-report và CSV raw", "Đạt"],
    ["R9", "Biểu đồ TPS, âm kho, retry", "docs/charts + báo cáo", "Đạt"],
    ["R10", "CRUD/search/read 10k < 1 giây", "CsvRepository + ProductController + tests", "Đạt"],
    ["R11", "Main/FlashSale/Order/Report/Simulator", "src/view và controllers tương ứng", "Đạt"],
    ["R12", "Ngoại lệ nghiệp vụ ≥ 5", "13 lớp exception chuyên biệt", "Đạt"],
    ["R13", "Class/use case/4 flowchart", "docs/*.png và docs/flowcharts", "Đạt"],
    ["R14", "README, Word, slide, ZIP", "README.md, docs/report.docx, docs/slide.pptx", "Đạt"],
]


ARCHITECTURE_ROWS = [
    ["View", "Menu theo vai trò, validation nhập liệu, loading thông qua trạng thái console", "Không truy cập repository"],
    ["Controller", "Điều phối use case và chuyển lỗi thành thông báo phù hợp", "Không chứa persistence"],
    ["Service", "Ràng buộc thời gian/trạng thái, FIFO, voucher, lock, simulator", "Atomic business flow"],
    ["Repository", "CRUD generic CSV, codec quote, khóa tiến trình/tệp, optimistic version", "UTF-8 + atomic replace"],
    ["Model", "Entity, enum, version và invariant dữ liệu", "OOP/encapsulation"],
]


DATA_ROWS = [
    ["products.csv", "5.000", "Product + version"],
    ["customers.csv", "2.000", "Customer, tier, Argon2id, wallet"],
    ["flash_events.csv", "10", "Khung giờ 1–2 giờ"],
    ["flash_items.csv", "500", "Giảm 30–70%, stock/version"],
    ["orders.csv", "2.500", "Order gắn customer + event"],
    ["order_details.csv", "2.500", "Chi tiết số lượng/giá"],
    ["vouchers.csv", "10", "Điều kiện và lượt dùng"],
    ["transactions.csv", "Tăng theo chạy", "Audit kết quả giao dịch"],
]


LOCK_ROWS = [
    ["NO_LOCK", "Cố ý stale read/write", "Minh họa race/âm kho", "Baseline không an toàn"],
    ["NIO FileLock", "Khóa sidecar cấp hệ điều hành", "Nhiều process", "Có chi phí I/O/OS lock"],
    ["synchronized", "Monitor trong JVM", "Một process, code đơn giản", "Không bảo vệ process khác"],
    ["Optimistic", "So version, tối đa 3 retry", "Xung đột thấp", "Suy giảm mạnh khi tranh chấp cao"],
]


SECURITY_ROWS = [
    ["Mật khẩu khách hàng", "Argon2id; legacy plaintext chỉ migrate sau đăng nhập đúng"],
    ["Admin", "Username/hash qua biến môi trường; công cụ hash nhập tương tác"],
    ["CSV", "Codec quote/escape; ghi temp rồi atomic move; UTF-8"],
    ["Input", "Parse hữu hạn; chặn âm/NaN/Infinity; trạng thái fail-fast"],
    ["Phân quyền", "Guest/Customer/Admin/Researcher có menu và hành động riêng"],
]


ROLE_ROWS = [
    ["Guest", "Xem sản phẩm/sự kiện, đăng ký, đăng nhập", "Không đặt/hủy đơn"],
    ["Customer", "Mua Flash Sale, voucher, lịch sử, hủy đơn hợp lệ", "Không CRUD master data"],
    ["Admin", "CRUD product/event/item/customer/voucher; duyệt/hủy đơn; báo cáo", "Bắt buộc xác thực env"],
    ["Researcher", "Quick/single/full benchmark, export báo cáo", "Không can thiệp đơn thật"],
]
