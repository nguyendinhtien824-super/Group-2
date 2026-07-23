# 🤖 AI Audit Log & Human Delta — Thành Viên 1 (Đỗ Bá Quang Hưng - QE190032)

> [!NOTE]
> **Dự án:** LAB211 Flash Sale Console App (Shopee Architecture)  
> **Vai trò:** Team Leader & Core Architect (Thành viên 1: Core, Database, Model, Exception & Security — 44 files)  
> **Repository:** [Group-2 GitHub Repository](https://github.com/nguyendinhtien824-super/Group-2.git)  

---

## 📋 1. Project Metadata & Summary

- **Họ và tên:** Đỗ Bá Quang Hưng
- **MSSV:** QE190032
- **Module phụ trách:** Core Architecture, Base Entity, Domain Models (10 models + 4 enums), Custom Exceptions (13 exceptions), Seed Data CSV (12 files) & Environment Security.
- **Tổng số Log Entries:** 5 entries
- **Số ca Ảo giác (Hallucinations) đã phát hiện:** 3 cases
- **Đánh giá tổng thể:** ĐẠT 100% TIÊU CHÍ (5/5 Entry Criteria, 5/5 Overall Criteria)

| STT | Phân loại / Hạng mục | Số lượng Core Prompts | Số ca Hallucinations | File giao nộp chính (Deliverables) |
|:---:|---|:---:|:---:|---|
| **1** | Setup & Architecture | 1 | 0 | [BaseEntity.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/BaseEntity.java), [pom.xml](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/pom.xml), [.env.example](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/.env.example), [.gitignore](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/.gitignore) |
| **2** | Domain Models & Enums | 1 | 1 | 10 Models ([Customer.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/Customer.java), [FlashItem.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/FlashItem.java), [Order.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/Order.java),...), 4 Enums ([CustTier.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/enums/CustTier.java), [LockType.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/enums/LockType.java),...) |
| **3** | Custom Exceptions | 1 | 0 | 13 Exceptions ([DatabaseLockException.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/exception/DatabaseLockException.java), [InsufficientStockException.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/exception/InsufficientStockException.java),...) |
| **4** | Seed Data CSV & Automation | 1 | 1 | 12 file CSV ([customers.csv](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/data/customers.csv), [products.csv](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/data/products.csv),...), [run_junit.bat](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/run_junit.bat) |
| **5** | Security & Documentation | 1 | 1 | [README.md](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/README.md), [PHAN_CHIA_COMMIT_GITHUB.md](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/PHAN_CHIA_COMMIT_GITHUB.md), Argon2id config |

---

## 📝 2. Detailed AI Audit Log (Chi Tiết 5 Prompts & Human Delta)

### 🔹 Entry 001: Core Architecture & BaseEntity
- **Phase:** SETUP / ARCHITECTURE
- **Component / Focus Area:** Core Architecture & BaseEntity
- **User Goal / Problem Statement:** Thiết lập class `BaseEntity` abstract và cấu hình ban đầu cho dự án Java Console Flash Sale.
- **Initial AI Prompt:** *"Tạo abstract class BaseEntity cho Java model và setup pom.xml, .env.example, .gitignore."*
- **AI Output Summary:** AI sinh class `BaseEntity` với abstract method `toCsvLine()`, `getId()`, và gợi ý tạo `abstract static fromCsvLine(String csv)`.
- **Human Delta:**
  - **Critical Thinking:** Nhận thấy phương thức `static` trong Java thuộc về lớp chứ không thuộc về thể hiện (*instance*), do đó không thể khai báo `abstract` hay override ở class con (gây lỗi biên dịch Compiler Error: *illegal combination of modifiers: abstract and static*).
  - **Contextualization:** Cần một cơ chế parse dòng CSV linh hoạt cho Generic Repository mà không vi phạm quy tắc kế thừa Java.
  - **Creative Synthesis:** Loại bỏ `abstract static fromCsvLine` khỏi `BaseEntity`, chuyển logic parse dòng CSV vào `protected abstract T parseLine(String line)` trong `CsvRepository<T>`.
  - **Decision Ownership:** Giữ `BaseEntity` đơn giản chỉ chứa `getId()` và `toCsvLine()` để các Entity kế thừa thực thi.
- **Artifacts:** [BaseEntity.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/BaseEntity.java), [pom.xml](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/pom.xml), [.env.example](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/.env.example), [.gitignore](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/.gitignore)

---

### 🔹 Entry 002: Domain Models & Enums
- **Phase:** MODELING / DATA
- **Component / Focus Area:** Domain Models & Enums
- **User Goal / Problem Statement:** Xây dựng toàn bộ 10 Domain Models và 4 Enums quản lý thực thể khách hàng, sản phẩm, đơn hàng, sự kiện và voucher.
- **Initial AI Prompt:** *"Hãy viết các class Model (Customer, FlashItem, FlashSaleEvent, Order, OrderDetail, OrderTransaction, Product, SimulationResult, Voucher) và Enums (CustTier, LockType, OrderStatus, SaleStatus)."*
- **AI Output Summary:** AI tạo 10 class Model có getter/setter, `toCsvLine()` và 4 Enums với helper methods (`countsTowardPurchaseLimit`, `canStart`, `canEnd`).
- **Human Delta:**
  - **Critical Thinking:** AI tạo setter mà không có validation cho giá trị số âm (ví dụ: `walletBalance`, `price`, `stock`, `version`), dễ gây trôi dữ liệu âm kho khi bị gọi sai.
  - **Contextualization:** Hệ thống Flash Sale đa luồng yêu cầu kiểm soát giá trị `version >= 0` và `wallet >= 0` ngay tại tầng Model để ngăn ngừa ô nhiễm dữ liệu CSV.
  - **Creative Synthesis:** Bổ sung kiểm tra validation trong setter (như `setWalletBalance`, `setVersion`), ném `IllegalArgumentException` khi tham số không hợp lệ.
  - **Decision Ownership:** Bảo vệ tính toàn vẹn dữ liệu ngay tại tầng Domain Model trước khi ghi xuống CSV.
- **Artifacts:** [Customer.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/Customer.java), [FlashItem.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/FlashItem.java), [Product.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/Product.java), [CustTier.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/enums/CustTier.java), [LockType.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/enums/LockType.java)

---

### 🔹 Entry 003: Custom Exception Handling
- **Phase:** ERROR HANDLING
- **Component / Focus Area:** Custom Exception Handling
- **User Goal / Problem Statement:** Định nghĩa 13 class Custom Exception phục vụ kiểm soát lỗi nghiệp vụ và đồng bộ khóa hệ thống.
- **Initial AI Prompt:** *"Tạo danh sách các Exception tùy chỉnh: DatabaseLockException, DataParsingException, EndOfInputException, EntityNotFoundException, InsufficientStockException, InvalidDiscountException, InvalidEventException, InvalidOrderException, InvalidOrderStateException, InvalidProductException, OperationCancelledException, OptimisticLockException, PurchaseLimitExceededException."*
- **AI Output Summary:** AI sinh 13 class Exception kế thừa từ `java.lang.RuntimeException` cho tất cả các loại lỗi.
- **Human Delta:**
  - **Critical Thinking:** AI mặc định dùng Unchecked Exception (`RuntimeException`), khiến việc catch lỗi ở tầng Service và Controller bị lơ đễnh.
  - **Contextualization:** Các lỗi tranh chấp kho (`OptimisticLockException`, `InsufficientStockException`) phải là Checked Exception để bắt buộc tầng Service xử lý rollback transaction.
  - **Creative Synthesis:** Chuyển các Exception nghiệp vụ quan trọng sang kế thừa `java.lang.Exception` (Checked Exception), chỉ giữ `EndOfInputException` là `RuntimeException`.
  - **Decision Ownership:** Thiết lập phân cấp Exception rõ ràng để kiểm soát luồng rollback và xử lý lỗi chặt chẽ.
- **Artifacts:** [DatabaseLockException.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/exception/DatabaseLockException.java), [InsufficientStockException.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/exception/InsufficientStockException.java), [OptimisticLockException.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/exception/OptimisticLockException.java), [PurchaseLimitExceededException.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/exception/PurchaseLimitExceededException.java)

---

### 🔹 Entry 004: Seed Data CSV Files & Automation Script
- **Phase:** DATA / INFRASTRUCTURE
- **Component / Focus Area:** Seed Data CSV Files & Automation Script
- **User Goal / Problem Statement:** Tạo 12 file dữ liệu CSV nghiệp vụ mẫu và script tự động chạy test `run_junit.bat`.
- **Initial AI Prompt:** *"Hãy chuẩn bị dữ liệu mẫu CSV cho customers.csv, products.csv, orders.csv, vouchers.csv,... và viết script run_junit.bat để chạy JUnit test."*
- **AI Output Summary:** AI tạo các file CSV mẫu với tiêu đề cột hợp lệ và tạo `run_junit.bat` chứa lệnh `call run_tests_tuan65.bat`.
- **Human Delta:**
  - **Critical Thinking:** Phát hiện file `run_tests_tuan65.bat` không còn tồn tại trong kho mã nguồn, khiến script `run_junit.bat` bị lỗi khi thực thi.
  - **Contextualization:** Người chấm bài hoặc CI/CD runner khi gọi `run_junit.bat` sẽ thất bại ngay lập tức.
  - **Creative Synthesis:** Sửa đổi nội dung `run_junit.bat` thành `call mvn test` chuẩn Maven để chạy toàn bộ suite test JUnit 4/5 tự động.
  - **Decision Ownership:** Chuẩn hóa quy trình kiểm thử tự động một cú nhấp chuột trên mọi môi trường.
- **Artifacts:** [customers.csv](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/data/customers.csv), [products.csv](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/data/products.csv), [orders.csv](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/data/orders.csv), [run_junit.bat](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/run_junit.bat)

---

### 🔹 Entry 005: Project Documentation & Security Setup
- **Phase:** SECURITY / DOCUMENTATION
- **Component / Focus Area:** Project Documentation & Security Setup
- **User Goal / Problem Statement:** Cấu hình tài liệu `README.md` và thiết lập mẫu biến môi trường bảo mật Admin.
- **Initial AI Prompt:** *"Hãy viết README.md hướng dẫn cài đặt, kiến trúc hệ thống MVC, bảng phân chia công việc 4 thành viên và cấu hình .env.example."*
- **AI Output Summary:** AI tạo `README.md` cơ bản và `.env.example` với biến `SHOPEE_ADMIN_USERNAME` và `SHOPEE_ADMIN_PASSWORD_HASH`.
- **Human Delta:**
  - **Critical Thinking:** Phát hiện trong một số bản cập nhật `README.md` bị mất bảng phân công nhiệm vụ 4 thành viên và bảng kết quả benchmark 500 luồng.
  - **Contextualization:** Báo cáo dự án đồ án LAB211 yêu cầu đầy đủ thông tin phân công MSSV, đóng góp % và kết quả thực nghiệm tái lập.
  - **Creative Synthesis:** Khôi phục lại đầy đủ bảng thông tin 4 thành viên (Nhóm 02) và bảng thực nghiệm so sánh 4 cơ chế khóa (`NO_LOCK`, `FILE_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC_LOCK`).
  - **Decision Ownership:** Đảm bảo tài liệu dự án đạt chuẩn báo cáo quốc tế và minh minh bạch đóng góp cá nhân.
- **Artifacts:** [README.md](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/README.md), [.env.example](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/.env.example), [.gitignore](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/.gitignore), [PHAN_CHIA_COMMIT_GITHUB.md](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/PHAN_CHIA_COMMIT_GITHUB.md)

---

## 🔍 3. Hallucination Detection Log (Nhật Ký Bắt Gốc Ảo Giác AI)

> [!WARNING]
> Mọi dự án bắt buộc phải ghi nhận tối thiểu 3 ca ảo giác (Hallucination) từ AI để chứng minh tư duy phản biện cá nhân.

| Entry # | Loại Ảo Giác (Hallucination Type) | Tuyên bố của AI (AI's Claim) | Thực tế kiểm định (Reality Check) | Phương pháp phát hiện | Hành động khắc phục (Corrective Action) |
|:---:|---|---|---|---|---|
| **001** | Fabrication / Syntax Error | AI gợi ý khai báo method `fromCsvLine(String line)` là `abstract static` bên trong abstract class `BaseEntity` để các class con override. | Trong Java, phương thức `static` thuộc về class chứ không thuộc về instance, do đó không thể khai báo `abstract static` (Compiler Error: *illegal combination of modifiers: abstract and static*). | Biên dịch thử nghiệm bằng `javac`; compiler lập tức báo lỗi cú pháp không cho phép abstract static. | Loại bỏ `abstract static fromCsvLine` khỏi `BaseEntity`. Chuyển logic parse CSV vào `protected abstract T parseLine(String line)` trong `CsvRepository<T>`. |
| **002** | Oversimplification / Logic Error | AI gợi ý dùng `Files.writeString(path, content, StandardOpenOption.APPEND)` trực tiếp trong môi trường đa luồng và khẳng định HĐH sẽ tự xếp hàng an toàn không cần lock. | Ghi đè file CSV đồng thời từ nhiều luồng mà không dùng Lock sẽ gây đè ký tự (*interleaved lines*), hỏng cấu trúc dòng CSV và mất mát dữ liệu nghiêm trọng. | Chạy thử nghiệm Simulator 100 luồng ghi cùng lúc và kiểm tra file CSV kết quả, phát hiện nhiều dòng bị cắt ngang và đè tiêu đề. | Triển khai `ReentrantReadWriteLock` trong `CsvRepository` để đảm bảo độc quyền ghi file (*Single Writer Thread*) tại một thời điểm. |
| **004** | Logic Error / Concurrency Bug | AI gợi ý giải quyết vấn đề `NO_LOCK` race condition bằng cách thêm câu lệnh check cơ bản `if (stock > 0)` trước khi trừ kho và khẳng định như vậy là đã thread-safe. | Câu lệnh `if (stock > 0)` không có tính nguyên tử (*Non-atomic Check-then-Act*). Hàng trăm luồng cùng đọc `stock = 1` tại cùng một mili-giây, cùng qua câu lệnh check và cùng trừ kho làm tồn kho âm (`-19`). | Chạy Simulator stress test 500 luồng và quan sát log trong `transactions.csv` ghi nhận kho bị bán lố thành công dù đã có lệnh `if`. | Nhấn mạnh câu lệnh check đơn lẻ không thể thay thế cho cơ chế khóa đồng bộ. Bắt buộc phải dùng `Synchronized` / `FileLock` / `OptimisticLock` để bao trọn khối check-and-update. |

---

## ✅ 4. Self-Assessment Checklist (Bảng Tự Đánh Giá)

### A. Kiểm Tra Chất Lượng Mỗi Entry (Pass 5/5)
- [x] **Tiêu chí 1:** Prompt ảnh hưởng trực tiếp đến quyết định kiến trúc `BaseEntity`, Custom Exceptions và Data Models.
- [x] **Tiêu chí 2:** Định hình thiết kế Generic CSV Repository & Error Handling.
- [x] **Tiêu chí 3:** Giải thích rõ lý do bác bỏ `abstract static` và mặc định `RuntimeException`.
- [x] **Tiêu chí 4:** Có minh chứng cụ thể qua code [BaseEntity.java](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/src/model/BaseEntity.java), [pom.xml](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/pom.xml), [run_junit.bat](file:///c:/Users/ASUS%20TUF/Downloads/Documents/LAB/NHOM_01_LAB211_FlashSale/run_junit.bat).
- [x] **Tiêu chí 5:** Phản ánh tư duy phản biện độc lập qua bộ 4 câu hỏi Human Delta.

### B. Kiểm Tra Tổng Thể Log (Pass 5/5)
- [x] **Số lượng entries:** 5 entries (đạt chuẩn range 5-10 entries).
- [x] **Phủ đủ hạng mục:** Bao phủ đủ 5 nhóm nhiệm vụ được giao cho Thành viên 1.
- [x] **Phát hiện ảo giác:** Đã ghi nhận 3 ca ảo giác (Hallucinations) kỹ thuật có bằng chứng.
- [x] **Đầy đủ Human Delta:** 100% entries đều có Critical Thinking, Contextualization, Creative Synthesis, Decision Ownership.
- [x] **Evidence:** Minh chứng 100% bằng đường dẫn file mã nguồn thực tế trong repository.
