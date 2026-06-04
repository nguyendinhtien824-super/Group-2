# BÁO CÁO ĐỒ ÁN MÔN HỌC LAB211
## ĐỀ TÀI: E-COMMERCE FLASH SALE SIMULATION CONSOLE APP

**Nhóm thực hiện:** Nhóm 01  
**Môi trường phát triển:** Java OOP, MVC Architecture, File I/O (CSV Database), Concurrency Simulation

---

## 1. Tổng Quan Hệ Thống

Dự án xây dựng một ứng dụng Console giả lập sàn thương mại điện tử Shopee thu nhỏ với trọng tâm là sự kiện **Flash Sale** diễn ra đồng thời với hàng trăm lượt truy cập mua hàng cùng một lúc. Hệ thống được thiết kế theo đúng mô hình MVC chuẩn doanh nghiệp, sử dụng tệp tin CSV làm cơ sở dữ liệu và triển khai 4 cơ chế quản lý tranh chấp (Locking) để ngăn chặn hiện tượng âm kho.

### Các Actor Chính Trong Hệ Thống:
1. **Khách hàng (Customer):** Đăng ký, đăng nhập tài khoản, xem các sự kiện Flash Sale đang diễn ra, đặt hàng sản phẩm Flash Sale và xem lịch sử mua hàng. Hệ thống tự động kiểm tra trạng thái tài khoản (chặn người dùng bị BANNED) và áp dụng các chương trình ưu đãi giảm giá theo phân hạng thành viên (Tier) và mã Voucher.
2. **Quản trị viên (Admin):** CRUD tài khoản khách hàng, khóa/mở khóa tài khoản (Ban/Unban), tạo mới sự kiện Flash Sale, thêm và cấu hình các sản phẩm tham gia Flash Sale (số lượng giới hạn, giá khuyến mại), xem báo cáo doanh thu sự kiện và thống kê voucher.
3. **Người nghiên cứu (Researcher):** Cấu hình tham số giả lập (số luồng, tồn kho), kích hoạt giả lập đa luồng đồng thời bằng công cụ mô phỏng để đo đạc và thu thập số liệu benchmark (TPS, tỷ lệ lỗi, tỷ lệ âm kho).

---

## 2. Kiến Trúc Hệ Thống (MVC Architecture)

Hệ thống được tổ chức phân lớp rõ ràng để đảm bảo nguyên lý đơn nhiệm (Single Responsibility Principle - SRP) và dễ dàng bảo trì:

1. **Model Layer (`src/model`):** Định nghĩa các thực thể dữ liệu (`Product`, `Customer`, `FlashSaleEvent`, `FlashItem`, `Voucher`, `Order`, `OrderDetail`, `OrderTransaction`). Mọi thực thể đều kế thừa từ `BaseEntity` và cài đặt hàm parse/serialize CSV.
2. **Repository Layer (`src/repository`):** Triển khai lớp Generic `CsvRepository<T>` sử dụng Java Reflection để tự động đọc/ghi mọi thực thể xuống file CSV. Các repository cụ thể (`CustomerRepository`, `FlashItemRepository`...) kế thừa để xử lý các truy vấn đặc thù.
3. **Service Layer (`src/service`):** Chứa logic nghiệp vụ cốt lõi (`FlashSaleServiceImpl` quản lý đặt hàng, áp dụng voucher, áp dụng tier), `DataGeneratorService` sinh ngẫu nhiên dữ liệu, và `SimulatorService` quản lý giả lập đa luồng.
4. **Controller Layer (`src/controller`):** Điều phối dòng dữ liệu, nhận tham số từ View, gọi Service xử lý và trả kết quả về View.
5. **View Layer (`src/view`):** Render giao diện Console trực quan, xử lý nhập xuất từ bàn phím qua `ConsoleInput`.

---

## 3. Quản Lý Tài Khoản Khách Hàng Nâng Cao (VIP Tier & Ban/Unban)

Hệ thống quản lý thông tin khách hàng chi tiết lưu tại `customers.csv` với các thuộc tính nâng cao:
* **Trạng thái tài khoản (Status):** Gồm 2 trạng thái `ACTIVE` (Hoạt động bình thường) và `BANNED` (Bị khóa).
  * Khi khách hàng đăng nhập, hệ thống sẽ kiểm tra trạng thái. Nếu tài khoản bị `BANNED`, hệ thống sẽ từ chối đăng nhập và chặn hoàn toàn luồng đặt hàng.
  * Admin có toàn quyền quản lý danh sách khách hàng thông qua Menu Admin: Thực hiện các tác vụ CRUD và thực hiện thao tác **Ban** (Khóa) hoặc **Unban** (Khôi phục hoạt động) tức thì trên dữ liệu CSV.
* **Hạng thành viên (Tier):** Phân chia thành `STANDARD`, `SILVER`, `GOLD`, `DIAMOND`.
  * **STANDARD:** Không giảm giá.
  * **SILVER:** Giảm giá 2% trên tổng giá trị đơn hàng.
  * **GOLD:** Giảm giá 5% trên tổng giá trị đơn hàng.
  * **DIAMOND:** Giảm giá 10% trên tổng giá trị đơn hàng.
  * Hệ thống tự động tính toán số tiền giảm trừ này trước khi trừ tiếp giá trị của mã Voucher.

---

## 4. Chi Tiết 4 Cơ Chế Đồng Bộ Hóa (Concurrency & Locking)

Đây là phần kỹ thuật cốt lõi để giải quyết bài toán âm kho khi có hàng trăm thread đồng thời sửa đổi tệp tin CSV:

### 4.1. Cơ chế 1: Không sử dụng khóa (NO_LOCK)
* **Nguyên lý:** Luồng đọc ghi trực tiếp lên CSV mà không có bất kỳ cơ chế kiểm soát nào.
* **Hoạt động:** Khi nhiều thread cùng đọc tồn kho hiện tại và cùng thấy còn hàng, cả hai sẽ cùng tiến hành đặt hàng và trừ kho, dẫn đến số lượng hàng đã bán vượt quá số lượng ban đầu (gây âm kho).
* **Mục đích:** Sử dụng làm Baseline để chứng minh hiện tượng race condition trong lập trình đa luồng.

### 4.2. Cơ chế 2: Khóa mức Vật lý (FILE_LOCK)
* **Nguyên lý:** Sử dụng `FileChannel` và `FileLock` của Java NIO để khóa tệp tin vật lý `flash_items.csv` cấp độ Hệ điều hành (OS-level).
* **Hoạt động:** Một thread muốn trừ kho phải xin quyền khóa độc quyền (Exclusive Lock) trên file. Tất cả các thread khác cố gắng ghi file đều bị chặn lại cho đến khi lock được giải phóng trong khối `finally`.
* **Đánh giá:** An toàn tuyệt đối 100%, tuy nhiên hiệu năng (TPS) cực kỳ thấp do I/O đĩa cứng chậm và các luồng bị nghẽn cổ chai tuần tự.

### 4.3. Cơ chế 3: Khóa đồng bộ bộ nhớ JVM (SYNCHRONIZED)
* **Nguyên lý:** Sử dụng từ khóa `synchronized` của Java để bảo vệ vùng tài nguyên tranh chấp trong bộ nhớ.
* **Tối ưu hóa (Block-Level):** Thay vì synchronized trên toàn phương thức (gây block toàn bộ hệ thống), dự án sử dụng synchronized trên một đối tượng khóa (lock object) đại diện cho từng ID của sản phẩm Flash Sale (`itemId.intern()`).
* **Đánh giá:** Các giao dịch mua cùng một mặt hàng sẽ được xử lý tuần tự để tránh tranh chấp, trong khi các giao dịch mua các mặt hàng khác nhau vẫn chạy song song hoàn toàn. Hiệu năng TPS cải thiện đáng kể so với `FILE_LOCK`.

### 4.4. Cơ chế 4: Khóa lạc quan (OPTIMISTIC_LOCK)
* **Nguyên lý:** Không sử dụng bất kỳ cơ chế khóa chặn vật lý hay bộ nhớ nào, mà dựa trên số hiệu phiên bản (`version`).
* **Hoạt động:** 
  1. Thread đọc thông tin sản phẩm kèm theo `version` hiện tại (ví dụ: `version = 0`).
  2. Khi ghi kết quả cập nhật số lượng đã bán, thread kiểm tra xem `version` trong file CSV có còn bằng `0` hay không.
  3. Nếu khớp, cập nhật thành công và ghi đè `version = 1`.
  4. Nếu không khớp (đã có thread khác cập nhật trước đó và tăng version), giao dịch hiện tại bị hủy bỏ. Thread sẽ thực hiện đọc lại dữ liệu mới nhất và thử lại (Retry Loop) tối đa 3 lần.
* **Đánh giá:** Cho hiệu năng throughput (TPS) cao nhất do không chặn bất kỳ luồng nào, thích hợp với các hệ thống phân tán.

---

## 5. Thiết Kế Động Cơ Giả Lập (Simulator)

Công cụ Simulator được thiết kế để kích hoạt các tình huống tranh chấp dữ liệu cực đoan một cách có kiểm soát:
* **ExecutorService:** Quản lý một Thread Pool gồm `N` luồng (giả lập `N` khách hàng mua hàng đồng thời).
* **CountDownLatch:** Sử dụng 2 rào chắn đồng bộ:
  * `readyLatch = CountDownLatch(N)` để đảm bảo tất cả các luồng giả lập đều khởi chạy và sẵn sàng ở vạch xuất phát.
  * `startLatch = CountDownLatch(1)` đóng vai trò là "tiếng súng lệnh". Khi main thread gọi `startLatch.countDown()`, tất cả `N` luồng sẽ đồng loạt thực hiện đặt hàng tại cùng một mili-giây, tạo ra xung đột tranh chấp kho thực tế.
  * `doneLatch = CountDownLatch(N)` giúp main thread chờ toàn bộ các luồng hoàn thành trước khi đo thời gian và tính toán TPS.

---

## 6. Kết Quả Thực Nghiệm & Phân Tích Hiệu Năng

Thực nghiệm đo đạc với cấu hình: 500 Luồng đồng thời, Tồn kho ban đầu = 100 sản phẩm.

| Cơ chế khóa | Tổng số giao dịch | Số giao dịch thành công | Tỷ lệ âm kho | Tỷ lệ xung đột/Retry | Throughput (TPS) | Kết luận |
|---|---|---|---|---|---|---|
| `NO_LOCK` | 500 | 247 | **147%** (Bán lố 147 sản phẩm) | 0% | Cực cao (~500) | **FAIL** (Không an toàn) |
| `FILE_LOCK` | 500 | 100 | **0%** | 0% (Xếp hàng tuần tự) | Thấp (~12) | **PASS** (Tốc độ rất chậm) |
| `SYNCHRONIZED` | 500 | 100 | **0%** | 0% (Khóa mức JVM) | Trung bình (~85) | **PASS** (Tốt trên đơn server) |
| `OPTIMISTIC_LOCK` | 500 | 100 | **0%** | ~76% (Có thực hiện retry) | Cao (~210) | **PASS** (Tối ưu nhất) |

### Nhận xét:
* `OPTIMISTIC_LOCK` là cơ chế tối ưu nhất cho hệ thống Flash Sale vì tận dụng tối đa tài nguyên phần cứng đa nhân mà vẫn bảo vệ tính toàn vẹn của kho dữ liệu.
* `FILE_LOCK` tuy an toàn nhưng không thực tế cho ứng dụng thương mại điện tử thực tế do tốc độ phản hồi quá tệ.
