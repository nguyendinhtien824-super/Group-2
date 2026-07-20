# CSV Schema Documentation

Tài liệu này mô tả đúng các file CSV mà console app đang đọc/ghi. Tất cả file nằm trong thư mục `data/` và được xử lý bằng UTF-8.

## `products.csv`

Header: `productId,name,brand,category,price,stock,description,version`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `productId` | String | Mã sản phẩm, ví dụ `P-00001` |
| `name` | String | Tên sản phẩm |
| `brand` | String | Thương hiệu |
| `category` | String | Danh mục |
| `price` | int | Giá gốc |
| `stock` | int | Tồn kho gốc |
| `description` | String | Mô tả |
| `version` | int | Version dùng cho optimistic update Product |

## `customers.csv`

Header: `customerId,name,email,phone,address,avatarUrl,tier,status,password,walletBalance`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `customerId` | String | Mã khách hàng |
| `name` | String | Họ tên |
| `email` | String | Email định danh đăng nhập |
| `phone` | String | Số điện thoại |
| `address` | String | Địa chỉ |
| `avatarUrl` | String | Link ảnh đại diện, có thể rỗng |
| `tier` | String | Hạng thành viên (STANDARD, SILVER, GOLD, DIAMOND) |
| `status` | String | Trạng thái tài khoản (ACTIVE, BANNED) |
| `password` | String | Argon2id PHC hash; không lưu mật khẩu thô |
| `walletBalance` | double | Số dư ví hiện tại |

## `vouchers.csv`

Header: `voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `voucherId` | String | Mã voucher |
| `code` | String | Mã giảm giá nhập vào |
| `type` | String | Loại giảm giá (PERCENTAGE, FIXED) |
| `value` | int | Giá trị giảm |
| `maxDiscount` | int | Số tiền giảm tối đa |
| `minOrderAmount` | int | Đơn hàng tối thiểu để dùng |
| `remainingUses` | int | Số lượt dùng còn lại |

## `flash_events.csv`

Header: `eventId,name,startTime,endTime,status,unlockTime`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `eventId` | String | Mã sự kiện |
| `name` | String | Tên sự kiện |
| `startTime` | String | Thời gian bắt đầu |
| `endTime` | String | Thời gian kết thúc |
| `status` | String | Trạng thái sự kiện |
| `unlockTime` | String | Thời điểm mở khóa; có thể rỗng |

## `flash_items.csv`

Header: `itemId,productId,eventId,productName,originalPrice,salePrice,initialStock,soldQty,remainingStock,version`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `itemId` | String | Mã mặt hàng Flash Sale |
| `productId` | String | Mã sản phẩm gốc |
| `eventId` | String | Mã sự kiện Flash Sale |
| `productName` | String | Tên sản phẩm hiển thị |
| `originalPrice` | int | Giá gốc |
| `salePrice` | int | Giá Flash Sale |
| `initialStock` | int | Số lượng giới hạn của Flash Sale |
| `soldQty` | int | Số lượng đã bán |
| `remainingStock` | int | Tồn kho còn lại, phải đồng bộ với `initialStock - soldQty` |
| `version` | int | Version cho Optimistic Locking |

## `orders.csv`

Header: `orderId,customerId,customerName,orderDate,totalAmount,status,eventId`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `orderId` | String | Mã đơn hàng |
| `customerId` | String | Mã khách hàng |
| `customerName` | String | Tên khách hàng tại thời điểm tạo đơn |
| `orderDate` | String | Ngày tạo đơn |
| `totalAmount` | int | Tổng tiền |
| `status` | String | Trạng thái đơn |
| `eventId` | String | Khóa ngoại tới Flash Sale Event |

## `order_details.csv`

Header: `detailId,orderId,productId,quantity,unitPrice,subtotal`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `detailId` | String | Mã dòng chi tiết |
| `orderId` | String | Mã đơn hàng |
| `productId` | String | Mã sản phẩm |
| `quantity` | int | Số lượng |
| `unitPrice` | int | Đơn giá |
| `subtotal` | int | Thành tiền |

## `transactions.csv`

Header: `transactionId,orderId,customerId,itemId,quantity,status,message,timestamp`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `transactionId` | String | Mã log giao dịch |
| `orderId` | String | Mã đơn thật hoặc mã tổng hợp của simulator |
| `customerId` | String | Mã khách hàng thật hoặc mã tổng hợp của simulator |
| `itemId` | String | Mã Flash Sale item |
| `quantity` | int | Số lượng đặt |
| `status` | String | Trạng thái lifecycle đơn (`PENDING`, `APPROVED`, `SUCCESS`, `CANCELLED`) hoặc kết quả simulator (`SUCCESS`, `FAILED`) |
| `message` | String | Ghi chú cơ chế xử lý |
| `timestamp` | long | Thời điểm ghi log |

## Quan hệ khóa ngoại của dữ liệu seed

- `flash_items.productId → products.productId`
- `flash_items.eventId → flash_events.eventId`
- `orders.customerId → customers.customerId`
- `orders.eventId → flash_events.eventId`
- `order_details.orderId → orders.orderId`
- `order_details.productId → products.productId`

Data Generator ghi từng file qua tệp tạm và atomic move. Riêng `transactions.csv` được giữ nguyên nếu đã tồn tại để không mất lịch sử giao dịch.

## Cơ chế lock trong simulator

`LockType` chỉ gồm 4 giá trị đúng theo đề bài:

- `NO_LOCK`
- `FILE_LOCK`
- `SYNCHRONIZED`
- `OPTIMISTIC_LOCK`
