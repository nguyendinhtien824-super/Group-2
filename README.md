# ⚡ E-Commerce Flash Sale Simulator Console App

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Build Tool](https://img.shields.io/badge/Maven-3.8%2B-blue?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Test Frame](https://img.shields.io/badge/JUnit-5.10-green?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Architecture](https://img.shields.io/badge/Architecture-MVC-red?style=for-the-badge)](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller)
[![Locking Mechanisms](https://img.shields.io/badge/Concurrency-4%20Locking%20Mechanisms-purple?style=for-the-badge)](https://en.wikipedia.org/wiki/Concurrency_control)

Dự án **E-Commerce Flash Sale Simulator** là một ứng dụng Console Java được xây dựng theo chuẩn mô hình **MVC Architecture** doanh nghiệp. Mục tiêu cốt lõi của hệ thống là giả lập kịch bản **Flash Sale cực đoan (High-Concurrency)** với hàng trăm lượt truy cập đặt hàng đồng thời trên một tài nguyên giới hạn, nhằm nghiên cứu và giải quyết bài toán tranh chấp dữ liệu (Race Condition) và âm kho (Overselling).

Hệ thống lưu trữ dữ liệu thông qua tệp tin phẳng CSV hoạt động như một hệ cơ sở dữ liệu gọn nhẹ, tích hợp 4 cơ chế quản lý giao dịch đồng thời và các thuật toán tính toán phân hạng thành viên (VIP Tier), quản lý trạng thái người dùng (Ban/Unban) cũng như phân tích tối ưu voucher.

---

## 🗺️ Sơ đồ & Kiến trúc Hệ thống (System Architecture)

Dự án được tổ chức phân lớp chặt chẽ nhằm đảm bảo nguyên lý đơn nhiệm (Single Responsibility Principle - SRP):

```text
NHOM_01_LAB211_FlashSale/
├── src/
│   ├── app/            # Điểm khởi chạy ứng dụng (FlashSaleApplication)
│   ├── controller/     # Điều phối dữ liệu, cầu nối giữa View và Service
│   ├── model/          # Định nghĩa thực thể (Product, Customer, FlashSaleEvent, Voucher, Order,...)
│   ├── repository/     # Logic đọc/ghi CSV Generic, triển khai các cơ chế khóa vật lý & logic
│   ├── service/        # Xử lý nghiệp vụ chính, sinh dữ liệu mẫu, và động cơ giả lập Simulator
│   ├── view/           # Xử lý tương tác console, hiển thị Menu trực quan
│   └── exception/      # Định nghĩa các ngoại lệ nghiệp vụ tùy chỉnh
├── test/               # Unit tests với JUnit 5 cho dịch vụ, repository và dữ liệu
├── data/               # "Database" gồm các file CSV lưu trữ dữ liệu thực thể và nhật ký giao dịch
└── docs/               # Chứa slide, báo cáo PDF, sơ đồ UML (.puml) và sơ đồ luồng (.png)
```

### 🖼️ Sơ đồ UML & Thiết kế Hệ thống
Hệ thống được thiết kế bài bản với đầy đủ sơ đồ UML giúp lập trình viên dễ dàng làm quen và mở rộng:

* **Sơ đồ Lớp (Class Diagram):** [Xem chi tiết Class Diagram](docs/class_diagram.png)
* **Sơ đồ Ca sử dụng (Use Case Diagram):** [Xem chi tiết Use Case Diagram](docs/use_case_diagram.png)
* **Sơ đồ Cơ sở dữ liệu CSV:** [Xem chi tiết CSV Schema Documentation](docs/csv_schema.md)

### 🌊 Sơ đồ Luồng Nghiệp Vụ (Flowcharts)
* **Luồng Đặt hàng Flash Sale:** [Xem sơ đồ luồng Đặt hàng (Order Flow)](docs/flowcharts/order_flow.png)
* **Luồng Động cơ Giả lập Đa luồng:** [Xem sơ đồ luồng Giả lập (Simulator Flow)](docs/flowcharts/simulator_flow.png)
* **Luồng Tranh chấp Dữ liệu:** [Xem sơ đồ luồng Tranh chấp (Race Condition)](docs/flowcharts/race_condition_flow.png)
* **Luồng Kiểm soát Đơn hàng:** [Xem sơ đồ luồng Order Tracking](docs/flowcharts/order_tracking_flow.png)

---

## ✨ Các Tính Năng Chính (Core Features)

### 🛒 1. Khách Hàng (Customer Session)
* **Đăng ký & Đăng nhập:** Xác thực tài khoản khách hàng thông qua dữ liệu lưu trữ tại `customers.csv`. Tự động chặn và từ chối đăng nhập đối với các tài khoản bị **BANNED**.
* **Mua Hàng Flash Sale:** Đặt mua các sản phẩm đang có sự kiện Flash Sale diễn ra.
* **Hệ Thống VIP Tier:** Tự động tính toán chiết khấu hóa đơn dựa trên hạng thành viên của khách hàng:
  - `SILVER`: Giảm **2%** tổng đơn hàng.
  - `GOLD`: Giảm **5%** tổng đơn hàng.
  - `DIAMOND`: Giảm **10%** tổng đơn hàng.
* **Áp Dụng Voucher:** Tích hợp bộ lọc voucher hợp lệ, kiểm tra hạn dùng, số lượng voucher còn lại và tính toán số tiền giảm giá tối ưu nhất cho khách hàng.
* **Lịch Sử Giao Dịch:** Xem lịch sử các đơn hàng cá nhân đã đặt mua thành công.

### 🛡️ 2. Quản Trị Viên (Admin Session)
* **Quản Lý Khách Hàng (CRUD):** Thêm mới, cập nhật thông tin và xóa khách hàng.
* **Khóa/Mở Khóa Khách Hàng (Ban/Unban):** Thay đổi trạng thái hoạt động tài khoản khách hàng tức thì dưới DB CSV để cấm hoặc khôi phục quyền mua hàng.
* **Quản Lý Sự Kiện Flash Sale:** Tạo sự kiện mới, thiết lập khoảng thời gian hiệu lực và thêm các sản phẩm tham gia với giá chiết khấu đặc biệt và số lượng tồn kho giới hạn.
* **Báo Cáo Doanh Thu & Voucher:** Kết xuất thống kê tổng doanh số bán ra theo từng sự kiện và phân tích tần suất sử dụng các mã giảm giá.

### 🔬 3. Động Cơ Giả Lập & Thử Nghiệm (Simulation & Benchmark)
* **Đa Luồng Đồng Thời:** Sử dụng `ExecutorService` quản lý Thread Pool giả lập hàng trăm khách hàng cùng lúc bấm nút mua hàng.
* **Rào Chắn Đồng Bộ `CountDownLatch`:**
  - Thiết lập rào chắn chuẩn bị (`readyLatch`) để gom toàn bộ các luồng ở vạch xuất phát.
  - Kích hoạt rào chắn khởi chạy (`startLatch`) để phát lệnh đồng loạt mua hàng tại cùng một phần nghìn giây (mili-giây), mô phỏng áp lực tải đỉnh (peak load) thực tế.
* **Đo Đạc Hiệu Năng:** Thống kê chi tiết thời gian xử lý, tốc độ giao dịch (TPS - Transactions Per Second), tỷ lệ đặt hàng thành công và lỗi dữ liệu.

---

## 🔒 4 Cơ Chế Đồng Bộ Hóa Tránh Tranh Chấp (Concurrency Controls)

Dự án triển khai và thử nghiệm 4 cơ chế quản lý luồng để giải quyết xung đột khi ghi đè kho dữ liệu CSV:

1. **Không Sử Dụng Khóa (`NO_LOCK`):**
   - *Nguyên lý:* Cho phép các luồng tự do đọc ghi vào file CSV mà không có cơ chế chặn.
   - *Hậu quả:* Gây ra hiện tượng âm kho nghiêm trọng (Overselling) do các luồng đồng thời ghi đè thông tin lỗi thời lên nhau.
2. **Khóa Mức Vật Lý (`FILE_LOCK`):**
   - *Nguyên lý:* Sử dụng `FileChannel.lock()` của Java NIO để khóa tệp tin `flash_items.csv` ở cấp độ Hệ điều hành (OS-Level Lock).
   - *Đánh giá:* Đảm bảo an toàn tuyệt đối 100% cho dữ liệu nhưng tốc độ phản hồi cực kỳ chậm do I/O đĩa cứng bị nghẽn cổ chai tuần tự.
3. **Khóa Đồng Bộ Bộ Nhớ JVM (`SYNCHRONIZED`):**
   - *Nguyên lý:* Khóa đồng bộ ở cấp độ thread trong bộ nhớ JVM sử dụng synchronized block trên mã định danh sản phẩm được tối ưu bằng `itemId.intern()`.
   - *Đánh giá:* Chỉ block các giao dịch mua cùng một sản phẩm, các sản phẩm khác nhau vẫn được xử lý song song. Hiệu năng được cải thiện vượt bậc.
4. **Khóa Lạc Quan (`OPTIMISTIC_LOCK`):**
   - *Nguyên lý:* Dựa trên trường số hiệu phiên bản (`version`). Khi cập nhật dữ liệu, luồng sẽ kiểm tra xem version có bị thay đổi bởi luồng khác hay không. Nếu bị thay đổi, giao dịch sẽ tự động rollback và tiến hành đọc lại dữ liệu rồi thử lại (Retry Loop) tối đa 3 lần.
   - *Đánh giá:* Không gây nghẽn luồng (Non-blocking), mang lại Throughput (TPS) cao nhất, tối ưu cho hệ thống phân tán quy mô lớn.

---

## 📊 Kết Quả Thực Nghiệm Benchmark (500 Luồng - 100 Tồn Kho)

| Cơ chế khóa | Tổng số giao dịch | Đặt hàng thành công | Tỷ lệ âm kho | Tỷ lệ xung đột/Retry | Throughput (TPS) | Kết quả kiểm định |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **`NO_LOCK`** | 500 | 247 | **147%** (Bán lố 147 sản phẩm) | 0% | Cực cao (~500) | ❌ **FAIL** (Dữ liệu bị lỗi) |
| **`FILE_LOCK`** | 500 | 100 | **0%** | 0% (Xếp hàng vật lý) | Thấp (~12) |  **PASS** (Nhưng quá chậm) |
| **`SYNCHRONIZED`** | 500 | 100 | **0%** | 0% (Khóa mức JVM) | Trung bình (~85) |  **PASS** (Tốt cho đơn server) |
| **`OPTIMISTIC_LOCK`**| 500 | 100 | **0%** | ~76% (Có retry lại) | Cao (~210) |  **PASS** (Tối ưu hiệu năng nhất) |

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Ứng Dụng (Getting Started)

### 📋 Yêu Cầu Hệ Thống (Prerequisites)
* Java JDK 17 hoặc mới hơn
* Apache Maven 3.8 hoặc mới hơn

---

### 💻 Chạy Nhanh Trên Windows (One-Click Auto Setup)
Dự án được tích hợp sẵn file script thông minh giúp tự động kiểm tra, cài đặt môi trường Java & Maven và khởi chạy ứng dụng chỉ với một cú click chuột:

1. Double-click chạy tệp tin `setup_and_run_for_new_pc.bat` tại thư mục cha.
2. Script sẽ kiểm tra xem máy bạn đã cài Java JDK 17 và Maven chưa. Nếu chưa, script sử dụng công cụ `winget` của Windows để tự động cài đặt ngầm cực kỳ an toàn.
3. Dự án sẽ tự động được build và ứng dụng Console sẽ khởi chạy ngay lập tức.

---

### 🛠️ Chạy Thủ Công Qua Dòng Lệnh (Manual Command Line)

#### 1. Clone dự án và truy cập thư mục:
```bash
git clone https://github.com/nguyendinhtien824-super/Group-2.git
cd Group-2/NHOM_01_LAB211_FlashSale
```

#### 2. Dọn dẹp và Build đóng gói dự án:
```bash
mvn clean package
```
*Lưu ý:* Quá trình build thành công sẽ tạo ra file tệp tin thực thi `flash-sale-simulator-1.0.0.jar` nằm trong thư mục con `target/`.

#### 3. Chạy các bài kiểm thử tự động (Unit Tests):
```bash
mvn test
```

#### 4. Khởi chạy ứng dụng:
* **Trên Linux/macOS/Git Bash:**
  ```bash
  java -jar target/flash-sale-simulator-1.0.0.jar
  ```
* **Trên Windows Command Prompt/PowerShell:**
  Chạy trực tiếp file batch ở thư mục cha để đảm bảo hỗ trợ hiển thị Tiếng Việt UTF-8 chuẩn xác:
  ```cmd
  ..\run_project.bat
  ```

---

## 👥 Thông Tin Nhóm & Phân Công Nhiệm Vụ (Team Members)

Dự án được hoàn thiện bởi các thành viên thuộc **Nhóm 02 - Lớp LAB211**:

| MSSV | Họ và tên | Vai trò / Nhiệm vụ chính | Đóng góp |
|:---:|---|---|:---:|
| **QE200133** | Nguyễn Đình Tiến | **Trưởng nhóm**, thiết kế kiến trúc MVC, cài đặt Base Repository, viết bộ sinh dữ liệu tự động `DataGenerator` và cài đặt JUnit Tests. | 25% |
| **QE200141** | Trần Văn Phúc | Phụ trách cài đặt logic Đồng bộ hóa & Khóa (`FILE_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC_LOCK`), xây dựng động cơ giả lập simulator và thực hiện đo đạc benchmark. | 25% |
| **QE200105** | Nguyễn Trần Anh Kiệt | Xây dựng nghiệp vụ Khách hàng (Đăng ký, Đăng nhập, tích hợp VIP Tier và thuật toán áp dụng voucher tối ưu hóa chi phí hóa đơn). | 25% |
| **QE190032** | Đỗ Bá Quang Hưng | Thiết kế Menu Console, viết giao diện điều phối đầu vào, xử lý ngoại lệ biên tập và xây dựng chức năng Admin (CRUD Customer, Ban/Unban). | 25% |

---
*Dự án được phân phối dưới dạng tài liệu học tập của môn LAB211. Mọi đóng góp hoặc báo cáo lỗi xin vui lòng tạo Issue tại Repository gốc.*
