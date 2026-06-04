# AI Log & Reflection - Thành viên 4

**Họ và tên:** Trần Văn Phúc  
**MSSV:** QE200141  
**Nhiệm vụ phân công:** Thiết kế động cơ giả lập đa luồng (`SimulatorService`), quản lý ghi nhận lịch sử giao dịch mô phỏng (`OrderTransaction`, `SimulationResult`), và triển khai thuật toán khóa lạc quan (`OPTIMISTIC_LOCK`).

---

## 1. Nhật ký tương tác (Raw Conversations)

### Tình huống 1: Thiết kế động cơ giả lập đặt hàng đồng thời để đo hiệu năng (TPS)
* **Problem/Context:** Cần giả lập hàng trăm khách hàng cùng lúc nhấn nút mua sản phẩm Flash Sale tại cùng một micro-giây để kiểm thử độ an toàn của các cơ chế khóa và đo hiệu năng hệ thống (TPS).
* **Prompt to AI (NGUYÊN VĂN):**
  > "Tôi muốn viết một Simulator bằng Java giả lập khoảng 500 threads đặt hàng đồng thời một sản phẩm Flash Sale để đo throughput (TPS) và tỷ lệ âm kho. Hãy hướng dẫn tôi viết SimulatorService sử dụng ExecutorService."
* **AI Response (Summary):** AI đề xuất tạo một `ExecutorService` với Thread Pool và dùng vòng lặp `for` thông thường để gọi `executor.submit()` khởi chạy các thread mua hàng.
* **Critical Thinking (AI đúng/sai ở đâu?):** AI đã **sai về mặt logic (Oversimplification)**. Việc gọi `executor.submit()` trong vòng lặp chạy tuần tự. Do luồng khởi chạy thread có độ trễ, các thread được tạo trước sẽ chạy xong và kết thúc trước cả khi thread thứ 500 được khởi tạo. Kết quả là các luồng chạy nối đuôi nhau chứ không hề xảy ra tranh chấp (race condition) thực sự tại cùng một thời điểm micro-giây.
* **Contextualization:** AI không hiểu bối cảnh kiểm thử của đồ án LAB211 là cần tạo ra xung đột tài nguyên cực đại tại cùng một thời điểm để chứng minh hiện tượng âm kho của cơ chế `NO_LOCK`.
* **Creative Synthesis (Cách fix thực tế):** Tôi đã bác bỏ code của AI và đề xuất áp dụng **`CountDownLatch`** để làm rào chắn đồng bộ. Tôi tạo 3 Latch: `readyLatch(numThreads)` để đợi tất cả các thread sẵn sàng, `startLatch(1)` làm còi hiệu kích nổ đồng thời, và `doneLatch(numThreads)` để luồng chính đợi tất cả hoàn thành trước khi đo thời gian chạy. Khi luồng chính gọi `startLatch.countDown()`, toàn bộ 500 threads mới đồng loạt xuất phát, tạo ra race condition thực thụ.
* **Decision Ownership:** Chọn cơ chế phối hợp `CountDownLatch` + `ExecutorService` vì đây là giải pháp chuẩn công nghiệp để giả lập tải đồng thời cao (concurrency load testing) trong Java.
* **Evidence:** File [SimulatorService.java](file:///c:/Users/phucv/Downloads/shopeeconsole/NHOM_01_LAB211_FlashSale/src/service/SimulatorService.java#L61-L76) dòng 61 đến 76 sử dụng `readyLatch`, `startLatch`, và `doneLatch` để điều khiển luồng.

---

### Tình huống 2: Lỗi âm kho khi giả lập cơ chế Khóa lạc quan (Optimistic Lock) in-memory
* **Problem/Context:** Viết logic trừ kho theo cơ chế Khóa lạc quan dựa trên thuộc tính `version` của thực thể `FlashItem` trong bộ nhớ mà không sử dụng database thực tế.
* **Prompt to AI (NGUYÊN VĂN):**
  > "Hãy viết cho tôi hàm purchaseOptimistic trong Java. Hàm này nhận vào đối tượng FlashItem (có trường soldQty, initialStock, và version). Áp dụng thuật toán Optimistic Locking: đọc version hiện tại, nếu còn đủ hàng thì update soldQty và tăng version lên 1. Nếu version đã bị thread khác thay đổi thì báo lỗi và thực hiện retry."
* **AI Response (Summary):** AI sinh code check-and-act thông thường:
  ```java
  int currentVersion = item.getVersion();
  if (remaining >= qty) {
      item.setSoldQty(item.getSoldQty() + qty);
      item.setVersion(currentVersion + 1);
      return true;
  }
  ```
* **Critical Thinking (AI đúng/sai ở đâu?):** **HALLUCINATION / LOGIC ERROR DETECTED!** AI đề xuất một thuật toán khóa lạc quan hoàn toàn sai về mặt bản chất đa luồng. Việc đọc version và cập nhật dữ liệu trên đối tượng Java in-memory thông thường là không nguyên tử (non-atomic). Khi chạy Simulator với 500 threads, hệ thống vẫn bị âm kho trầm trọng (-18 sản phẩm) do nhiều thread cùng đọc được một giá trị `currentVersion` và cùng thực hiện ghi đè.
* **Contextualization:** AI chỉ áp dụng máy móc lý thuyết khóa lạc quan của hệ quản trị cơ sở dữ liệu (nơi có câu lệnh `UPDATE ... WHERE version = x` do DB engine xử lý nguyên tử), nhưng trong Java in-memory, lập trình viên phải tự quản lý tính nguyên tử này.
* **Creative Synthesis (Cách fix thực tế):** Tôi đã sửa lại hàm bằng cách bọc phần kiểm tra version và cập nhật trạng thái bên trong một khối `synchronized (item)` siêu nhỏ (critical section cực hẹp) chỉ chứa logic so khớp version và tăng tiến version nhằm giả lập phép so sánh CAS (Compare-And-Swap) nguyên tử:
  ```java
  synchronized (item) {
      if (item.getVersion() == currentVersion) {
          item.setSoldQty(currentSold + quantity);
          item.setVersion(currentVersion + 1);
          return true;
      }
  }
  ```
* **Decision Ownership:** Quyết định sử dụng khối synchronized siêu nhỏ để đảm bảo tính nguyên tử cho thao tác ghi đè version trong bộ nhớ, vừa đạt mục tiêu chặn đứng 100% âm kho, vừa giữ throughput cực cao vì luồng bị lock chỉ trong vài nano-giây chứ không bị block toàn bộ I/O ghi file CSV.
* **Evidence:** File [SimulatorService.java](file:///c:/Users/phucv/Downloads/shopeeconsole/NHOM_01_LAB211_FlashSale/src/service/SimulatorService.java#L240-L260) dòng 240 đến 260.

---

## 2. AI Reflection (Đánh giá cá nhân - ~650 từ)

### a. Đánh giá chất lượng hỗ trợ của AI
Trong suốt quá trình triển khai đồ án giả lập Flash Sale, công cụ AI đã hỗ trợ tôi rất tốt trong việc sinh mã nguồn cấu trúc (boilerplate code). Cụ thể là việc tạo cấu trúc dữ liệu cho lớp lưu trữ kết quả `SimulationResult.java` và các lớp thực thể lưu lịch sử giao dịch `OrderTransaction.java`. AI cũng viết rất nhanh các hàm phụ trợ như tự động phân chia tỉ lệ thành viên VIP (Diamond, Gold, Silver) bằng thuật toán phân bổ ngẫu nhiên dựa trên xác suất tích lũy. Nhờ đó, tôi tiết kiệm được nhiều thời gian thiết kế các cấu phần phụ để tập trung vào kiến trúc cốt lõi.

Tuy nhiên, AI bộc lộ điểm yếu rất lớn khi giải quyết các vấn đề đồng bộ hóa phức tạp. Khi được yêu cầu viết mã nguồn xử lý đa luồng hoặc ngăn chặn race condition, AI thường đưa ra các giải pháp mang tính lý thuyết in-memory đơn giản, thiếu kiểm chứng thực tế và bỏ qua các yếu tố về hiệu năng. Nhiều đoạn code do AI đề xuất ban đầu biên dịch thành công nhưng khi đưa vào chạy thử nghiệm với tải cao (500-1000 threads) lập tức xảy ra lỗi vỡ layout ghi file hoặc âm kho nghiêm trọng.

### b. Hạn chế của AI đối với Concurrent Programming
Lập trình đa luồng đòi hỏi tư duy cực kỳ chặt chẽ về mặt vật lý hệ thống (CPU, bộ nhớ đệm, I/O đĩa cứng), đây chính là điểm mù lớn nhất của AI. AI không thể lường trước được tốc độ đọc/ghi đĩa cứng rất chậm của file CSV so với tốc độ xử lý của CPU. Khi AI đề xuất các cơ chế khóa, nó thường đưa ra giải pháp "khóa thô" như dùng `synchronized` bao quanh toàn bộ phương thức nghiệp vụ đặt hàng (bao gồm cả việc đọc file và parse dữ liệu). Điều này vô tình triệt tiêu sức mạnh xử lý song song của CPU, biến hệ thống đa luồng thành đơn luồng tuần tự, làm sụt giảm throughput (TPS) xuống mức thê thảm, vi phạm nghiêm trọng yêu cầu "không giảm quá 30% throughput" của đề bài.

Hơn nữa, AI rất hay gặp lỗi ảo tưởng (hallucination) về tính nguyên tử của các biến trong Java. Nó cho rằng việc sử dụng các biến thông thường và kiểm tra điều kiện `if` đơn giản đã đủ để triển khai Khóa lạc quan trong môi trường đa luồng in-memory. Nếu tôi không chủ động viết Simulator để kiểm chứng bằng số liệu thực tế, dự án chắc chắn sẽ thất bại khi nghiệm thu với giáo viên do không phát hiện ra lỗi âm kho tiềm ẩn này.

### c. Bài học rút ra
1. **Kỹ năng Prompt Engineering:** Tôi nhận ra rằng không bao giờ được đưa ra các yêu cầu chung chung như "code cho tôi cơ chế khóa". Tôi phải cung cấp đầy đủ ngữ cảnh kỹ thuật: *"Viết cơ chế Khóa lạc quan in-memory trong Java sử dụng nguyên lý CAS, đảm bảo vùng đồng bộ nhỏ nhất có thể để không block luồng xử lý chính."*
2. **Kỹ năng kiểm chứng (Verify):** Đây là bài học quan trọng nhất. Code chạy không lỗi compile chưa chắc đã là code đúng. Đối với các bài toán Concurrency, mọi cơ chế đồng bộ đều phải được đưa vào bộ giả lập Simulator dưới áp lực tải cao (Stress test) để thu thập số liệu thực tế (TPS, số lần xung đột, tỷ lệ âm kho). Số liệu thực nghiệm là bằng chứng duy nhất chứng minh code hoạt động đúng.
3. **Mức độ phụ thuộc vào AI:** AI chỉ là một trợ lý viết code nhanh, tuyệt đối không được giao phó tư duy kiến trúc cho nó. Học sinh bắt buộc phải nắm vững kiến trúc đa luồng, cơ chế hoạt động của Thread Pool, rào chắn đồng bộ `CountDownLatch` và các loại Lock trong Java để làm người dẫn dắt và kiểm soát chất lượng code của AI, tránh để AI tạo ra các đoạn code rác, chắp vá (prototype slop) mất kiểm soát.
