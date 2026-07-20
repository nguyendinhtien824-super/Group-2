# 📋 Bảng Phân Chia Commit GitHub (Dự Án 4 Thành Viên)

> **Tổng số file:** 177 files  
> **Phân chia:** 44 - 44 - 44 - 45 files/người.

---

## 👤 Thành viên 1: Core, Database, Model, Exception & Security (44 file)

### 📌 Nhiệm vụ
Phụ trách cấu hình dự án gốc, file dữ liệu CSV mẫu, Domain Models, Enums, Custom Exceptions và Security.

### 📁 Danh sách File (44 file)
1. `.env.example`
2. `.gitignore`
3. `pom.xml`
4. `README.md`
5. `run_junit.bat`
6. `data/customers.csv`
7. `data/flash_events.csv`
8. `data/flash_items.csv`
9. `data/orders.csv`
10. `data/order_details.csv`
11. `data/products.csv`
12. `data/simulation_stock_100.csv`
13. `data/simulation_stock_100.md`
14. `data/simulation_stock_2000.csv`
15. `data/simulation_stock_2000.md`
16. `data/transactions.csv`
17. `data/vouchers.csv`
18. `src/model/BaseEntity.java`
19. `src/model/Customer.java`
20. `src/model/FlashItem.java`
21. `src/model/FlashSaleEvent.java`
22. `src/model/Order.java`
23. `src/model/OrderDetail.java`
24. `src/model/OrderTransaction.java`
25. `src/model/Product.java`
26. `src/model/SimulationResult.java`
27. `src/model/Voucher.java`
28. `src/model/enums/CustTier.java`
29. `src/model/enums/LockType.java`
30. `src/model/enums/OrderStatus.java`
31. `src/model/enums/SaleStatus.java`
32. `src/exception/DatabaseLockException.java`
33. `src/exception/DataParsingException.java`
34. `src/exception/EndOfInputException.java`
35. `src/exception/EntityNotFoundException.java`
36. `src/exception/InsufficientStockException.java`
37. `src/exception/InvalidDiscountException.java`
38. `src/exception/InvalidEventException.java`
39. `src/exception/InvalidOrderException.java`
40. `src/exception/InvalidOrderStateException.java`
41. `src/exception/InvalidProductException.java`
42. `src/exception/OperationCancelledException.java`
43. `src/exception/OptimisticLockException.java`
44. `src/exception/PurchaseLimitExceededException.java`

### 💻 Lệnh Git Commit
```bash
rtk git add .env.example .gitignore pom.xml README.md run_junit.bat data/ src/model/ src/exception/
rtk git commit -m "feat(core): setup project config, data csvs, models, exceptions and security"
```

---

## 👤 Thành viên 2: Repository, Core Services & Infrastructure (44 file)

### 📌 Nhiệm vụ
Phụ trách tầng Data Access (Repository), các Service xử lý logic hệ thống (Services) và Security tools.

### 📁 Danh sách File (44 file)
1. `src/Main.java`
2. `src/app/BenchmarkCommand.java`
3. `src/app/FlashSaleApplication.java`
4. `src/config/FlashSaleFormats.java`
5. `src/security/AdminCredentials.java`
6. `src/security/AdminPasswordTool.java`
7. `src/security/PasswordPolicy.java`
8. `src/security/PasswordSecurity.java`
9. `src/security/SecurityEnvironment.java`
10. `src/repository/CsvReflectionMapper.java`
11. `src/repository/CsvRepository.java`
12. `src/repository/CsvRowCodec.java`
13. `src/repository/CsvValueConverter.java`
14. `src/repository/CustomerRepository.java`
15. `src/repository/FlashItemRepository.java`
16. `src/repository/FlashItemValidator.java`
17. `src/repository/FlashSaleEventRepository.java`
18. `src/repository/OrderDetailRepository.java`
19. `src/repository/OrderRepository.java`
20. `src/repository/OrderTransactionRepository.java`
21. `src/repository/ProductRepository.java`
22. `src/repository/VoucherRepository.java`
23. `src/service/AdminOrderService.java`
24. `src/service/AdminReportService.java`
25. `src/service/AtomicCsvFile.java`
26. `src/service/CustomerAdminService.java`
27. `src/service/DataGeneratorService.java`
28. `src/service/FlashSaleConstants.java`
29. `src/service/FlashSaleEventService.java`
30. `src/service/FlashSalePolicy.java`
31. `src/service/FlashSaleService.java`
32. `src/service/FlashSaleServiceImpl.java`
33. `src/service/OrderLifecycleService.java`
34. `src/service/OrderPlacementService.java`
35. `src/service/OrderRequestQueue.java`
36. `src/service/SimulationExecutor.java`
37. `src/service/SimulationReportService.java`
38. `src/service/SimulatorPerformanceTarget.java`
39. `src/service/SimulatorService.java`
40. `src/service/VoucherService.java`
41. `test/test/AdminReportServiceTest.java`
42. `test/test/AuthenticationSecurityTest.java`
43. `test/test/ConcurrencyLockTest.java`
44. `test/test/ConsoleInputTest.java`

### 💻 Lệnh Git Commit
```bash
rtk git add src/Main.java src/app/ src/config/ src/security/ src/repository/ src/service/ test/test/AdminReportServiceTest.java test/test/AuthenticationSecurityTest.java test/test/ConcurrencyLockTest.java test/test/ConsoleInputTest.java
rtk git commit -m "feat(service): implement repository layer, core business services and backend tests"
```

---

## 👤 Thành viên 3: Controllers, Console Views & UI Interaction (44 file)

### 📌 Nhiệm vụ
Phụ trách tầng Điều khiển (Controllers), Giao diện Console (Views) và các bài test liên quan.

### 📁 Danh sách File (44 file)
1. `src/controller/AdminCustomerController.java`
2. `src/controller/AdminOrderController.java`
3. `src/controller/AdminReportController.java`
4. `src/controller/CustomerController.java`
5. `src/controller/DataController.java`
6. `src/controller/FlashSaleController.java`
7. `src/controller/OrderController.java`
8. `src/controller/OrderTrackingController.java`
9. `src/controller/ProductController.java`
10. `src/controller/SimulationReportController.java`
11. `src/controller/SimulatorController.java`
12. `src/controller/VoucherController.java`
13. `src/view/AdminCustomerView.java`
14. `src/view/AdminOrderView.java`
15. `src/view/AdminReportView.java`
16. `src/view/AdminView.java`
17. `src/view/AdminVoucherView.java`
18. `src/view/ConsoleInput.java`
19. `src/view/CustomerAccountView.java`
20. `src/view/CustomerVoucherView.java`
21. `src/view/FlashSaleEventAdminView.java`
22. `src/view/FlashSaleItemAdminView.java`
23. `src/view/FlashSaleShoppingView.java`
24. `src/view/FlashSaleView.java`
25. `src/view/MainView.java`
26. `src/view/OrderDetailView.java`
27. `src/view/OrderHistoryView.java`
28. `src/view/OrderTrackingView.java`
29. `src/view/OrderView.java`
30. `src/view/ProductAdminView.java`
31. `src/view/ReportView.java`
32. `src/view/ResearcherView.java`
33. `src/view/SimulatorView.java`
34. `test/test/CsvRepositoryRobustnessTest.java`
35. `test/test/CsvRepositoryTest.java`
36. `test/test/CustomerAdminServiceTest.java`
37. `test/test/CustomerControllerTest.java`
38. `test/test/CustomRepositoryTest.java`
39. `test/test/DataGeneratorTest.java`
40. `test/test/FlashSaleAdminCrudTest.java`
41. `test/test/FlashSaleControllerTest.java`
42. `test/test/FlashSaleLifecycleRequirementsTest.java`
43. `test/test/FlashSaleServiceTest.java`
44. `test/test/LockMechanismRepositoryTest.java`

### 💻 Lệnh Git Commit
```bash
rtk git add src/controller/ src/view/ test/test/CsvRepo* test/test/Customer* test/test/CustomRepo* test/test/DataGen* test/test/FlashSale* test/test/LockMech*
rtk git commit -m "feat(ui): implement application controllers, console views and UI component tests"
```

---

## 👤 Thành viên 4: Documentation, Diagrams, Automation Scripts & Unit Tests (45 file)

### 📌 Nhiệm vụ
Phụ trách toàn bộ tài liệu (Docs, Sơ đồ Class/Use case), Scripts tự động hóa và Unit Tests còn lại.

### 📁 Danh sách File (45 file)
1. `docs/architecture_overview.png`
2. `docs/class_diagram.png`
3. `docs/csv_schema.md`
4. `docs/report.docx`
5. `docs/slide.pptx`
6. `docs/use_case_diagram.png`
7. `docs/charts/paired_throughput_drop_stock_2000.png`
8. `docs/charts/safety_retry_stock_100.png`
9. `docs/charts/throughput_stock_100.png`
10. `docs/charts/throughput_stock_2000.png`
11. `docs/diagrams/architecture-overview.mmd`
12. `docs/diagrams/class-diagram.mmd`
13. `docs/diagrams/data-generator-flow.mmd`
14. `docs/diagrams/order-failure-flow.mmd`
15. `docs/diagrams/order-flow.mmd`
16. `docs/diagrams/race-condition-flow.mmd`
17. `docs/diagrams/simulator-flow.mmd`
18. `docs/diagrams/use-case-diagram.mmd`
19. `docs/diagrams/use_case_diagram.png`
20. `docs/flowcharts/data_generator_flow.png`
21. `docs/flowcharts/order_failure_flow.png`
22. `docs/flowcharts/order_flow.png`
23. `docs/flowcharts/race_condition_flow.png`
24. `docs/flowcharts/simulator_flow.png`
25. `scripts/create_contact_sheets.py`
26. `scripts/create_submission_docs.py`
27. `scripts/create_submission_zip.py`
28. `scripts/render_submission_docs.ps1`
29. `scripts/validate_submission.py`
30. `scripts/submission_docs/benchmark_data.py`
31. `scripts/submission_docs/build_report.py`
32. `scripts/submission_docs/build_slides.py`
33. `scripts/submission_docs/report_content.py`
34. `scripts/submission_docs/slide_theme.py`
35. `scripts/submission_docs/word_theme.py`
36. `test/test/ModelTest.java`
37. `test/test/MultiRoleWorkflowTest.java`
38. `test/test/OrderBusinessRequirementsTest.java`
39. `test/test/OrderRequestQueueTest.java`
40. `test/test/ProductRequirementsTest.java`
41. `test/test/SimulationReportServiceTest.java`
42. `test/test/SimulatorPerformanceTargetTest.java`
43. `test/test/SimulatorServiceTest.java`
44. `test/test/TestModelJUnit.java`
45. `test/test/VoucherServiceTest.java`

### 💻 Lệnh Git Commit
```bash
rtk git add docs/ scripts/ test/test/ModelTest.java test/test/MultiRole* test/test/Order* test/test/Product* test/test/Simul* test/test/TestModel* test/test/Voucher*
rtk git commit -m "docs(project): add architecture diagrams, project documentation, scripts and test suite"
```
