# BẢNG PHÂN CÔNG NHIỆM VỤ DỰ ÁN (HƯỚNG TRỌNG TÂM KỸ THUẬT CỐT LÕI)

**Tên dự án:** LAB211 - E-Commerce Flash Sale Simulation Console App  
**Hình thức:** Java OOP, MVC, dữ liệu CSV, chạy bằng console  
**Số lượng thành viên:** 04 người  

---

> [!IMPORTANT]
> **ĐỊNH HƯỚNG BẢO VỆ ĐỒ ÁN LAB211 (QUAN TRỌNG HƠN GIAO DIỆN):**  
> Giáo viên LAB211 chấm điểm **ĐÉO QUAN TÂM ĐẾN GIAO DIỆN XẤU HAY ĐẸP**. View chỉ là phần vỏ để kích hoạt chương trình. Giáo viên sẽ xoáy sâu 100% vào:
> 1. **Kiến trúc MVC & OOP**: SRP (Single Responsibility), Abstraction, Polymorphism, Encapsulation được code thế nào.
> 2. **Xử lý Đa luồng (Concurrency)**: Tại sao xảy ra race condition làm âm kho, cơ chế hoạt động của `CountDownLatch`.
> 3. **Cơ chế Khóa (Locking)**: Cách code chi tiết và hoạt động thực tế bên dưới của `FILE_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC_LOCK`.
> 4. **Xử lý File I/O**: Đọc/ghi CSV an toàn, parse dữ liệu động, tránh corrupt file.
> 
> Bản phân công này tập trung **100% vào Core Logic Kỹ Thuật** để đảm bảo đứa nào cũng phải code tính năng lõi và trả lời xuất sắc các câu hỏi "sát thủ" của giáo viên.

---

## 1. Bản Đồ Phân Chia Trọng Tâm Kỹ Thuật (OOP & Concurrency)

| Thành viên | Trọng tâm OOP phụ trách | Trọng tâm Concurrency & Lock | File Logic Cốt Lõi Phải Code | Sơ đồ Kỹ Thuật Phải Vẽ |
| :--- | :--- | :--- | :--- | :--- |
| **TV1** | **Abstraction & Polymorphism** (Thiết kế lớp cha, Generic Repository động) | **Baseline Race Condition** (Chứng minh luồng không đồng bộ gây âm kho) | `BaseEntity.java`, Generic `CsvRepository<T>`, `DataGeneratorService.java` | Sơ đồ luồng sinh dữ liệu & Sơ đồ UML Class Diagram tổng thể |
| **TV2** | **Encapsulation & Domain Model** (Đóng gói thông tin, quản lý trạng thái thực thể) | **Physical Lock (FILE_LOCK)** (Khóa kênh FileChannel mức OS) | `Customer.java`, `FlashSaleEvent.java`, `FILE_LOCK` implementation | Sơ đồ Use Case hệ thống & Sơ đồ luồng vật lý `file_lock_flow` |
| **TV3** | **Business Logic & Error Handling** (Ràng buộc nghiệp vụ chặt chẽ, Custom Exceptions) | **Application Lock (SYNCHRONIZED)** (Khóa vùng nhớ monitor của JVM) | `FlashSaleItem.java`, `OrderDetail.java`, `SYNCHRONIZED` logic | Sơ đồ luồng đặt hàng & Sơ đồ luồng khóa đồng bộ `synchronized_flow` |
| **TV4** | **System Integration & Multi-threading** (Thiết kế động cơ kiểm thử đa luồng đồng thời) | **Logical Lock (OPTIMISTIC_LOCK)** (Khóa lạc quan với version & retry) | `SimulatorService.java`, `OrderTransaction.java`, `OPTIMISTIC_LOCK` logic | Sơ đồ luồng Simulator & Sơ đồ luồng khóa lạc quan `optimistic_flow` |

---

## 2. Chi Tiết Phân Công & Bộ Câu Hỏi Phản Biện "Thực Chiến"

*(Giao diện View chỉ là phần vỏ kích hoạt, code View được chia đều để đủ cấu trúc MVC, phần dưới đây tập trung hoàn toàn vào Logic)*

---

### 2.1. Thành viên 1: Hạ tầng OOP, Generic CSV & Baseline Concurrency (Cơ chế: NO_LOCK)

#### A. File Code Lõi Phụ Trách:
1. `BaseEntity.java` (Lớp trừu tượng định nghĩa các trường dữ liệu chung: `id`, `version`, `createdAt`, `updatedAt`).
2. Generic `CsvRepository.java` (Lớp generic sử dụng Reflection của Java để tự động ánh xạ thuộc tính của mọi Entity thành dòng CSV và ngược lại).
3. `ProductRepository.java` & `FlashEventRepository.java` (Repository cụ thể thao tác với tệp).
4. `DataGeneratorService.java` (Logic sinh ngẫu nhiên dữ liệu có quan hệ khóa ngoại chặt chẽ ≥ 10.000 dòng).
5. Nghiệp vụ đặt hàng không dùng khóa (`NO_LOCK` flow) tại Service để cố tình tạo ra race condition.

#### B. Trọng tâm giáo viên sẽ hỏi (Câu hỏi Phản biện Kỹ thuật):
> [!CAUTION]
> **Câu hỏi 1: Em hãy giải thích tính đa hình (Polymorphism) và trừu tượng (Abstraction) được thể hiện như thế nào trong lớp Generic `CsvRepository<T>`?**
> - *Trả lời cốt lõi:* Sử dụng tham số kiểu generic `<T extends BaseEntity>`. Nhờ đó, phương thức đọc/ghi file chỉ cần viết một lần nhưng áp dụng đa hình cho mọi Entity (Product, Customer, Order...). Sử dụng Java Reflection (`getDeclaredFields()`) để lấy động danh sách trường của lớp `T` tại thời điểm runtime nhằm tự động parse/serialize dữ liệu CSV mà không cần code cứng.
> 
> **Câu hỏi 2: Tại sao cơ chế `NO_LOCK` lại xảy ra hiện tượng âm kho (Negative Stock)? Race condition xảy ra cụ thể ở dòng code nào trong luồng xử lý của em?**
> - *Trả lời cốt lõi:* Race condition xảy ra ở khoảng trống giữa 2 bước: "Đọc tồn kho hiện tại lên bộ nhớ" và "Ghi số lượng mới (trừ kho) xuống file CSV". Khi nhiều thread cùng nhảy vào lúc số lượng tồn kho = 1: cả 2 thread cùng đọc được stock = 1 -> cả 2 cùng kiểm tra thấy hợp lệ -> cả 2 cùng ghi đè stock = 0 xuống file, dẫn đến thực tế bán được 2 sản phẩm (âm kho).

---

### 2.2. Thành viên 2: Đóng gói Thực thể, Phân quyền & Khóa Vật lý (Cơ chế: FILE_LOCK)

#### A. File Code Lõi Phụ Trách:
1. `Customer.java` (Đóng gói thông tin khách hàng, hạng VIP/PREMIUM, mật khẩu mã hóa, tích hợp trạng thái ACTIVE/BANNED).
2. `FlashSaleEvent.java` (Quản lý trạng thái sự kiện, thời gian start/end).
3. `CustomerRepository.java` (Tìm kiếm khách hàng theo email/username, lưu thông tin auth, cập nhật trạng thái Ban/Unban).
4. `AdminCustomerView.java` (Tạo giao diện quản lý tài khoản khách hàng CRUD, Ban/Restore cho Admin).
5. `CustomerController.java` (Xử lý nghiệp vụ Đăng ký/Đăng nhập, xác thực quyền truy cập).
6. Triển khai cơ chế **FILE_LOCK** (Sử dụng `FileChannel` và `FileLock` của Java NIO tại repository để khóa vật lý tệp CSV khi trừ kho).

#### B. Trọng tâm giáo viên sẽ hỏi (Câu hỏi Phản biện Kỹ thuật):
> [!CAUTION]
> **Câu hỏi 1: Cơ chế `FILE_LOCK` em code hoạt động ở mức độ nào? Nếu có exception xảy ra trong lúc đang giữ Lock thì làm sao em đảm bảo giải phóng khóa để tránh treo hệ thống (Deadlock)?**
> - *Trả lời cốt lõi:* `FileLock` hoạt động ở mức độ Hệ điều hành (OS-level file lock). Để đảm bảo giải phóng khóa an toàn tuyệt đối khi xảy ra lỗi (như lỗi parse dữ liệu, lỗi IO), em sử dụng khối lệnh `try-with-resources` hoặc đặt lệnh `lock.release()` bên trong khối `finally`. Nhờ đó, dù code có ném ra bất kỳ Runtime Exception nào thì khóa vẫn được tự động giải phóng.
> 
> **Câu hỏi 2: Tại sao `FILE_LOCK` ngăn chặn được 100% âm kho nhưng Throughput (TPS) thu được lại cực kỳ thấp so với các cơ chế khác?**
> - *Trả lời cốt lõi:* Vì `FILE_LOCK` khóa toàn bộ tệp vật lý CSV. Tại một thời điểm, chỉ có duy nhất một thread được quyền đọc/ghi file này, các thread khác phải xếp hàng đợi (block). Do I/O đĩa cứng rất chậm, việc bắt các thread phải chờ đợi tuần tự hóa (serialization) trên file khiến hiệu năng hệ thống giảm thê thảm.

---

### 2.3. Thành viên 3: Logic Ràng buộc Nghiệp vụ, Ngoại lệ & Khóa JVM (Cơ chế: SYNCHRONIZED)

#### A. File Code Lõi Phụ Trách:
1. `FlashSaleItem.java` (Thực thể sản phẩm flash sale: limitedQty, soldQty, version).
2. `OrderDetail.java` (Chi tiết đơn hàng).
3. `OrderDetailRepository.java` & `FlashItemRepository.java` (Repository xử lý đọc ghi chi tiết đơn hàng và cập nhật soldQty sản phẩm).
4. `OrderController.java` (Xử lý nghiệp vụ check tồn kho, check giới hạn mua tối đa 2 sản phẩm cùng loại trên một khách hàng).
5. Thiết kế hệ thống Custom Exceptions (`OutOfStockException`, `LimitExceededException`...).
6. Triển khai cơ chế **SYNCHRONIZED** (Đồng bộ hóa luồng sử dụng từ khóa `synchronized` của Java tại tầng nghiệp vụ trừ kho).

#### B. Trọng tâm giáo viên sẽ hỏi (Câu hỏi Phản biện Kỹ thuật):
> [!CAUTION]
> **Câu hỏi 1: Em hãy giải thích sự khác biệt giữa việc đặt từ khóa `synchronized` ở mức độ phương thức (Method-level) và mức độ khối (Block-level)? Trong dự án này em dùng loại nào và tại sao?**
> - *Trả lời cốt lõi:* `synchronized` mức phương thức sẽ khóa toàn bộ đối tượng (object instance), khiến các luồng khác không thể gọi bất kỳ hàm synchronized nào khác trên đối tượng đó. Em sử dụng `synchronized` mức khối (Block-level) với monitor lock là một đối tượng cụ thể (ví dụ: lock trên ID của sản phẩm Flash Sale). Việc này giúp thu hẹp phạm vi đồng bộ (critical section), chỉ khóa luồng đối với những giao dịch mua **cùng một sản phẩm**, các giao dịch mua sản phẩm khác nhau vẫn có thể chạy song song hoàn toàn, giúp tăng throughput.
> 
> **Câu hỏi 2: Tại sao việc kiểm tra giới hạn mua (tối đa 2 sản phẩm/khách hàng) và trừ kho bắt buộc phải đặt trong vùng đồng bộ hóa (synchronized block) mà không được đặt bên ngoài?**
> - *Trả lời cốt lõi:* Nếu đặt bên ngoài synchronized block, hai luồng của cùng một khách hàng chạy đồng thời có thể cùng đọc thấy số lượng đã mua = 0 -> cả hai cùng kiểm tra thấy thỏa mãn điều kiện `<= 2` -> cả hai cùng tiến hành ghi đơn hàng, kết quả khách hàng mua được 4 sản phẩm (vượt giới hạn). Toàn bộ chuỗi "Kiểm tra giới hạn -> Kiểm tra tồn kho -> Trừ kho" phải được xử lý như một thao tác nguyên tử (atomic operation) bên trong lock.

---

### 2.4. Thành viên 4: Động cơ Đa luồng Simulator & Khóa Lạc quan (Cơ chế: OPTIMISTIC_LOCK)

#### A. File Code Lõi Phụ Trách:
1. `Order.java` & `OrderTransaction.java` (Entity thông tin đơn hàng và lịch sử giao dịch mô phỏng).
2. `OrderRepository.java` & `OrderTransactionRepository.java` (Ghi nhận kết quả giao dịch và lưu log).
3. `SimulatorService.java` (Sử dụng `ExecutorService` quản lý Thread Pool và `CountDownLatch` để kích nổ đồng thời).
4. `SimulationResult.java` (Đóng gói kết quả đo đạc: TPS, tỉ lệ âm kho, số lần retry...).
5. Triển khai cơ chế **OPTIMISTIC_LOCK** (Khóa lạc quan so khớp giá trị thuộc tính `version` khi ghi đè, thực hiện vòng lặp retry tối đa 3 lần nếu phát hiện xung đột dữ liệu).

#### B. Trọng tâm giáo viên sẽ hỏi (Câu hỏi Phản biện Kỹ thuật):
> [!CAUTION]
> **Câu hỏi 1: Em sử dụng `CountDownLatch` như thế nào để đảm bảo hàng trăm thread cùng thực hiện đặt hàng tại cùng một thời điểm micro-giây? Tại sao không dùng vòng lặp khởi tạo thread thông thường?**
> - *Trả lời cốt lõi:* Nếu dùng vòng lặp thông thường, thread được khởi tạo trước sẽ chạy trước và kết thúc trước khi thread cuối cùng được tạo ra (xử lý tuần tự). Em sử dụng `CountDownLatch(1)` khởi tạo với count = 1. Tất cả các thread giả lập sau khi được tạo ra đều phải gọi hàm `latch.await()` để rơi vào trạng thái chờ. Khi luồng chính gọi `latch.countDown()`, rào chắn bị phá vỡ, toàn bộ hàng trăm thread sẽ đồng loạt xuất phát cùng một micro-giây, tạo ra race condition thực sự để kiểm thử độ an toàn của các cơ chế lock.
> 
> **Câu hỏi 2: Hãy giải thích cách hoạt động của cơ chế `OPTIMISTIC_LOCK` trong dự án. Tại sao cơ chế này không dùng từ khóa lock nào của Java hay OS nhưng vẫn đảm bảo 0% âm kho và cho throughput cực cao?**
> - *Trả lời cốt lõi:* Cơ chế này hoạt động dựa trên logic so sánh phiên bản (version-based). Khi đọc dữ liệu sản phẩm, thread sẽ đọc kèm giá trị `version` (ví dụ: version = 5). Khi thực hiện cập nhật soldQty xuống file CSV, thread sẽ kiểm tra xem version hiện tại trong file còn bằng 5 hay không. Nếu bằng 5 -> ghi đè dữ liệu mới và tự động tăng version lên 6. Nếu version trong file đã thay đổi (do thread khác nhanh chân ghi trước) -> thread hiện tại hủy bỏ giao dịch, tiến hành đọc lại dữ liệu mới và thử lại (retry) tối đa 3 lần. Vì không block thread nào, hệ thống tận dụng tối đa sức mạnh đa luồng song song nên throughput rất cao mà vẫn tuyệt đối chặn đứng âm kho.

---

## 3. Lộ Trình Triển Khai Kỹ Thuật (10 Tuần)

Lộ trình tập trung vào việc hoàn thiện logic nghiệp vụ, các cơ chế khóa và simulator:

- **Tuần 1 - Tuần 2: Thiết kế Kiến trúc OOP & Schema CSV**
  - **Cả nhóm:** Thiết kế UML Class Diagram, phân rã các lớp thực thể kế thừa `BaseEntity`.
  - **TV1:** Viết cấu trúc lớp cha `BaseEntity`, khung generic `CsvRepository<T>` và sinh dữ liệu mẫu.
  - *Verify:* Chạy file `DataGenerator` tạo thành công dữ liệu mẫu ≥ 10.000 dòng chuẩn định dạng CSV.

- **Tuần 3 - Tuần 4: Cài đặt Repository & Đăng ký/Đăng nhập (Checkpoint Kỹ thuật 1)**
  - **TV1:** Hoàn thiện generic `CsvRepository<T>` (sử dụng Reflection để parse dữ liệu).
  - **TV2:** Hoàn thiện `CustomerRepository`, `ProductRepository`, `FlashSaleEventRepository`.
  - **TV3:** Hoàn thiện `FlashItemRepository`, `OrderRepository`, `OrderDetailRepository`.
  - *Verify:* Đọc/ghi và truy vấn đơn luồng dữ liệu từ các Repository chạy trơn tru, không lỗi corrupt file.

- **Tuần 5 - Tuần 6: Hoàn thiện Nghiệp vụ Đặt hàng đơn luồng (baseline)**
  - **TV3:** Viết logic check tồn kho, check giới hạn đặt hàng tối đa 2 sản phẩm, thiết lập hệ thống custom exception.
  - **TV1:** Cài đặt luồng đặt hàng đơn luồng không dùng khóa (`NO_LOCK` flow).
  - **TV2:** Cài đặt luồng xác thực đăng nhập/đăng ký người dùng và xem danh sách flash sale.
  - *Verify:* Chạy thử đặt hàng thủ công đơn luồng trên console thành công, tạo đủ đơn hàng và chi tiết đơn hàng hợp lệ.

- **Tuần 7: Bùng nổ Concurrency & Locking (Checkpoint Kỹ thuật 2 - Cực Kỳ Quan Trọng)**
  - **TV2:** Thực hiện code cơ chế khóa vật lý `FILE_LOCK` dùng Java NIO `FileChannel`.
  - **TV3:** Thực hiện code cơ chế khóa luồng `SYNCHRONIZED` mức khối (Block-level synchronized).
  - **TV4:** Thực hiện code cơ chế khóa lạc quan `OPTIMISTIC_LOCK` (quản lý `version` và retry loop).
  - *Verify:* Kiểm thử thủ công đa luồng (2-5 luồng chạy song song) hoạt động đúng, chặn đứng âm kho ở cả 3 cơ chế lock.

- **Tuần 8: Triển khai Simulator Service đa luồng**
  - **TV4:** Viết động cơ giả lập đa luồng dùng `ExecutorService` (Thread Pool) và rào chắn `CountDownLatch`.
  - **Cả nhóm:** Phối hợp chạy thử Simulator với các cơ chế khóa tương ứng để bắt bug đồng bộ.
  - *Verify:* Chạy simulator với 100 - 500 threads, in bảng so sánh kết quả ASCII trực quan.

- **Tuần 9: Thực nghiệm quy mô lớn & Hoàn thành Báo cáo thực nghiệm**
  - **TV4:** Chạy simulator 1000 threads x 4 cơ chế x 3 lần lặp để lấy trung bình số liệu TPS và tỉ lệ âm kho.
  - **Cả nhóm:** Mỗi người tự vẽ flowchart cho cơ chế khóa và nghiệp vụ mình phụ trách. Hoàn thiện Slide và Báo cáo Word.
  - *Verify:* Báo cáo và Slide đầy đủ bảng số liệu so sánh và biểu đồ throughput trực quan.

- **Tuần 10: AI Reflection, Đóng gói & Nộp bài**
  - **Cả nhóm:** Hoàn thành file AI Log cá nhân, viết báo cáo AI Reflection (≥ 500 từ/người).
  - **TV1:** Viết README chi tiết (lệnh compile, lệnh chạy generator, lệnh chạy simulator). Đóng gói ZIP.
  - *Verify:* Build thử dự án từ file ZIP trên máy độc lập thành công 100% đéo lỗi compile.
