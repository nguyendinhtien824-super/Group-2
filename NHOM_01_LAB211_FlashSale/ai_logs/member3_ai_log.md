# AI Log & Reflection - Thành viên 3 (Mẫu Tham Khảo)

**Họ và tên:** [Điền tên của bạn ở đây]  
**MSSV:** [Điền MSSV của bạn ở đây]  
**Nhiệm vụ phân công:** Phụ trách thực thể `FlashSaleItem`, `OrderDetail`, logic ràng buộc đặt hàng, kiểm soát giới hạn mua và cơ chế khóa `SYNCHRONIZED`.

---

## 1. Nhật ký tương tác (Raw Conversations)

### Tình huống 1: Lỗi biên dịch hệ thống sau khi chỉnh sửa Service lớp dưới
- **Prompt:**
  > "Tôi đã sửa `FlashSaleServiceImpl.java` để hỗ trợ kiểm tra giới hạn mua hàng tối đa 2 sản phẩm cho mỗi khách hàng trong sự kiện Flash Sale bằng cách lấy dữ liệu từ `orders.csv` và `order_details.csv`. Vì vậy hàm `bookItem` ở lớp Service hiện tại yêu cầu thêm tham số `customerId`. Nhưng sau khi sửa xong, toàn bộ ứng dụng Java bị lỗi compile nghiêm trọng. Hãy phân tích và đề xuất phương án sửa lỗi đồng bộ trên toàn hệ thống."
- **AI Output:**
  > AI phân tích lỗi và chỉ ra rằng các lớp gọi Service này gồm `OrderController.java`, giao diện Console `MainView.java`, lớp khởi chạy chính `FlashSaleApplication.java` và tệp unit test `FlashSaleConsoleAppTest.java` vẫn đang gọi hàm `bookItem` với signature cũ (thiếu `customerId`). AI đã đề xuất kế hoạch:
  > 1. Sửa `OrderController` nhận và truyền `customerId`.
  > 2. Sửa `MainView` lưu giữ trạng thái đăng nhập `loggedInCustomer` để lấy `customerId` truyền đi, đồng thời chặn người dùng đặt hàng nếu chưa đăng nhập.
  > 3. Khởi tạo `OrderRepository` và `OrderDetailRepository` trong `FlashSaleApplication` để truyền vào constructor của `FlashSaleServiceImpl`.
  > 4. Đồng bộ hóa toàn bộ Unit Test và bổ sung test case kiểm tra giới hạn mua.
- **Lỗi phát hiện:** 
  Khi thực hiện chạy thử nghiệm biên dịch bằng lệnh đề xuất `rtk mvn clean test`, terminal trả về lỗi: `rtk : The term 'rtk' is not recognized as the name of a cmdlet...` do môi trường Windows không cài đặt wrapper này.
- **Cách fix thực tế:**
  Bỏ tiền tố `rtk` và chạy trực tiếp lệnh native `mvn clean test` trên PowerShell để biên dịch dự án.

---

## 2. AI Reflection (Đánh giá cá nhân - ~650 từ)

### a. Đánh giá chất lượng hỗ trợ của AI
Trong suốt quá trình thực hiện đồ án LAB211, công cụ AI đã thể hiện vai trò là một trợ lý lập trình rất hiệu quả, đặc biệt trong việc sinh mã nguồn khung (boilerplate code), thiết lập cấu trúc lớp kế thừa từ `BaseEntity` và hỗ trợ viết các test case cơ bản một cách nhanh chóng. AI giúp tiết kiệm khoảng 50% thời gian gõ code thông thường.

Tuy nhiên, sai sót lớn nhất của AI nằm ở tính toàn vẹn của hệ thống khi thực hiện các thay đổi quy mô lớn. Khi thay đổi signature của một phương thức ở tầng sâu như Service hoặc Repository để đáp ứng một nghiệp vụ cụ thể (trong trường hợp này là thêm `customerId` để kiểm soát giới hạn mua 2 sản phẩm theo yêu cầu của PDF), AI thường chỉ tập trung sửa đổi file đó và bỏ qua tầm ảnh hưởng domino lên các tầng bên trên (Controller, View, Test). Điều này trực tiếp gây ra lỗi compile toàn hệ thống, đòi hỏi tôi phải chủ động triệt tiêu các lỗi này bằng cách rà soát thủ công hoặc yêu cầu AI phân tích tổng thể sơ đồ phụ thuộc.

### b. Hạn chế của AI đối với Concurrent Programming
Lập trình đa luồng và đồng bộ hóa là một trong những phần khó nhất của đồ án. AI có xu hướng viết code đồng bộ hóa mang tính "lý thuyết suông", chỉ hoạt động tốt trong bộ nhớ in-memory. Khi kết hợp với I/O đĩa cứng (thao tác trực tiếp trên các file CSV như `orders.csv` và `order_details.csv`), AI rất dễ bỏ qua các trường hợp xung đột ghi file (file corrupt) hoặc các vấn đề nghẽn cổ chai hiệu năng.

Ví dụ, khi viết logic khóa đồng bộ `SYNCHRONIZED`, ban đầu AI đề xuất đồng bộ hóa mức phương thức (Method-level). Tôi đã phát hiện ra rằng việc này sẽ khóa toàn bộ Repository, khiến Throughput (TPS) giảm thê thảm vì bất kỳ khách hàng nào mua bất kỳ sản phẩm nào cũng phải xếp hàng tuần tự. Tôi phải yêu cầu AI tối ưu hóa bằng cách thu hẹp phạm vi lock xuống mức khối (Block-level synchronized) và khóa trên ID cụ thể của từng sản phẩm Flash Sale. Bài học ở đây là không thể tin tưởng hoàn toàn vào giải pháp ban đầu của AI về Concurrency mà bắt buộc phải chạy simulator để đo số liệu TPS thực tế.

### c. Bài học rút ra
1. **Kỹ năng đặt câu hỏi (Prompt Engineering)**: Thay vì đưa ra các prompt chung chung như "hãy code chức năng đặt hàng", tôi đã học được cách chia nhỏ vấn đề thành các yêu cầu mang tính kỹ thuật sâu, ví dụ: "Viết logic kiểm tra chéo lịch sử đơn hàng từ file CSV để giới hạn số lượng mua tối đa 2 đơn vị/khách hàng".
2. **Kỹ năng kiểm chứng (Verify)**: Mọi dòng code AI sinh ra đều phải được xác minh thông qua chạy thử nghiệm tự động bằng JUnit và chạy simulator đa luồng thực tế bằng `CountDownLatch` để đo hiệu năng và kiểm soát âm kho.
3. **Mức độ phụ thuộc**: AI chỉ là một công cụ hỗ trợ tăng tốc viết code. Bản thân sinh viên phải nắm vững kiến trúc MVC, cơ chế hoạt động của Thread, Lock và cách tổ chức OOP để định hướng và dẫn dắt AI viết đúng kiến trúc hệ thống, tránh biến ứng dụng thành một đống code rối rắm (prototype slop) không thể bảo trì.
