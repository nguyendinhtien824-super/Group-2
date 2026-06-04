# BÁO CÁO THUYẾT TRÌNH ĐỒ ÁN LAB211
## ĐỀ TÀI: MÔ PHỎNG SHOPEE FLASH SALE CONSOLE APP
### NHÓM 01

---

## 1. THÀNH VIÊN VÀ PHÂN CÔNG NHIỆM VỤ

* **Thành viên 1:** Hạ tầng OOP, Lớp cha `BaseEntity`, Generic `CsvRepository<T>`, và `DataGeneratorService`.
* **Thành viên 2:** Thực thể `Customer`, Phân quyền, CRUD & Ban/Unban tài khoản trên CSV, và cơ chế `FILE_LOCK`.
* **Thành viên 3:** Thực thể `FlashSaleItem`, Logic đặt hàng (Voucher, VIP Tier, check tồn kho), và cơ chế `SYNCHRONIZED`.
* **Thành viên 4:** Thực thể `Order`, Simulator đa luồng song song (`CountDownLatch`), và cơ chế `OPTIMISTIC_LOCK`.

---

## 2. KIẾN TRÚC MÔ HÌNH MVC TRONG DỰ ÁN

* **Model:** Đóng gói dữ liệu thực thể, chịu trách nhiệm parse/serialize dòng CSV.
* **Repository:** Giao tiếp trực tiếp với file CSV. Sử dụng Java Reflection trong Generic Repository để tránh viết lặp code CRUD.
* **Service:** Xử lý nghiệp vụ (Đặt hàng, tính chiết khấu VIP Tier, trừ kho, chạy Simulator).
* **Controller:** Điều phối dữ liệu từ View xuống Service/Repository.
* **View:** Console Interface hiển thị menu và nhận nhập liệu từ người dùng.

---

## 3. NGHIỆP VỤ KHÁCH HÀNG NÂNG CAO

* **Đăng nhập & Đăng ký:** Đọc/ghi thông tin trực tiếp từ `customers.csv`.
* **Hạng thành viên (Tier):** 
  * `STANDARD` (0%), `SILVER` (giảm 2%), `GOLD` (giảm 5%), `DIAMOND` (giảm 10%).
  * Tự động áp dụng chiết khấu trực tiếp trên tổng tiền trước khi trừ Voucher.
* **Trạng thái tài khoản (Status):**
  * `ACTIVE` (Bình thường), `BANNED` (Bị khóa).
  * Tài khoản bị `BANNED` sẽ lập tức bị chặn không cho đăng nhập và đặt hàng.
  * Admin có Menu quản lý riêng để thực hiện CRUD và Ban/Unban khách hàng trực tiếp.

---

## 4. BÀI TOÁN TRANH CHẤP KHO (CONCURRENCY)

* **Vấn đề:** 100 khách hàng cùng mua 1 sản phẩm còn lại duy nhất tại cùng một mili-giây.
* **NO_LOCK (Baseline):** 
  * Không khóa tài nguyên tranh chấp.
  * Nhiều luồng đọc cùng một giá trị kho cũ → cùng thấy còn hàng → cùng ghi đè trừ kho.
  * **Hậu quả:** Bán lố hàng, số lượng tồn kho bị âm (Negative Stock).

---

## 5. CƠ CHẾ 1: OS-LEVEL LOCK (FILE_LOCK)

* **Cách thức hoạt động:** Sử dụng `FileChannel.lock()` của Java NIO để khóa vật lý file `flash_items.csv` mức hệ điều hành.
* **Ưu điểm:** An toàn tuyệt đối 100%, không bị âm kho.
* **Nhược điểm:**
  * Hiệu năng TPS cực kỳ thấp (I/O đĩa cứng chặn các thread phải chờ tuần tự).
  * Dễ gây nghẽn và deadlock nếu không giải phóng khóa cẩn thận trong khối `finally`.

---

## 6. CƠ CHẾ 2: JVM-LEVEL MONITOR LOCK (SYNCHRONIZED)

* **Cách thức hoạt động:** Sử dụng từ khóa `synchronized` của Java để đồng bộ hóa.
* **Tối ưu hóa (Block-level Interning):**
  * Chỉ synchronized trên đối tượng khóa là chuỗi định danh sản phẩm: `synchronized (itemId.intern())`.
  * Tránh khóa toàn bộ hàm (gây nghẽn cả hệ thống). Chỉ các giao dịch mua **cùng một sản phẩm** mới phải xếp hàng đợi nhau.
* **Đánh giá:** Hiệu năng tốt trên môi trường đơn máy chủ (Single JVM).

---

## 7. CƠ CHẾ 3: LOGICAL LOCK (OPTIMISTIC_LOCK)

* **Cách thức hoạt động:** Khóa lạc quan không chặn luồng, hoạt động dựa trên thuộc tính `version`.
* **Luồng xử lý:**
  1. Đọc sản phẩm kèm `version = X`.
  2. Khi ghi đè xuống CSV, kiểm tra xem `version` trong tệp có còn bằng `X`.
  3. Nếu khớp: Cập nhật thành công, lưu `version = X + 1`.
  4. Nếu lệch (có luồng khác đã cập nhật trước): Báo lỗi conflict → Thực hiện đọc lại dữ liệu mới và chạy lại (Retry Loop) tối đa 3 lần.
* **Đánh giá:** Throughput (TPS) cao nhất, thích hợp cho hệ thống lớn.

---

## 8. THIẾT KẾ ĐỘNG CƠ GIẢ LẬP (SIMULATOR)

* **ExecutorService (Thread Pool):** Khởi chạy `N` luồng giả lập `N` khách hàng mua hàng đồng thời.
* **Đồng bộ hóa bằng CountDownLatch:**
  * `readyLatch`: Đợi toàn bộ các thread chuẩn bị xong ở vạch xuất phát.
  * `startLatch`: Kích nổ đồng thời các thread cùng đặt hàng tại một mili-giây.
  * `doneLatch`: Đợi toàn bộ thread hoàn thành để tính thời gian chạy và TPS.

---

## 9. KẾT QUẢ THỰC NGHIỆM (BENCHMARK)

*Cấu hình thử nghiệm: 500 Threads đồng thời, 100 sản phẩm.*

* **NO_LOCK:** TPS rất cao (~500), nhưng tỷ lệ âm kho lên tới **147%** (Thất bại).
* **FILE_LOCK:** 0% âm kho, nhưng TPS cực thấp (~12) do nghẽn I/O đĩa cứng.
* **SYNCHRONIZED (Block-level):** 0% âm kho, TPS khá tốt (~85), chạy ổn định.
* **OPTIMISTIC_LOCK:** 0% âm kho, TPS cao nhất (~210) nhờ cơ chế không block và retry loop.

---

## 10. BÀI HỌC RÚT RA TỪ DỰ ÁN

1. Thiết kế MVC và OOP giúp quản lý mã nguồn chặt chẽ, dễ phân công công việc.
2. Hiểu rõ sự khác biệt giữa khóa vật lý (I/O) và khóa bộ nhớ (JVM) trong môi trường Concurrency.
3. Khóa lạc quan (Optimistic Lock) là lựa chọn tối ưu cho hiệu năng nhưng đòi hỏi phải thiết kế cơ chế retry hợp lý.
4. Tận dụng AI để sinh mã khung nhanh chóng nhưng phải tự kiểm soát kiến trúc và logic đồng bộ hóa cốt lõi.
