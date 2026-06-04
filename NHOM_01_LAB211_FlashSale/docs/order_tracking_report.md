
# Báo cáo: Tính năng "Theo dõi đơn hàng" (Order Tracking Feature)

## I. Tổng quan

Tính năng **"Theo dõi đơn hàng"** cho phép khách hàng (sau khi đăng nhập) xem và quản lý thông tin về các đơn hàng đã đặt, bao gồm danh sách đơn hàng, chi tiết sản phẩm, và lịch sử giao dịch.

---

## II. Các Use Case trong phần "Theo dõi đơn hàng"

### 1. **Xem danh sách tất cả đơn hàng của tôi** (UC_ViewOrders)
- **Mô tả**: Khách hàng xem tất cả các đơn hàng đã đặt, sắp xếp theo thứ tự mới nhất trước
- **Thông tin hiển thị**: ID đơn hàng, ngày đặt hàng, tổng tiền, trạng thái
- **Actor**: Khách hàng đã đăng nhập
- **Pre-condition**: Khách hàng phải đã đăng nhập vào hệ thống
- **Post-condition**: Danh sách đơn hàng được hiển thị trên màn hình

### 2. **Xem chi tiết sản phẩm trong một đơn hàng** (UC_ViewOrderDetail)
- **Mô tả**: Khách hàng xem danh sách sản phẩm cụ thể trong một đơn hàng đã chọn
- **Thông tin hiển thị**: Tên sản phẩm, số lượng, giá, tổng giá trị từng sản phẩm
- **Actor**: Khách hàng đã đăng nhập
- **Pre-condition**: Khách hàng phải chọn một đơn hàng từ danh sách
- **Post-condition**: Chi tiết sản phẩm được hiển thị

### 3. **Xem lịch sử giao dịch của tôi** (UC_ViewTransactions)
- **Mô tả**: Khách hàng xem tất cả các giao dịch đã thực hiện, bao gồm thời gian, số tiền, trạng thái
- **Thông tin hiển thị**: ID giao dịch, thời gian, số tiền, trạng thái thanh toán (thành công/thất bại)
- **Actor**: Khách hàng đã đăng nhập
- **Pre-condition**: Khách hàng phải đã đăng nhập
- **Post-condition**: Lịch sử giao dịch được hiển thị

---

## III. Kiến trúc và Triển khai

### 3.1 Các thành phần liên quan

| Thành phần | Vị trí | Mô tả |
|-----------|--------|-------|
| **OrderTrackingController** | `src/controller/OrderTrackingController.java` | Xử lý logic nghiệp vụ cho phần theo dõi đơn hàng |
| **OrderTrackingView** | `src/view/OrderTrackingView.java` | Giao diện console hiển thị menu theo dõi |
| **Order Model** | `src/model/Order.java` | Đối tượng đơn hàng |
| **OrderDetail Model** | `src/model/OrderDetail.java` | Chi tiết sản phẩm trong đơn hàng |
| **OrderTransaction Model** | `src/model/OrderTransaction.java` | Giao dịch thanh toán |
| **Repositories** | `src/repository/` | OrderRepository, OrderDetailRepository, OrderTransactionRepository |

### 3.2 Quy trình thực hiện

```
┌─ Khách hàng đăng nhập
│
├─ Menu chính hiển thị option: "11. Theo dõi đơn hàng của tôi"
│
├─ Gọi MainView.trackMyOrders()
│  ├─ Kiểm tra: Khách hàng đã đăng nhập?
│  └─ Nếu CÓ → Mở OrderTrackingView
│     └─ Nếu KHÔNG → Yêu cầu đăng nhập trước
│
├─ OrderTrackingView.display(customerId)
│  ├─ Hiển thị menu con:
│  │  ├─ 1. Danh sách tất cả đơn hàng
│  │  ├─ 2. Xem chi tiết một đơn hàng
│  │  ├─ 3. Lịch sử giao dịch
│  │  └─ 0. Quay lại
│  └─ Xử lý lựa chọn:
│     ├─ Nếu chọn 1 → showMyOrders(customerId)
│     ├─ Nếu chọn 2 → showOrderDetails(customerId)
│     └─ Nếu chọn 3 → showMyTransactions(customerId)
│
└─ Hiển thị kết quả hoặc quay lại menu
```

### 3.3 Các phương thức chính trong OrderTrackingController

```java
// Lấy danh sách đơn hàng của khách hàng
public List<Order> getOrdersByCustomer(String customerId)

// Lấy chi tiết một đơn hàng
public Order getOrderById(String orderId)

// Lấy danh sách sản phẩm trong một đơn hàng
public List<OrderDetail> getOrderDetails(String orderId)

// Lấy lịch sử giao dịch
public List<OrderTransaction> getTransactionsByCustomer(String customerId)
```

---

## IV. Quy trình khách hàng sử dụng

### Kịch bản 1: Xem danh sách đơn hàng
1. Khách hàng đăng nhập → Menu chính → Chọn option 11
2. OrderTrackingView hiển thị menu → Chọn option 1
3. Hệ thống gọi `orderTrackingController.getOrdersByCustomer(customerId)`
4. Danh sách đơn hàng được hiển thị (ID, ngày, tổng tiền)

### Kịch bản 2: Xem chi tiết sản phẩm trong đơn hàng
1. Từ menu theo dõi → Chọn option 2
2. Nhập ID đơn hàng muốn xem
3. Hệ thống gọi `orderTrackingController.getOrderDetails(orderId)`
4. Chi tiết sản phẩm được hiển thị (tên, số lượng, giá)

### Kịch bản 3: Xem lịch sử giao dịch
1. Từ menu theo dõi → Chọn option 3
2. Hệ thống gọi `orderTrackingController.getTransactionsByCustomer(customerId)`
3. Lịch sử giao dịch được hiển thị (ID giao dịch, thời gian, số tiền, trạng thái)

---

## V. Dữ liệu được sử dụng

### Entities liên quan:
- **Order**: ID đơn hàng, ID khách hàng, tổng tiền, ngày tạo, trạng thái
- **OrderDetail**: ID chi tiết, ID đơn hàng, ID sản phẩm, số lượng, giá
- **OrderTransaction**: ID giao dịch, ID đơn hàng, số tiền, thời gian, trạng thái

### Repositories:
- **OrderRepository**: Truy vấn thông tin đơn hàng (`findByCustomerId`, `findById`)
- **OrderDetailRepository**: Truy vấn chi tiết đơn hàng (`findByOrderId`)
- **OrderTransactionRepository**: Truy vấn lịch sử giao dịch (`findByCustomerId`)

---

## VI. Lợi ích của tính năng

✅ **Cho khách hàng:**
- Theo dõi trạng thái đơn hàng real-time
- Xem chi tiết sản phẩm đã mua
- Kiểm tra lịch sử thanh toán

✅ **Cho hệ thống:**
- Giảm thiểu câu hỏi hỗ trợ khách hàng
- Tăng trải nghiệm người dùng
- Cung cấp dữ liệu cho phân tích hành vi mua

---

## VII. Các file liên quan

| File | Loại | Mô tả |
|------|------|-------|
| `FlashSaleApplication.java` | Main | Khởi tạo OrderTrackingController |
| `MainView.java` | View | Gọi OrderTrackingView từ menu chính |
| `OrderTrackingView.java` | View | Giao diện menu theo dõi |
| `OrderTrackingController.java` | Controller | Logic nghiệp vụ |
| `use_case_diagram.puml` | Diagram | Sơ đồ use case (đã cập nhật) |

---

## VIII. Kết luận

Tính năng **"Theo dõi đơn hàng"** là một phần quan trọng của hệ thống, cho phép khách hàng quản lý và theo dõi các đơn hàng của mình một cách dễ dàng. Tính năng này được thiết kế với kiến trúc **MVC** rõ ràng, tách biệt logic và giao diện, dễ bảo trì và mở rộng.

---

**Ngày báo cáo:** 25/05/2026  
**Trạng thái:** ✅ Hoàn thành triển khai
