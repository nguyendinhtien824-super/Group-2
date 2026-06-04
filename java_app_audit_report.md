# Java App Audit Report

Status: **PASS**

## Scanned Inventory
- Root: `C:\Users\phucv\Downloads\shopeeconsole`
- App dir: `C:\Users\phucv\Downloads\shopeeconsole\NHOM_01_LAB211_FlashSale`
- Directories scanned: 38
- Files scanned: 143
- Empty directories: 2
- Empty files: 0
- Empty dirs: `NHOM_01_LAB211_FlashSale/target/generated-sources/annotations`, `NHOM_01_LAB211_FlashSale/target/generated-test-sources/test-annotations`

## Spec Checklist
- OK: source dirs - `src/model`
- OK: source dirs - `src/repository`
- OK: source dirs - `src/controller`
- OK: source dirs - `src/view`
- OK: source dirs - `src/service`
- OK: submission dirs - `data`
- OK: submission dirs - `docs`
- OK: submission dirs - `ai_logs`
- OK: docs - `README.md`
- OK: docs - `docs/report.docx`
- OK: docs - `docs/slide.pptx`
- OK: docs - `docs/class_diagram.png`
- OK: docs - `docs/flowcharts/order_flow.png`
- OK: docs - `docs/flowcharts/race_condition_flow.png`
- OK: docs - `docs/flowcharts/simulator_flow.png`
- OK: docs - `docs/flowcharts/datagen_flow.png`
- OK: csv - `data/products.csv`
- OK: csv - `data/customers.csv`
- OK: csv - `data/flash_events.csv`
- OK: csv - `data/flash_items.csv`
- OK: csv - `data/orders.csv`
- OK: csv - `data/order_details.csv`
- OK: csv - `data/transactions.csv`
- OK: ai logs - `ai_logs/member1_ai_log.md`
- OK: ai logs - `ai_logs/member2_ai_log.md`
- OK: ai logs - `ai_logs/member3_ai_log.md`
- OK: ai logs - `ai_logs/member4_ai_log.md`
- OK: code term - `CountDownLatch`
- OK: code term - `NO_LOCK`
- OK: code term - `FILE_LOCK`
- OK: code term - `SYNCHRONIZED`
- OK: code term - `OPTIMISTIC_LOCK`
- OK: code term - `CsvRepository`
- OK: code term - `FlashItemRepository`
- OK: code term - `DataGeneratorService`
- OK: code term - `SimulatorService`
- OK: stale term absent - `SpringApplication`
- OK: stale term absent - `spring-boot`
- OK: stale term absent - `org.springframework`
- OK: stale term absent - `jakarta.mail`
- OK: stale term absent - `resources/static`
- OK: stale term absent - `localhost:8080`
- OK: stale term absent - `PESSIMISTIC_LOCK`
- OK: stale term absent - `QUEUE_BASED`
- OK: pdf term - `MVC`
- OK: pdf term - `CountDownLatch`
- OK: pdf term - `NO_LOCK`
- OK: pdf term - `FILE_LOCK`
- OK: pdf term - `SYNCHRONIZED`
- OK: pdf term - `OPTIMISTIC`

## CSV Data
- `customers.csv`: 2000 data rows, header=['customerId', 'name', 'email', 'phone', 'address', 'avatarUrl', 'tier', 'status']
- `flash_events.csv`: 10 data rows, header=['eventId', 'name', 'startTime', 'endTime', 'status']
- `flash_items.csv`: 500 data rows, header=['itemId', 'productId', 'eventId', 'productName', 'originalPrice', 'salePrice', 'initialStock', 'soldQty', 'version']
- `order_details.csv`: 2500 data rows, header=['detailId', 'orderId', 'productId', 'quantity', 'unitPrice', 'subtotal']
- `orders.csv`: 2500 data rows, header=['orderId', 'customerId', 'orderDate', 'totalAmount', 'status']
- `products.csv`: 5000 data rows, header=['productId', 'name', 'brand', 'category', 'price', 'stock', 'description']
- `transactions.csv`: 120 data rows, header=['transactionId', 'orderId', 'customerId', 'itemId', 'quantity', 'status', 'message', 'timestamp']
- `vouchers.csv`: 10 data rows, header=['voucherId', 'code', 'type', 'value', 'maxDiscount', 'minOrderAmount', 'remainingUses']
- Transaction mechanisms: {'false': 85, 'true': 35}

## Office Documents
- `docs/report.docx`: exists=True, size=38195, valid_zip=True
- `docs/slide.pptx`: exists=True, size=42302, valid_zip=True

## Build And Test Results
### mvn test: PASS (5.3s)
```text
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< com.flashsale:flash-sale-simulator >-----------------
[INFO] Building Flash Sale Simulator 1.0.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ flash-sale-simulator ---
[INFO] skip non existing resourceDirectory C:\Users\phucv\Downloads\shopeeconsole\NHOM_01_LAB211_FlashSale\src\main\resources
[INFO] 
[INFO] --- compiler:3.10.1:compile (default-compile) @ flash-sale-simulator ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ flash-sale-simulator ---
[INFO] skip non existing resourceDirectory C:\Users\phucv\Downloads\shopeeconsole\NHOM_01_LAB211_FlashSale\src\test\resources
[INFO] 
[INFO] --- compiler:3.10.1:testCompile (default-testCompile) @ flash-sale-simulator ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.2.5:test (default-test) @ flash-sale-simulator ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] ---
... truncated ...
```
### mvn clean package: PASS (8.75s)
```text
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< com.flashsale:flash-sale-simulator >-----------------
[INFO] Building Flash Sale Simulator 1.0.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.2.0:clean (default-clean) @ flash-sale-simulator ---
[INFO] Deleting C:\Users\phucv\Downloads\shopeeconsole\NHOM_01_LAB211_FlashSale\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ flash-sale-simulator ---
[INFO] skip non existing resourceDirectory C:\Users\phucv\Downloads\shopeeconsole\NHOM_01_LAB211_FlashSale\src\main\resources
[INFO] 
[INFO] --- compiler:3.10.1:compile (default-compile) @ flash-sale-simulator ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 47 source files to C:\Users\phucv\Downloads\shopeeconsole\NHOM_01_LAB211_FlashSale\target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ flash-sale-simulator ---
[INFO] skip non existing resourceDirectory C:\Users\phucv\Downloads\shopeeconsole\NHOM_01_LAB211_FlashSale\src\test\resources
[INFO] 
[INFO] --- compiler:3.10.1:testCompile (default-testCompile) @ 
... truncated ...
```

## Smoke Run
- PASS: `java -jar` (1.03s)
```text
===== LAB211 FLASH SALE CONSOLE =====
Khach hang: Chua dang nhap
1. Tao du lieu CSV theo de bai
2. Xem 20 san pham Flash Sale
3. Dat hang Flash Sale (Optimistic Lock + Voucher + Tier)
4. Chay simulator tat ca co che
5. Chay simulator mot co che
6. Dang ky khach hang
7. Dang nhap khach hang
8. Benchmark 3 lan lay trung binh
9. Menu Admin (Tao Event/Flash Item, xem Doanh thu/Voucher)
10. Menu Researcher (Cau hinh & Chay gia lap nang cao)
0. Thoat
Chon chuc nang [0]: Da tao du lieu:
- products.csv: 5000 dong
- customers.csv: 2000 dong
- vouchers.csv: 10 dong
- flash_events.csv: 10 dong
- flash_items.csv: 500 dong
- orders.csv: 2500 dong
- order_details.csv: 2500 dong
- transactions.csv: 0 dong
- TOTAL: 12520 dong

===== LAB211 FLASH SALE CONSOLE =====
Khach hang: Chua dang nhap
1. Tao du lieu CSV theo de bai
2. Xem 20 san pham Flash Sale
3. Dat hang Flash Sale (Optimistic Lock + Voucher + Tier)
4. Chay simulator tat ca co che
5. Chay simulator mot co che
6. Dang ky khach hang
7. Dang nhap khach hang
8. Benchmark 3 lan lay trung binh
9. Menu Admin (Tao Event/Flash Item, xem Doanh thu/Voucher)
10. Menu Researcher (Cau hinh & Chay gia lap nang cao)
0. Thoat
Chon chuc nang [0]: So thread [500]: Ton kho ban dau [100]: Co che             Thread    Stock       OK     Fail    Con lai        TPS    vs Base %   Muc tieu
------------------------------------------------------------------------------------------------
NO_LOCK                30        5       30        0        -19        175          0.0   BASELINE
FILE_LOCK              30        5        3       27          0       3333       1804.6        DAT
SYNCHRONIZED           30        5        4       26          0       4286       2349.1        DAT
OPTIMISTIC_LOCK        30        5        4       26          0       2727       1458.3        DAT

===== LAB211 FLASH SALE CONSOLE =====
Khach hang: Chua dang nhap
1. Tao du lieu CSV theo de bai
2. Xem 20 san pham Flash Sale
3. Dat hang Flash Sale (Optimistic Lock + Voucher +
... truncated ...
```

## Gaps
- None detected by this script.

## Fixes
- This script does not edit source files. Codex should fix gaps manually and re-run it.

## How To Re-run
```bash
python C:/Users/phucv/.codex/skills/java-app-auto-test/scripts/java_app_audit.py --root . --pdf LAB211_FlashSale_V2_De_Tai.docx.pdf --report java_app_audit_report.md
```