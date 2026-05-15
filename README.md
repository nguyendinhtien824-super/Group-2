# Flash Sale System - LAB211

## 📌 Giới thiệu
Hệ thống giả lập Flash Sale với quy mô dữ liệu lớn, xử lý tranh chấp (Race Condition) và mô phỏng giao dịch thực tế.

## 📁 Cấu trúc dự án
- `src/`: Mã nguồn Java (MVC Pattern).
- `data/`: Dữ liệu CSV (Products, Customers, Orders...).
- `docs/`: Tài liệu hướng dẫn, Class Diagram và Flowcharts.
- `ai_logs/`: Nhật ký sử dụng AI của các thành viên.

## 🚀 Hướng dẫn chạy
### 1. Yêu cầu hệ thống
- Java JDK 17+
- Git

### 2. Cài đặt và Biên dịch
```bash
javac -d bin src/**/*.java
```

### 3. Chạy DataGenerator
```bash
java -cp bin controller.DataGenerator
```

### 4. Chạy chương trình chính
```bash
java -cp bin Main
```

### 5. Chạy Simulator (Mô phỏng Flash Sale)
```bash
java -cp bin controller.Simulator
```

---
**Nhóm:** XX
**Môn:** LAB211
