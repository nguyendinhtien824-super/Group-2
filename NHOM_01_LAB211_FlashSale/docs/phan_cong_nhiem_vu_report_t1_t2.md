# PHÂN CHIA NHIỆM VỤ VIẾT BÁO CÁO TUẦN 1 & 2 (NHÓM 01)

**Dự án:** LAB211 - E-Commerce Flash Sale Simulation Console App  
**Mục tiêu Tuần 1 - 2:** **Thiết kế hệ thống thuần lý thuyết & sơ đồ** (Use Cases, Class Diagram, Flowcharts, CSV Database Schema). Chưa cài đặt mã nguồn (Code).

Bản phân chia này bám sát **Bản đồ Phân chia Trọng tâm Kỹ thuật** và các sơ đồ thực tế trong thư mục `docs/flowcharts/` của dự án để phân bổ nhiệm vụ đồng đều và hợp lý nhất cho 4 thành viên.

---

## BẢNG TỔNG HỢP PHÂN CHIA NHIỆM VỤ BÁO CÁO TUẦN 1 - 2

| Thành viên | Trọng tâm phân tích kỹ thuật | Sơ đồ Kỹ thuật phụ trách vẽ & giải thích | Thiết kế Schema CSV phụ trách |
| :--- | :--- | :--- | :--- |
| **TV1** | - Kiến trúc tổng quan hệ thống<br>- Kiến trúc phân lớp MVC (SRP) tổng thể<br>- Trọng tâm OOP: Abstraction & Polymorphism trong Generic Repository | - **Sơ đồ UML Class Diagram tổng thể** (`class_diagram.puml`) <br>- **Sơ đồ luồng sinh dữ liệu mẫu** (`datagen_flow.puml`) | `products.csv`<br>`flash_events.csv` |
| **TV2** | - Phân tích các Tác nhân (Actor) trong hệ thống<br>- Cơ chế đóng gói thông tin Khách hàng, phân hạng VIP & trạng thái Banned<br>- Khái niệm khóa vật lý cấp Hệ điều hành (FILE_LOCK) | - **Sơ đồ Use Case hệ thống** (`use_case_diagram.puml`) <br>- **Sơ đồ luồng khóa vật lý** (`file_lock_flow`) | `customers.csv` |
| **TV3** | - Nghiệp vụ đặt hàng & các ràng buộc Flash Sale (kho, giới hạn mua tối đa 2)<br>- Custom Exceptions phục vụ quản lý lỗi nghiệp vụ<br>- Khái niệm khóa vùng nhớ JVM (SYNCHRONIZED) | - **Sơ đồ luồng đặt hàng** (`order_flow.puml`) <br>- **Sơ đồ luồng khóa đồng bộ** (`synchronized_flow`) | `flash_items.csv`<br>`order_details.csv` |
| **TV4** | - Động cơ giả lập đa luồng Simulator (Thread Pool & CountDownLatch)<br>- Khái niệm khóa logic phiên bản (OPTIMISTIC_LOCK)<br>- Hiện tượng Race Condition khi không dùng khóa | - **Sơ đồ luồng giả lập Simulator** (`simulator_flow.puml`) <br>- **Sơ đồ luồng khóa lạc quan** (`optimistic_flow`)<br>- **Sơ đồ tranh chấp Baseline** (`race_condition_flow.puml`) | `orders.csv`<br>`transactions.csv` |

---

## CHI TIẾT NHIỆM VỤ TỪNG THÀNH VIÊN TRONG BÁO CÁO TUẦN 1 - 2

### 1. Thành viên 1 (TV1): Tổng quan Kiến trúc MVC, Khung OOP & Sinh Dữ liệu
* **Nhiệm vụ phân tích lý thuyết:**
  * **Tổng quan Kiến trúc:** Trình bày cấu trúc phân lớp MVC (Model - View - Controller), phân định ranh giới và trách nhiệm của từng package (`src/model`, `src/repository`, `src/service`...) theo nguyên lý Đơn nhiệm (Single Responsibility Principle - SRP).
  * **Cơ chế OOP trừu tượng & Đa hình:** Giải thích thiết kế lớp cha `BaseEntity` và giải pháp thiết kế Generic `CsvRepository<T>` sử dụng Java Reflection giúp thao tác với file dữ liệu tự động thay vì code cứng cho từng lớp.
  * **Cơ chế Sinh dữ liệu mẫu:** Mô tả logic hoạt động của trình sinh dữ liệu lớn (`DataGeneratorService`) đảm bảo tính toàn vẹn quan hệ khóa ngoại.
* **Sơ đồ kỹ thuật phải hoàn thiện & giải thích trong báo cáo:**
  1. **Sơ đồ UML Class Diagram tổng thể** (mối liên kết giữa các thực thể kế thừa `BaseEntity`).
  2. **Sơ đồ luồng sinh dữ liệu** (`datagen_flow.puml`): Biểu diễn quy trình đọc/ghi tuần tự các file thô.
* **Thiết kế Schema CSV phụ trách đặc tả:**
  * `products.csv` (Cấu trúc bảng, kiểu dữ liệu, các thuộc tính ID, tên, giá, kho gốc...).
  * `flash_events.csv` (Mã sự kiện, thời gian bắt đầu, thời gian kết thúc, trạng thái).

---

### 2. Thành viên 2 (TV2): Phân tích Tác nhân, Use Case & Khóa Vật lý (FILE_LOCK)
* **Nhiệm vụ phân tích lý thuyết:**
  * **Phân tích Tác nhân (Actor):** Phân tích chi tiết hành vi, quyền hạn của 3 Actor: Khách hàng (Customer), Quản trị viên (Admin) và Người nghiên cứu (Researcher).
  * **Cơ chế đóng gói (Encapsulation):** Phân tích thiết kế của thực thể `Customer` (mật khẩu mã hóa, VIP Tier để giảm giá đơn hàng SILVER/GOLD/DIAMOND, cơ chế khóa tài khoản BANNED).
  * **Cơ chế Khóa vật lý (FILE_LOCK):** Định nghĩa lý thuyết về khóa kênh dữ liệu cấp Hệ điều hành (Java NIO FileChannel Lock) và ưu/nhược điểm (an toàn 100% nhưng nghẽn đĩa cứng).
* **Sơ đồ kỹ thuật phải hoàn thiện & giải thích trong báo cáo:**
  1. **Sơ đồ Use Case chi tiết hệ thống** (`use_case_diagram.puml`): Thể hiện sự tương tác của 3 Actor với các chức năng.
  2. **Sơ đồ luồng khóa vật lý (File Lock Flow):** Biểu diễn quá trình tranh chấp khi một thread độc chiếm quyền ghi file vật lý, các thread khác rơi vào trạng thái chờ (blocked).
* **Thiết kế Schema CSV phụ trách đặc tả:**
  * `customers.csv` (Cấu trúc thông tin khách hàng, phân hạng VIP và trạng thái hoạt động).

---

### 3. Thành viên 3 (TV3): Logic Nghiệp vụ Đặt hàng, Custom Exceptions & Khóa JVM (SYNCHRONIZED)
* **Nhiệm vụ phân tích lý thuyết:**
  * **Nghiệp vụ mua hàng Flash Sale:** Phân tích logic quy trình mua hàng đơn luồng, ràng buộc kiểm tra tồn kho và giới hạn mua tối đa 2 sản phẩm/khách hàng.
  * **Cơ chế xử lý lỗi hệ thống:** Thiết kế hệ thống Custom Exceptions tùy biến (`OutOfStockException`, `LimitExceededException`...) phục vụ giao tiếp nghiệp vụ rõ ràng giữa các tầng.
  * **Cơ chế Khóa vùng nhớ JVM (SYNCHRONIZED):** Định nghĩa khóa đồng bộ cấp độ ứng dụng. Giải thích phương án tối ưu khóa mức Khối (Block-Level Synchronized) trên ID sản phẩm (`itemId.intern()`) để nâng cao throughput so với khóa toàn phương thức.
* **Sơ đồ kỹ thuật phải hoàn thiện & giải thích trong báo cáo:**
  1. **Sơ đồ luồng đặt hàng đơn luồng** (`order_flow.puml`): Luồng kiểm tra nghiệp vụ từ lúc bấm mua đến khi ghi nhận đơn hàng thành công.
  2. **Sơ đồ luồng khóa đồng bộ (Synchronized Flow):** Trực quan hóa cơ chế xếp hàng đợi monitor lock của JVM trên cùng một sản phẩm Flash Sale.
* **Thiết kế Schema CSV phụ trách đặc tả:**
  * `flash_items.csv` (Mặt hàng Flash Sale: giá sale, số lượng giới hạn, số lượng đã bán, phiên bản version).
  * `order_details.csv` (Chi tiết dòng hóa đơn đơn hàng).

---

### 4. Thành viên 4 (TV4): Động cơ Giả lập Concurrency, Khóa Lạc quan & Race Condition
* **Nhiệm vụ phân tích lý thuyết:**
  * **Kiến trúc Simulator mô phỏng tải cực đoan:** Phân tích giải pháp sử dụng Thread Pool (`ExecutorService`) và sự phối hợp của 3 rào chắn `CountDownLatch` (`readyLatch`, `startLatch`, `doneLatch`) để kích nổ hàng trăm luồng khách hàng đồng thời tại một mili-giây.
  * **Cơ chế Khóa lạc quan (OPTIMISTIC_LOCK):** Giải thích giải pháp so khớp phiên bản (`version-based matching`) không block luồng và cơ chế tự động thử lại (Retry Loop tối đa 3 lần).
  * **Phân tích hiện tượng Race Condition (NO_LOCK):** Lý giải tại sao việc đọc/ghi file thô đồng thời không kiểm soát dẫn đến lỗi âm kho (bán quá giới hạn tồn kho thực tế).
* **Sơ đồ kỹ thuật phải hoàn thiện & giải thích trong báo cáo:**
  1. **Sơ đồ luồng giả lập Simulator** (`simulator_flow.puml`): Luồng điều phối các thread giả lập từ vạch xuất phát đến khi đo đạc hiệu năng.
  2. **Sơ đồ luồng khóa lạc quan (Optimistic Lock Flow):** Quy trình đọc version, so sánh khi cập nhật và xử lý retry loop.
  3. **Sơ đồ tranh chấp Baseline** (`race_condition_flow.puml`): Chỉ ra điểm xung đột giữa các luồng đọc/ghi đồng thời gây âm kho.
* **Thiết kế Schema CSV phụ trách đặc tả:**
  * `orders.csv` (Thông tin tổng quát đơn hàng).
  * `transactions.csv` (Nhật ký giao dịch chi tiết phục vụ simulator thống kê hiệu năng TPS).

---

## HƯỚNG DẪN LÀM BÀI VÀ RÁP BÁO CÁO

1. **Vẽ sơ đồ:** Các thành viên sử dụng PlantUML để kết xuất sơ đồ ra định dạng `.png` rõ nét (các tệp `.puml` lưu trữ trong thư mục `docs/flowcharts/` và `docs/`).
2. **Quy chuẩn định nghĩa Schema CSV:** Mô tả rõ tên bảng, tên các header cột, kiểu dữ liệu Java tương ứng và ví dụ dòng dữ liệu cụ thể.
3. **Cách ráp bài:** Các thành viên gửi nội dung phân tích dạng Word/Markdown kèm hình ảnh sơ đồ cho trưởng nhóm để tổng hợp vào báo cáo chính [report.md](file:///c:/Users/Hi/Downloads/shopeeconsole/NHOM_01_LAB211_FlashSale/docs/report.md) trước hạn chót.
