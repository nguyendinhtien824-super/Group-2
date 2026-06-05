# AI Log & Reflection - Thành viên 1

**Họ và tên:** Đỗ Bá Quang Hưng  
**MSSV:** QE190032  
**Nhiệm vụ phân công:** Thiết kế kiến trúc Entity trừu tượng (`BaseEntity`), lớp Repository dùng Generic (`CsvRepository<T>`), xây dựng logic sinh dữ liệu giả lập hệ thống (`DataGeneratorService`), cấu hình và kiểm nghiệm luồng mua hàng đa luồng không dùng khóa (`NO_LOCK` flow) để chứng minh hiện tượng race condition gây âm kho. Phụ trách vẽ Sơ đồ luồng sinh dữ liệu & Sơ đồ UML Class Diagram tổng thể cho dự án.

---

## 1. Nhật ký tương tác (Raw Conversations)

### Tình huống 1: Thiết kế cấu trúc Entity trừu tượng và Generic Repository cho CSV
* **Problem/Context:** Thiết kế kiến trúc lưu trữ dữ liệu dưới dạng file CSV cho ứng dụng Flash Sale, tối ưu hóa code để tránh lặp lại cho từng thực thể.
* **Prompt to AI (NGUYÊN VĂN):**
  > "Tôi cần thiết kế hệ thống lưu trữ dữ liệu bằng file CSV cho ứng dụng Java OOP mô phỏng Flash Sale. Hãy phân tích cấu trúc dữ liệu và đề xuất cách tổ chức các class Entity kế thừa từ một class cha chung để giảm thiểu lặp code, đồng thời viết CsvRepository<T> generic dùng Reflection tự động parse và serialize tệp."
* **AI Response (Summary):** AI đề xuất tạo lớp trừu tượng `BaseEntity` với hai phương thức trừu tượng `toCsvLine()` và `fromCsvLine(String csv)`. Ở lớp `CsvRepository<T>`, AI đề xuất dùng Java Reflection (`Field[] fields = entity.getClass().getDeclaredFields()`) để tự động lặp qua các thuộc tính của lớp con `T` nhằm parse dữ liệu và chuyển đổi đối tượng thành dòng CSV tự động.
* **Critical Thinking (AI đúng/sai ở đâu?):** AI đã **sai về mặt logic và hiệu năng (Oversimplification / Performance)**.
  1. *Lỗi biên dịch (Compile Error):* Hàm static `fromCsvLine` ở class cha `BaseEntity` không thể khai báo là `abstract` vì trong Java, static methods không thể được ghi đè (overridden) ở các lớp con.
  2. *Lỗi hiệu năng (Performance):* Việc sử dụng Java Reflection để quét các trường lớp thực thể động khi đọc ghi file quy mô lớn (hơn 10.000 dòng dữ liệu như products, customers) làm chậm tốc độ I/O hệ thống cực kỳ nghiêm trọng khi chạy simulator.
* **Contextualization:** AI chỉ đề xuất giải pháp thuần lý thuyết chạy in-memory mà không lường trước các ràng buộc về cú pháp Java (static abstract) và tốc độ đọc ghi tệp tin vật lý trong môi trường tải cực đoan.
* **Creative Synthesis (Cách fix thực tế):**
  1. Loại bỏ phương thức static `fromCsvLine` ở lớp trừu tượng `BaseEntity`. Chỉ định nghĩa các hàm trừu tượng instance-level như `getId()` và `toCsvLine()`.
  2. Thiết lập phương thức trừu tượng `protected abstract T parseLine(String line)` trong `CsvRepository<T>`. Từng repository con cụ thể (như `ProductRepository`, `FlashEventRepository`) kế thừa và tự viết code parse thủ công tối ưu cho đối tượng của mình.
  3. Trong `CsvRepository`, gọi trực tiếp `entity.toCsvLine()` thay vì sử dụng cơ chế phản chiếu Reflection động nhằm tối ưu tối đa tốc độ ghi file.
* **Decision Ownership:** Quyết định chuyển dịch logic parse từ `BaseEntity` sang tầng `CsvRepository` và loại bỏ Reflection để đảm bảo tính thực thi cao và tuân thủ chặt chẽ cú pháp hướng đối tượng của Java.
* **Evidence:** File BaseEntity.java dòng 7-18, và CsvRepository.java dòng 42.

---

### Tình huống 2: Lỗi corrupted file khi ghi CSV đồng thời (Multi-threaded Write)
* **Problem/Context:** Khi chạy simulator với 100 thread ghi đơn hàng vào file CSV cùng một lúc bằng Java BufferedWriter thông thường thì bị lỗi corrupted dữ liệu (các dòng bị ghi đè, ghi đứt quãng, trùng lặp hoặc mất dòng).
* **Prompt to AI (NGUYÊN VĂN):**
  > "Tôi chạy thử nghiệm simulator với 100 thread ghi đơn hàng vào file CSV cùng một lúc bằng Java BufferedWriter thông thường thì bị lỗi corrupted dữ liệu (các dòng bị ghi đè, ghi đứt quãng, trùng lặp hoặc mất dòng). Hãy giải thích nguyên nhân và đề xuất giải pháp."
* **AI Response (Summary):** AI giải thích do luồng ghi file bằng các lớp I/O thông thường không thread-safe khi truy cập song song cùng một tài nguyên vật lý, đề xuất bọc từ khóa `synchronized` trên toàn bộ phương thức ghi file ở Repository hoặc tầng Service.
* **Critical Thinking (AI đúng/sai ở đâu?):** AI đề xuất giải pháp **chưa tối ưu và gây nghẽn cổ chai (Oversimplification / Performance)**. Việc sử dụng từ khóa `synchronized` ở cấp phương thức (Method-level lock) khiến hiệu năng hệ thống giảm sút nghiêm trọng (throughput TPS giảm thê thảm) vì nó biến toàn bộ việc đọc và ghi file thành một tiến trình tuần tự đơn luồng. Cả luồng đọc dữ liệu (truy vấn) cũng bị block bởi luồng ghi.
* **Contextualization:** Bối cảnh dự án Flash Sale đòi hỏi hệ thống phải hỗ trợ truy vấn đọc tồn kho song song liên tục với tốc độ cao, chỉ khóa khi ghi đè dữ liệu kho.
* **Creative Synthesis (Cách fix thực tế):**
  1. Thay vì sử dụng từ khóa `synchronized` thô sơ, tôi triển khai `ReentrantReadWriteLock` trong lớp cha `CsvRepository`.
  2. Bọc phương thức đọc dữ liệu `findAll()` bằng khóa đọc `rwLock.readLock().lock()`. Điều này cho phép hàng trăm luồng đọc file đồng thời cực nhanh mà không bị nghẽn.
  3. Bọc các phương thức ghi/cập nhật/xóa file như `save()`, `update()`, `deleteById()` bằng khóa ghi `rwLock.writeLock().lock()`. Khóa ghi đảm bảo tính độc chiếm tại một thời điểm ghi tệp vật lý, ngăn chặn hoàn toàn việc corrupt file.
* **Decision Ownership:** Sử dụng `ReentrantReadWriteLock` làm cơ chế điều khiển truy cập tài nguyên file CSV để cân bằng giữa an toàn dữ liệu và tối đa hóa hiệu năng hệ thống (TPS).
* **Evidence:** File CsvRepository.java dòng 18, 45, 73, 95, 113.
s
---

## 2. AI Reflection (Đánh giá cá nhân - ~680 từ)

### a. Đánh giá chất lượng hỗ trợ của AI
Trong quá trình thực hiện đồ án LAB211, AI đã thể hiện vai trò là một trợ lý đắc lực trong các tác vụ phát triển ban đầu. AI làm rất tốt ở khâu sinh mã nguồn cấu trúc (boilerplate code), giúp tôi nhanh chóng xây dựng các thực thể Java cơ bản, thiết kế cấu trúc thư mục MVC và viết mã nguồn ban đầu của `DataGeneratorService` để tạo ra hàng nghìn dòng dữ liệu mẫu chuẩn hóa CSV. Nhờ AI, tốc độ viết mã thô tăng lên rõ rệt, tiết kiệm được nhiều thời gian thiết lập hạ tầng.

Tuy nhiên, đối với các logic nghiệp vụ phức tạp liên quan đến tính toán và phân luồng, AI lộ rõ nhiều điểm yếu về sự thiếu tính toàn vẹn (holistic view) của hệ thống. Khi tôi yêu cầu AI viết các phương thức Generic, nó thường đề xuất các cú pháp lý thuyết (như static abstract method) không hợp lệ trong Java SE. AI cũng dễ rơi vào hiện tượng ảo tưởng (hallucination), cung cấp các package hoặc thư viện không tồn tại trong JDK tiêu chuẩn như `java.nio.file.csv`. Điều này đòi hỏi sinh viên phải có nền tảng lý thuyết vững chắc để nhận diện và loại bỏ mã nguồn sai sót.

### b. Hạn chế của AI đối với Concurrent Programming
Lập trình đa luồng (Concurrent Programming) và đồng bộ hóa là thách thức lớn nhất trong dự án này, và đây cũng là nơi AI bộc lộ nhiều hạn chế nghiêm trọng. AI có xu hướng áp dụng các giải pháp khóa đồng bộ quá thô sơ (như `synchronized` mức phương thức) hoặc đề xuất các đoạn code không an toàn (như ghi file APPEND không dùng khóa) với lời khẳng định hệ điều hành sẽ tự bảo vệ. 

Trong kịch bản mô phỏng Flash Sale đa luồng với tần suất truy cập cực cao, các lỗi race condition mang tính chất "Check-then-Act" là cực kỳ phổ biến. AI thường viết code kiểm tra tồn kho `if (stock > 0)` nằm ngoài khối đồng bộ, dẫn đến việc nhiều thread cùng vượt qua kiểm tra và ghi đè giá trị âm xuống file CSV (`NO_LOCK` flow). AI thiếu khả năng tư duy về sự trễ của phần cứng I/O vật lý. Việc đọc/ghi file CSV chậm hơn rất nhiều so với RAM, do đó các khoảng hở thời gian (race window) là rất lớn. Nếu chỉ tin tưởng vào các đoạn code do AI tự sinh mà không tự phân tích luồng và chạy thực nghiệm qua Simulator, hệ thống sẽ liên tục gặp lỗi âm kho và hỏng cấu trúc tệp dữ liệu.

### c. Bài học rút ra
1. **Kỹ năng đặt prompt (Prompt Engineering):** Tôi đã học được cách từ bỏ các câu lệnh chung chung mang tính phó mặc cho AI. Thay vào đó, tôi chia nhỏ bài toán thành các đặc tả kỹ thuật chi tiết: chỉ định rõ kiểu dữ liệu, các ràng buộc nghiệp vụ, cơ chế khóa cần sử dụng (ví dụ: dùng `ReentrantReadWriteLock` thay cho synchronized) và yêu cầu AI chú ý đến hiệu năng luồng đọc/ghi.
2. **Kỹ năng kiểm chứng (Verification):** Code do AI viết ra chỉ là một bản nháp thô. Tôi rút ra bài học sâu sắc là bắt buộc phải xây dựng các kịch bản kiểm thử tự động (JUnit) và chạy Simulator tải cao dưới rào cản `CountDownLatch` để đo lường các chỉ số thực tế (TPS, tỉ lệ đơn hàng thành công, tỉ lệ lỗi âm kho) trước khi tích hợp vào dự án. Số liệu benchmark thực tế không biết nói dối và là thước đo chính xác nhất cho chất lượng code.
3. **Mức độ phụ thuộc:** Việc học lập trình thực chất đòi hỏi tư duy phân tích độc lập. AI là một công cụ tăng tốc tuyệt vời, nhưng nếu sinh viên lười tư duy và chỉ sao chép mã nguồn của AI, họ sẽ hoàn toàn bất lực khi gặp lỗi biên dịch hệ thống hoặc lỗi tranh chấp tài nguyên phức tạp. Nắm vững kiến trúc OOP (Đa hình, Trừu tượng), cấu trúc dữ liệu và cơ chế hoạt động của Thread/Lock là điều kiện bắt buộc để làm chủ AI và bảo vệ đồ án thành công trước hội đồng giám khảo.
