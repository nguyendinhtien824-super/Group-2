# Báo cáo Chi tiết: Tất cả các Use Cases - Hệ thống Mô phỏng Shopee Flash Sale

---

## I. TỔNG QUAN

Hệ thống gồm **3 actor chính** với **19 use cases** được chia thành **3 nhóm chức năng**:

|         Nhóm         | Actor      | Số Use Cases | Chức năng                    |
|----------------------|------------|--------------|------------------------------|
|     **Khách hàng**   | Customer   | 10           | Mua hàng + Theo dõi đơn hàng |
| **Quản trị viên**    | Admin      | 10           | Quản lý hệ thống             |
| **Người nghiên cứu** | Researcher | 6            | Giả lập & Benchmark          |

---

## II. NHÓM 1: NGHIỆP VỤ KHÁCH HÀNG (CUSTOMER)

### A. PHẦN MUA HÀNG (7 Use Cases)

#### **UC1: Đăng ký tài khoản mới (Validate trùng lặp)**
- **Mô tả**: Khách hàng tạo tài khoản mới với validation kiểm tra trùng lặp
- **Actor**: Khách hàng chưa có tài khoản
- **Pre-condition**: Khách hàng chưa đăng ký
- **Các bước**:
  1. Nhập thông tin: ID, Tên, Email, Mật khẩu
  2. Hệ thống kiểm tra ID đã tồn tại?
  3. Nếu hợp lệ → Tạo tài khoản → Mặc định Tier = SILVER
  4. Nếu trùng lặp → Thông báo lỗi
- **Post-condition**: Tài khoản được tạo, khách hàng có thể đăng nhập
- **Data saved**: Customer record in CSV

---

#### **UC2: Đăng nhập hệ thống (Check trạng thái Banned)**
- **Mô tả**: Khách hàng đăng nhập với kiểm tra trạng thái tài khoản
- **Actor**: Khách hàng đã đăng ký
- **Pre-condition**: Khách hàng phải đã tạo tài khoản
- **Các bước**:
  1. Nhập ID khách hàng
  2. Hệ thống kiểm tra tài khoản có tồn tại?
  3. Kiểm tra trạng thái Banned?
  4. Nếu bị khóa → Từ chối đăng nhập
  5. Nếu hợp lệ → Đăng nhập thành công
- **Post-condition**: Khách hàng được ghi nhận đã đăng nhập
- **Error handling**: Tài khoản không tồn tại, tài khoản bị khóa

---

#### **UC3: Xem danh sách sự kiện Flash Sale (Active/Pending)**
- **Mô tả**: Khách hàng xem tất cả sự kiện Flash Sale đang hoạt động
- **Actor**: Khách hàng (đã/chưa đăng nhập)
- **Pre-condition**: Có ít nhất 1 sự kiện Flash Sale trong hệ thống
- **Các bước**:
  1. Hệ thống lấy danh sách sự kiện Flash Sale
  2. Lọc sự kiện có trạng thái ACTIVE hoặc PENDING
  3. Hiển thị: ID sự kiện, Tên, Mô tả, Thời gian, Trạng thái
- **Post-condition**: Danh sách sự kiện được hiển thị
- **Data returned**: FlashSaleEvent list

---

#### **UC4: Xem chi tiết sản phẩm Flash Sale & Tồn kho**
- **Mô tả**: Khách hàng xem thông tin chi tiết sản phẩm trong sự kiện
- **Actor**: Khách hàng
- **Pre-condition**: Khách hàng phải chọn một sự kiện Flash Sale
- **Các bước**:
  1. Chọn sự kiện từ danh sách
  2. Hệ thống lấy danh sách sản phẩm của sự kiện
  3. Hiển thị: ID sản phẩm, Tên, Giá gốc, Giá Flash, Tồn kho, Số đã bán
- **Post-condition**: Chi tiết sản phẩm được hiển thị
- **Data returned**: FlashItem list with stock information

---

#### **UC5: Đặt hàng sản phẩm Flash Sale**
- **Mô tả**: Khách hàng đặt hàng sản phẩm Flash Sale với cơ chế khóa Optimistic Lock
- **Actor**: Khách hàng đã đăng nhập
- **Pre-condition**: 
  - Khách hàng phải đăng nhập
  - Sản phẩm phải có tồn kho > 0
- **Các bước**:
  1. Chọn sản phẩm và nhập số lượng
  2. Hệ thống kiểm tra tồn kho
  3. Áp dụng Optimistic Lock (kiểm tra phiên bản)
  4. Nếu tồn kho đủ → Trừ tồn kho, tạo Order
  5. Nếu conflict → Thông báo và yêu cầu thử lại
- **Post-condition**: Đơn hàng được tạo, tồn kho được giảm
- **Exception handling**: OptimisticLockException, InsufficientStockException
- **Data updated**: Order, OrderDetail, FlashItem stock

---

#### **UC6: Chọn và áp dụng mã Voucher giảm giá**
- **Mô tả**: Khách hàng nhập mã voucher để được giảm giá
- **Actor**: Khách hàng đang đặt hàng
- **Pre-condition**: Khách hàng phải có mã voucher hợp lệ
- **Các bước**:
  1. Khách hàng nhập mã voucher
  2. Hệ thống kiểm tra voucher có tồn tại?
  3. Kiểm tra voucher còn hạn sử dụng?
  4. Tính toán số tiền giảm (%)
  5. Cập nhật tổng tiền = Tổng - Giảm
- **Post-condition**: Giảm giá được áp dụng vào đơn hàng
- **Error handling**: Voucher không tồn tại, hết hạn, hết lượt

---

#### **UC7: Áp dụng giảm giá tự động VIP Tier**
- **Mô tả**: Hệ thống tự động giảm giá theo VIP Tier của khách hàng
- **Actor**: Hệ thống (tự động)
- **Tier levels**:
  - SILVER: 0% giảm
  - GOLD: 5% giảm
  - PLATINUM: 10% giảm
  - DIAMOND: 15% giảm
- **Các bước**:
  1. Lấy VIP Tier của khách hàng từ Order
  2. Kiểm tra discount % tương ứng
  3. Tính toán: Giảm = Tổng × Tier discount / 100
  4. Cập nhật tổng tiền cuối cùng
- **Post-condition**: Tự động giảm giá được áp dụng
- **Priority**: Tier discount chồng với Voucher discount

---

### B. PHẦN THEO DÕI ĐƠN HÀNG (3 Use Cases)

#### **UC8: Xem danh sách tất cả đơn hàng của tôi**
- **Mô tả**: Khách hàng xem tất cả đơn hàng của mình
- **Actor**: Khách hàng đã đăng nhập
- **Pre-condition**: Khách hàng phải đã đăng nhập
- **Các bước**:
  1. Hệ thống lấy tất cả Order theo customerId
  2. Sắp xếp theo ngày mới nhất trước
  3. Hiển thị: ID đơn, Ngày, Tổng tiền, Trạng thái
- **Post-condition**: Danh sách đơn hàng được hiển thị
- **Data returned**: Order list sorted by date DESC

---

#### **UC9: Xem chi tiết sản phẩm trong một đơn hàng**
- **Mô tả**: Khách hàng xem chi tiết sản phẩm của một đơn hàng cụ thể
- **Actor**: Khách hàng đã đăng nhập
- **Pre-condition**: Khách hàng phải chọn một đơn hàng
- **Các bước**:
  1. Chọn đơn hàng từ danh sách
  2. Hệ thống lấy OrderDetail theo orderId
  3. Hiển thị: Tên sản phẩm, Số lượng, Giá, Tổng
- **Post-condition**: Chi tiết sản phẩm được hiển thị
- **Data returned**: OrderDetail list

---

#### **UC10: Xem lịch sử giao dịch của tôi**
- **Mô tả**: Khách hàng xem tất cả giao dịch thanh toán của mình
- **Actor**: Khách hàng đã đăng nhập
- **Pre-condition**: Khách hàng phải đã đăng nhập
- **Các bước**:
  1. Hệ thống lấy tất cả OrderTransaction theo customerId
  2. Sắp xếp theo ngày mới nhất trước
  3. Hiển thị: ID giao dịch, Thời gian, Số tiền, Trạng thái
- **Post-condition**: Lịch sử giao dịch được hiển thị
- **Data returned**: OrderTransaction list sorted by date DESC

---

## III. NHÓM 2: NGHIỆP VỤ QUẢN TRỊ (ADMIN)

#### **UC11: Tự động sinh dữ liệu mẫu (Mock Data)**
- **Mô tả**: Admin tạo tất cả dữ liệu CSV mẫu cho hệ thống
- **Actor**: Admin/Developer
- **Các bước**:
  1. Chọn tùy chọn "Tạo dữ liệu CSV"
  2. Hệ thống tạo: customers.csv, products.csv, flash_items.csv, vouchers.csv, orders.csv, order_details.csv, transactions.csv
  3. Ghi dữ liệu vào file
  4. Hiển thị số lượng records được tạo
- **Post-condition**: Tất cả CSV files được tạo
- **Data files created**: 8 CSV files in data/ folder

---

#### **UC12: Quản lý thông tin khách hàng (CRUD)**
- **Mô tả**: Admin xem, thêm, sửa, xóa thông tin khách hàng
- **Actor**: Admin
- **Operations**:
  - **Create**: Thêm khách hàng mới
  - **Read**: Xem thông tin khách hàng
  - **Update**: Cập nhật thông tin (Tên, Email, Tier)
  - **Delete**: Xóa khách hàng khỏi hệ thống
- **Data fields**: ID, Name, Email, Tier, Banned status
- **Post-condition**: Dữ liệu khách hàng được quản lý

---

#### **UC13: Khóa tài khoản khách hàng (Ban Customer)**
- **Mô tả**: Admin khóa tài khoản khách hàng vi phạm
- **Actor**: Admin
- **Pre-condition**: Khách hàng phải tồn tại trong hệ thống
- **Các bước**:
  1. Chọn khách hàng cần khóa
  2. Admin chọn "Ban Customer"
  3. Hệ thống cập nhật Banned = true
  4. Khách hàng không thể đăng nhập
- **Post-condition**: Tài khoản khách hàng bị khóa
- **Verification**: Customer.banned = true

---

#### **UC14: Mở khóa tài khoản khách hàng (Unban Customer)**
- **Mô tả**: Admin mở khóa tài khoản khách hàng đã bị ban
- **Actor**: Admin
- **Pre-condition**: Khách hàng phải bị ban
- **Các bước**:
  1. Chọn khách hàng cần mở khóa
  2. Admin chọn "Unban Customer"
  3. Hệ thống cập nhật Banned = false
  4. Khách hàng có thể đăng nhập lại
- **Post-condition**: Tài khoản khách hàng được kích hoạt lại

---

#### **UC15: Tạo mới sự kiện Flash Sale**
- **Mô tả**: Admin tạo sự kiện Flash Sale mới
- **Actor**: Admin
- **Các bước**:
  1. Nhập thông tin sự kiện: Tên, Mô tả, Ngày bắt đầu, Ngày kết thúc
  2. Hệ thống tạo FlashSaleEvent với trạng thái PENDING
  3. Lưu vào CSV
- **Post-condition**: Sự kiện Flash Sale được tạo
- **Data saved**: FlashSaleEvent record

---

#### **UC16: Cập nhật trạng thái sự kiện Flash Sale**
- **Mô tả**: Admin thay đổi trạng thái sự kiện (PENDING → ACTIVE → ENDED)
- **Actor**: Admin
- **Status flow**: PENDING → ACTIVE → ENDED
- **Các bước**:
  1. Chọn sự kiện
  2. Admin chọn trạng thái mới
  3. Hệ thống cập nhật status
- **Post-condition**: Trạng thái sự kiện được cập nhật

---

#### **UC17: Thêm sản phẩm vào sự kiện Flash Sale**
- **Mô tả**: Admin thêm sản phẩm vào sự kiện Flash Sale
- **Actor**: Admin
- **Các bước**:
  1. Chọn sự kiện
  2. Chọn sản phẩm và nhập: Giá Flash, Tồn kho
  3. Hệ thống tạo FlashItem record
  4. Lưu vào CSV
- **Post-condition**: Sản phẩm được thêm vào sự kiện
- **Data saved**: FlashItem record

---

#### **UC18: Cập nhật sản phẩm trong sự kiện Flash Sale**
- **Mô tả**: Admin cập nhật thông tin sản phẩm (Giá, Tồn kho, Trạng thái)
- **Actor**: Admin
- **Updatable fields**: flashPrice, stock, status
- **Các bước**:
  1. Chọn sản phẩm trong sự kiện
  2. Admin cập nhật thông tin
  3. Hệ thống lưu thay đổi
- **Post-condition**: Thông tin sản phẩm được cập nhật

---

#### **UC19: Xem báo cáo doanh thu chi tiết theo sự kiện**
- **Mô tả**: Admin xem tổng doanh thu, số đơn hàng, số sản phẩm bán của mỗi sự kiện
- **Actor**: Admin
- **Metrics**:
  - Tổng doanh thu (Revenue)
  - Số đơn hàng (Order count)
  - Số sản phẩm bán (Items sold)
  - Tỷ suất chuyển đổi (Conversion rate)
- **Các bước**:
  1. Admin chọn "Xem báo cáo doanh thu"
  2. Hệ thống tính toán cho mỗi sự kiện
  3. Hiển thị báo cáo chi tiết
- **Post-condition**: Báo cáo được hiển thị

---

#### **UC20: Thống kê hiệu quả sử dụng Voucher**
- **Mô tả**: Admin xem số lượng voucher được sử dụng, doanh thu từ voucher
- **Actor**: Admin
- **Metrics**:
  - Số lượng voucher được phát hành
  - Số lượng voucher được sử dụng
  - Tỷ lệ sử dụng (%)
  - Tổng tiền giảm
- **Các bước**:
  1. Admin chọn "Thống kê Voucher"
  2. Hệ thống tính toán thống kê
  3. Hiển thị báo cáo
- **Post-condition**: Thống kê Voucher được hiển thị

---

## IV. NHÓM 3: NGHIỆP VỤ NGHIÊN CỨU & GIẢ LẬP (RESEARCHER)

#### **UC21: Cấu hình tham số giả lập (Threads, Stock, Loop)**
- **Mô tả**: Researcher cấu hình các tham số cho giả lập
- **Actor**: Researcher
- **Configurable parameters**:
  - **Number of threads**: Số thread đặt hàng đồng thời (default: 10)
  - **Stock**: Số lượng sản phẩm tồn kho (default: 100)
  - **Loop count**: Số lần mỗi thread đặt hàng (default: 10)
- **Các bước**:
  1. Researcher chọn "Cấu hình tham số"
  2. Nhập các giá trị tham số
  3. Hệ thống lưu cấu hình
- **Post-condition**: Tham số được cấu hình cho giả lập kế tiếp

---

#### **UC22: Chạy giả lập đặt hàng không khóa (NO_LOCK)**
- **Mô tả**: Giả lập đặt hàng đồng thời mà không có cơ chế khóa
- **Actor**: Researcher
- **Concurrency mechanism**: NONE
- **Các bước**:
  1. Chọn cơ chế NO_LOCK
  2. Nhập số thread và stock
  3. Hệ thống khởi động thread pool
  4. Các thread cùng lúc đặt hàng
  5. Ghi lại kết quả (orders placed, conflicts, final stock)
- **Post-condition**: Giả lập hoàn thành, kết quả được ghi lại
- **Expected result**: Race condition xảy ra (final stock < 0 hoặc > expected)

---

#### **UC23: Chạy giả lập đặt hàng khóa vật lý CSV (FILE_LOCK)**
- **Mô tả**: Giả lập đặt hàng với khóa file CSV
- **Actor**: Researcher
- **Locking mechanism**: File-based lock (FileLock trên CSV file)
- **Các bước**:
  1. Chọn cơ chế FILE_LOCK
  2. Hệ thống tạo FileLock trên CSV file
  3. Các thread phải acquire lock trước khi read/write
  4. Ghi lại kết quả
- **Post-condition**: Giả lập hoàn thành
- **Expected result**: Các thread chạy tuần tự, final stock chính xác

---

#### **UC24: Chạy giả lập đặt hàng khóa bộ nhớ (SYNCHRONIZED)**
- **Mô tả**: Giả lập đặt hàng với synchronized keyword
- **Actor**: Researcher
- **Locking mechanism**: Synchronized block in-memory
- **Các bước**:
  1. Chọn cơ chế SYNCHRONIZED
  2. Các method read/write stock được synchronized
  3. Chỉ 1 thread có thể access stock tại một lúc
  4. Ghi lại kết quả
- **Post-condition**: Giả lập hoàn thành
- **Expected result**: Final stock chính xác, nhưng performance thấp

---

#### **UC25: Chạy giả lập đặt hàng khóa phiên bản (OPTIMISTIC_LOCK)**
- **Mô tả**: Giả lập đặt hàng với Optimistic Lock (version checking)
- **Actor**: Researcher
- **Locking mechanism**: Version column on FlashItem
- **Các bước**:
  1. Chọn cơ chế OPTIMISTIC_LOCK
  2. Các thread read version trước update
  3. Khi update, kiểm tra version có match không
  4. Nếu conflict → retry, nếu success → tăng version
  5. Ghi lại: successful orders, retries, conflicts
- **Post-condition**: Giả lập hoàn thành
- **Expected result**: Final stock chính xác, performance tốt, ít retries

---

#### **UC26: Chạy benchmark so sánh hiệu năng 4 cơ chế**
- **Mô tả**: Researcher chạy tất cả 4 cơ chế với cùng tham số và so sánh hiệu năng
- **Actor**: Researcher
- **Comparison metrics**:
  - **Execution time** (ms)
  - **Throughput** (orders/second)
  - **Lock contention** (conflicts/retries)
  - **Final stock accuracy** (correct/incorrect)
  - **Resource usage** (CPU, Memory)
- **Các bước**:
  1. Researcher chọn "Chạy benchmark"
  2. Hệ thống chạy 4 cơ chế liên tiếp với cùng tham số
  3. Mỗi cơ chế chạy 3 lần → lấy trung bình
  4. So sánh kết quả
  5. Hiển thị bảng so sánh
- **Post-condition**: Kết quả benchmark được hiển thị
- **Output example**:
  ```
  Mechanism      | Time(ms) | Throughput | Conflicts | Accuracy
  ───────────────┼──────────┼───────────┼──────────┼──────────
  NO_LOCK        | 500      | 200       | 45       | FAIL
  FILE_LOCK      | 2000     | 50        | 0        | PASS
  SYNCHRONIZED   | 1800     | 55        | 0        | PASS
  OPTIMISTIC     | 800      | 125       | 8        | PASS
  ```

---

## V. BIỂU ĐỒ CẤP ĐỘ USE CASE

```
                     Hệ thống Flash Sale
                    ┌──────────────────┐
                    │  19 Use Cases    │
                    │  3 Nhóm Chức năng │
                    └──────────────────┘
                          │
                ┌─────────┼─────────┐
                │         │         │
         Khách hàng    Quản trị   Nghiên cứu
         (10 cases)    (10 cases)  (6 cases)
             │            │           │
        ┌────┴────┐        │           │
        │          │        │           │
      Mua hàng   Theo dõi  Admin      Research
      (7 UC)    (3 UC)    (10 UC)     (6 UC)
```

---

## VI. MỘT SỐ USE CASE QUAN TRỌNG

### A. Quy trình Đặt hàng của Khách hàng
```
Customer
  ├─ UC2: Đăng nhập
  ├─ UC3: Xem sự kiện Flash Sale
  ├─ UC4: Xem chi tiết sản phẩm
  ├─ UC5: Đặt hàng
  │   ├─ Kiểm tra tồn kho
  │   ├─ Áp dụng Optimistic Lock
  │   └─ Tạo Order
  ├─ UC6: Áp dụng voucher (tùy chọn)
  ├─ UC7: Áp dụng VIP Tier (tự động)
  └─ UC8,9,10: Theo dõi đơn hàng
```

### B. Quy trình Quản trị của Admin
```
Admin
  ├─ UC11: Tạo mock data
  ├─ UC15: Tạo Flash Sale event
  ├─ UC17: Thêm sản phẩm
  ├─ UC16: Kích hoạt event
  ├─ UC19: Xem báo cáo doanh thu
  └─ UC20: Xem thống kê voucher
```

### C. Quy trình Giả lập của Researcher
```
Researcher
  ├─ UC21: Cấu hình tham số
  ├─ UC22-25: Chạy 4 cơ chế
  │   ├─ NO_LOCK
  │   ├─ FILE_LOCK
  │   ├─ SYNCHRONIZED
  │   └─ OPTIMISTIC_LOCK
  └─ UC26: Chạy benchmark so sánh
```

---

## VII. LIÊN KẾT GIỮA CÁC USE CASE

| Use Case | Điều kiện trước | Sử dụng dữ liệu từ | Tạo dữ liệu cho |
|----------|-----------------|-------------------|-----------------|
| UC2 Login | UC1 registered | Customer CSV | Session |
| UC3 View Events | System ready | FlashSaleEvent CSV | User selection |
| UC4 View Items | UC3 selected | FlashItem CSV | UC5 selected items |
| UC5 Order | UC2 logged in | FlashItem, Stock | UC8,9,10 display |
| UC6 Voucher | UC5 ordering | Voucher CSV | Order discount |
| UC7 Tier | UC5 ordering | Customer tier | Order total |
| UC8,9,10 Tracking | UC2 logged in | Order CSV | Display report |

---

## VIII. TÓM LẠI

| Khía cạnh | Chi tiết |
|----------|----------|
| **Tổng Use Cases** | 19 |
| **Actor** | 3 (Customer, Admin, Researcher) |
| **Nhóm chức năng** | 3 (Khách hàng, Quản trị, Nghiên cứu) |
| **Main focus** | Đặt hàng đồng thời + So sánh cơ chế khóa |
| **Technology** | CSV-based, Thread-based simulation, Lock mechanisms |
| **Complexity** | Trung bình - Cao |

---

**Ngày báo cáo:** 25/05/2026  
**Trạng thái:** ✅ Tất cả use cases được triển khai
