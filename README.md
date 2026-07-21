# ⚡ LAB211 Flash Sale Console — Mô phỏng bán hàng đồng thời trên CSV

[![Java 17](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-1565C0?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JUnit 4](https://img.shields.io/badge/JUnit-4.13.2-25A162?style=for-the-badge)](https://junit.org/junit4/)
[![Storage](https://img.shields.io/badge/Storage-UTF--8_CSV-7C3AED?style=for-the-badge)](#-lưu-trữ-csv)

Ứng dụng console Java mô phỏng một hệ thống Flash Sale kiểu Shopee với ba khu vực nghiệp vụ: Khách hàng, Quản trị viên và Nghiên cứu viên. Dự án dùng kiến trúc MVC nhiều tầng, lưu trữ CSV thật, kiểm soát vòng đời đơn hàng và so sánh bốn cơ chế xử lý race condition. Bộ sinh dữ liệu tạo hơn 12.500 bản ghi có khóa ngoại hợp lệ để phục vụ demo, kiểm thử và benchmark.

## Mục lục

- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Sơ đồ kỹ thuật](#-sơ-đồ-kỹ-thuật)
- [Luồng đặt hàng và ràng buộc](#-luồng-đặt-hàng-và-ràng-buộc)
- [Mô phỏng đồng thời](#-mô-phỏng-đồng-thời)
- [Lưu trữ CSV](#-lưu-trữ-csv)
- [Cài đặt và chạy](#-cài-đặt-và-chạy)
- [Cấu hình Admin](#-cấu-hình-admin)
- [Kiểm thử](#-kiểm-thử)
- [Báo cáo](#-báo-cáo)

## ✨ Tính năng chính

### Khách hàng

- Đăng ký, đăng nhập, đăng xuất; tài khoản `BANNED` bị chặn khỏi phiên hoạt động.
- Xem sự kiện đang hoạt động và các mặt hàng Flash Sale.
- Đặt từ 1 đến 2 sản phẩm cho mỗi cặp khách hàng - sản phẩm - sự kiện.
- Xếp hàng ưu tiên các hạng trên `STANDARD`; các request cùng nhóm giữ thứ tự FIFO.
- Tự động áp dụng giảm giá theo `CustTier`; voucher là tùy chọn.
- Quản lý hồ sơ, đổi mật khẩu, nạp ví và xem voucher khả dụng.
- Theo dõi đơn hàng, chi tiết đơn và lịch sử giao dịch.

### Quản trị viên

- Xác thực bằng tài khoản lấy từ biến môi trường; khóa đăng nhập sau ba lần sai trong một phiên.
- CRUD Product và tìm theo danh mục/khoảng giá.
- CRUD Flash Sale Event; hỗ trợ `start`, `end`, `lock`, `unlock`.
- CRUD Flash Sale Item với kiểm tra giá, tồn kho và version.
- CRUD Customer, tìm kiếm, `ban`/`unban`; khóa hoặc xóa tài khoản sẽ xử lý các đơn còn hiệu lực.
- CRUD Voucher; duyệt, hủy, hoàn tất đơn hàng.
- Xem báo cáo doanh thu, trạng thái đơn, hạng khách hàng, voucher và giao dịch.

### Nghiên cứu viên

- Cấu hình quick test từ 100 đến 500 luồng, tồn kho và tỷ lệ hạng thành viên.
- Chạy riêng một cơ chế hoặc benchmark cả bốn cơ chế khóa.
- Chạy ma trận bắt buộc `1000 luồng × 4 cơ chế × 3 lần`.
- Thu thập success, fail, successful quantity, retry, elapsed time, TPS, tồn kho cuối và tính nhất quán.
- Xuất kết quả sang CSV và Markdown.

### Sinh dữ liệu

- Sinh 12.520 bản ghi nghiệp vụ, chưa tính lịch sử trong `transactions.csv`.
- Ghi UTF-8 qua tệp tạm và atomic move để tránh file nửa chừng.
- Giữ nguyên `transactions.csv` nếu file đã tồn tại.
- Menu tương tác yêu cầu nhập chính xác `TAO` trước khi thay bộ CSV nghiệp vụ.

## 🗺️ Kiến trúc hệ thống

```text
tuan 6 5/
├── src/
│   ├── app/            # Composition root và entrypoint đóng gói
│   ├── config/         # Format thời gian và cấu hình miền nghiệp vụ
│   ├── security/       # Argon2id, policy mật khẩu, credential Admin
│   ├── model/          # Entity, kết quả mô phỏng và enums
│   ├── repository/     # Generic CsvRepository và repository chuyên biệt
│   ├── service/        # Nghiệp vụ đặt hàng, vòng đời, simulator, báo cáo
│   ├── controller/     # API điều phối giữa View và Service
│   └── view/           # Console UI theo Customer/Admin/Researcher
├── test/test/          # JUnit 4 unit, integration và concurrency tests
├── data/               # CSV và báo cáo runtime
├── docs/
│   └── diagrams/       # Nguồn Mermaid được đồng bộ với code hiện tại
├── pom.xml
├── .env.example          # Mẫu tên biến môi trường, không tự được nạp
├── run_app_tuan65.bat
└── run_tests_tuan65.bat
```

Luồng phụ thuộc chính:

```text
View → Controller → Service → Repository → CSV
                         ↓
                  Domain Model/Policy
```

`app.FlashSaleApplication` là composition root duy nhất của bản JAR. Lớp này khởi tạo repository, service, controller và ba router giao diện rồi mới gọi `MainView.display()`.

## 🖼️ Sơ đồ kỹ thuật

| Sơ đồ | Nguồn Mermaid | Nội dung |
|---|---|---|
| Class Diagram | [class-diagram.mmd](docs/diagrams/class-diagram.mmd) | Visibility, fields, methods, kế thừa, dependency và multiplicity |
| Use Case | [use-case-diagram.mmd](docs/diagrams/use-case-diagram.mmd) | Guest, Customer, Admin và Researcher |
| Order Flow | [order-flow.mmd](docs/diagrams/order-flow.mmd) | Validate, critical flow, rollback và persist |
| Race Condition Flow | [race-condition-flow.mmd](docs/diagrams/race-condition-flow.mmd) | So sánh hành vi của bốn lock |
| Simulator Flow | [simulator-flow.mmd](docs/diagrams/simulator-flow.mmd) | Executor, latch, metrics, transactions và report |
| Data Generator Flow | [data-generator-flow.mmd](docs/diagrams/data-generator-flow.mmd) | Số lượng seed, quan hệ khóa ngoại và atomic write |

Các file `.mmd` là nguồn chuẩn mới. Nền tảng hỗ trợ Mermaid có thể mở trực tiếp hoặc render bằng Mermaid CLI.

## 🛒 Luồng đặt hàng và ràng buộc

Luồng đặt hàng thật đi qua `OrderController → FlashSaleServiceImpl → OrderPlacementService`. `OrderRequestQueue` cấp một permit bao trọn critical flow; bên trong permit, toàn bộ bước đọc lịch sử, kiểm tra hạn mức, trừ kho và tạo đơn tiếp tục được serialize theo `OrderRepository`.

| Ràng buộc | Quy tắc đang áp dụng |
|---|---|
| Số lượng | Từ 1 đến 2; cộng dồn theo Customer + Product + Event |
| Hàng đợi | Mọi hạng trên `STANDARD` thuộc nhóm VIP/Premium; FIFO trong cùng nhóm |
| Tính công bằng | Request đang active không bị preempt; VIP chỉ được chọn trước các request Standard đang chờ |
| Trạng thái Customer | Phải tồn tại và không `BANNED` |
| Trạng thái Event | Phải `ACTIVE`, không `LOCKED`, chưa hết hạn và đúng khung giờ |
| Thời lượng Event | Từ 1 đến 2 giờ |
| Mức giảm Flash Sale | Từ 30% đến 70% |
| Tồn kho | Đặt hàng thật dùng optimistic version check, retry tối đa 3 |
| Ví | Phải đủ tiền sau ưu đãi hạng và voucher |
| Voucher | Kiểm tra loại, giá trị đơn tối thiểu và lượt dùng còn lại |
| Vòng đời đơn | `PENDING → APPROVED → SUCCESS`; chỉ `PENDING/APPROVED` được hủy |
| Rollback | Lỗi ghi đơn sẽ hoàn ví, tồn kho và lượt voucher đã giữ |

Ưu tiên chỉ quyết định request nào được xử lý kế tiếp. Sau khi nhận permit, VIP/Premium vẫn phải re-check tài khoản, sự kiện, hạn mức mua, ví, voucher, version và tồn kho như request Standard.

Việc hủy đơn hiện được điều phối qua khu vực Admin. Khi hủy hợp lệ, hệ thống hoàn ví, kho và voucher rồi ghi thêm một `OrderTransaction`.

## 🧵 Mô phỏng đồng thời

Simulator cố ý đi thẳng vào `FlashItemRepository` thay vì luồng đặt hàng đã serialize, nhờ đó bốn cơ chế tạo ra kết quả so sánh thật trên CSV.

| Cơ chế | Cách hoạt động | Kỳ vọng |
|---|---|---|
| `NO_LOCK` | Stale read-modify-write; chỉ bảo vệ từng thao tác I/O để CSV không hỏng vật lý | Baseline, có lost update/oversell logic |
| `FILE_LOCK` | `FileChannel` + `FileLock` trên sidecar `.lck`, kết hợp khóa theo path | Không âm kho |
| `SYNCHRONIZED` | Monitor tại repository và khóa ghi theo path | Không âm kho |
| `OPTIMISTIC_LOCK` | So expected version, tăng version khi ghi, retry tối đa 3 | Không âm kho; có thống kê conflict/retry |

Mỗi lượt chạy sử dụng:

1. `ExecutorService` với đúng số worker yêu cầu.
2. `readyLatch`, `startLatch` và `doneLatch` để mọi worker bắt đầu cùng lúc.
3. Quantity ngẫu nhiên từ 1 đến 2 tùy hạng thành viên.
4. Một `OrderTransaction` cho mỗi worker, kể cả request thất bại.
5. Công thức `TPS = successCount / elapsedSeconds`.
6. Kiểm tra `negativeStock` và `dataConsistent`; ba cơ chế bảo vệ phải giữ kho không âm.

Hai chế độ chuẩn:

- Quick benchmark: `100–500 threads × 4 LockType`.
- Required benchmark: `1000 threads × 4 LockType × 3 lần`.

## 💾 Lưu trữ CSV

| File | Dữ liệu | Số dòng seed |
|---|---|---:|
| `products.csv` | Product, gồm trường `version` | 5.000 |
| `customers.csv` | Customer, Argon2id password hash và wallet | 2.000 |
| `vouchers.csv` | Voucher phần trăm/cố định | 10 |
| `flash_events.csv` | Event, status và unlockTime | 10 |
| `flash_items.csv` | Product/Event FK, soldQty, remainingStock và version | 500 |
| `orders.csv` | Customer/Event FK và OrderStatus | 2.500 |
| `order_details.csv` | Order/Product FK, quantity và subtotal | 2.500 |
| `transactions.csv` | Audit đặt hàng, lifecycle và simulator | Giữ lịch sử |

`CsvRepository<T extends BaseEntity>` chịu trách nhiệm parse/serialize bằng reflection, khóa theo đường dẫn tuyệt đối đã normalize và thay file nguyên tử. Các repository chuyên biệt bổ sung validation, tìm kiếm và hành vi đồng thời.

Xem header, kiểu cột và quan hệ khóa ngoại tại [CSV Schema Documentation](docs/csv_schema.md).

## 🚀 Cài đặt và chạy

### Yêu cầu hệ thống

- JDK 17 trở lên.
- Apache Maven 3.8 trở lên.
- Terminal hỗ trợ UTF-8.

Dự án không yêu cầu database server hoặc `.env`. Sau khi clone hoặc giải nén gói nộp, mở terminal tại thư mục chứa `pom.xml`.

### Windows PowerShell/CMD

Chạy toàn bộ test:

```powershell
.\run_tests_tuan65.bat
```

Build, verify và chạy ứng dụng:

```powershell
.\run_app_tuan65.bat
```

Sau khi build, có thể gọi hai tiện ích CLI mà không mở menu tương tác:

```powershell
java -jar target\flash-sale-simulator.jar --generate-data
java -jar target\flash-sale-simulator.jar --admin-hash
java -jar target\flash-sale-simulator.jar --benchmark-report
```

`--generate-data` chạy trực tiếp, không hỏi lại chuỗi `TAO`; lệnh này thay các file seed nghiệp vụ nhưng vẫn giữ `transactions.csv` đã có.

`--benchmark-report` (alias `--benchmark`) chạy hai kịch bản tồn kho `100` và `2000`, mỗi kịch bản `1000 requests × 4 cơ chế × 3 lần`, rồi xuất CSV raw và Markdown tổng hợp dưới `data/`.

### Linux/macOS hoặc terminal đa nền tảng

```bash
mvn -B clean verify
java -Dfile.encoding=UTF-8 -jar target/flash-sale-simulator.jar

# Tiện ích không tương tác với menu chính
java -jar target/flash-sale-simulator.jar --generate-data
java -jar target/flash-sale-simulator.jar --admin-hash
java -jar target/flash-sale-simulator.jar --benchmark-report
```

Entrypoint đóng gói là `app.FlashSaleApplication`. `src/Main.java` chỉ là compatibility wrapper chuyển tiếp vào entrypoint này; sinh dữ liệu chuẩn qua tùy chọn `--generate-data`.

## 🔐 Cấu hình Admin

Nếu thiếu cấu hình, khu vực Admin được vô hiệu hóa an toàn. Không hardcode tài khoản hoặc mật khẩu vào mã nguồn.

### 1. Tạo Argon2id hash

Sau khi build JAR, chạy công cụ nhập mật khẩu tương tác. Mật khẩu không được truyền qua command-line argument:

```powershell
java -jar target/flash-sale-simulator.jar --admin-hash
```

Sao chép chuỗi PHC bắt đầu bằng `$argon2id$`.

### 2. Đặt biến môi trường cho phiên terminal

PowerShell:

```powershell
$env:SHOPEE_ADMIN_USERNAME = "admin"
$env:SHOPEE_ADMIN_PASSWORD_HASH = '$argon2id$...'
.\run_app_tuan65.bat
```

Linux/macOS:

```bash
export SHOPEE_ADMIN_USERNAME='admin'
export SHOPEE_ADMIN_PASSWORD_HASH='$argon2id$...'
mvn -B clean verify
java -Dfile.encoding=UTF-8 -jar target/flash-sale-simulator.jar
```

File `.env.example` chỉ liệt kê đúng tên biến; ứng dụng đọc process environment và không tự nạp file này. Không commit hash dùng cho môi trường thật, mật khẩu thô, `.env` hoặc credential vào repository. Tài khoản Customer do Data Generator sinh chỉ là dữ liệu seed với mật khẩu ngẫu nhiên; hãy đăng ký tài khoản mới để thử luồng đăng nhập.

## 🧪 Kiểm thử

Chạy toàn bộ suite:

```bash
mvn -B clean test
```

Chạy riêng nhóm concurrency/simulator:

```bash
mvn -Dtest=ConcurrencyLockTest,LockMechanismRepositoryTest,SimulatorServiceTest test
```

Các nhóm kiểm thử bao phủ:

- Model, CSV reflection mapping, escaping và atomic replacement.
- Product/Event/FlashItem CRUD và validation.
- Customer authentication, password security, Admin operations.
- Order business rules, trạng thái, rollback, voucher và báo cáo.
- Hàng đợi VIP/Premium, FIFO cùng priority, không preempt và re-check tồn kho.
- Bốn cơ chế concurrency, quick benchmark và ma trận `1000 × 4 × 3`.
- Data Generator, schema, số dòng, event duration và bảo toàn transaction history.

Snapshot hiện tại đã đạt `226/226` test bằng `mvn -B clean verify`.

## 📊 Báo cáo

Chạy `--benchmark-report` để tạo bằng chứng tái lập:

- `data/simulation_stock_100.csv` và `.md`: 12 raw rows của kịch bản kích hoạt oversell/race.
- `data/simulation_stock_2000.csv` và `.md`: 12 raw rows của kịch bản đủ capacity để so throughput.
- `docs/charts/`: biểu đồ TPS, âm kho/retry và mức giảm throughput ghép cặp.
- `data/transactions.csv`: audit chi tiết từng request mô phỏng và giao dịch nghiệp vụ.

Mọi đường dẫn runtime đều tương đối với thư mục dự án, không phụ thuộc máy Windows của tác giả.
