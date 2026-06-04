# LAB211 Flash Sale Console App

Ung dung mo phong Flash Sale theo de bai LAB211: Java OOP, MVC, du lieu CSV, simulator da luong bang `CountDownLatch`.

## Yeu cau

- Java JDK 17+
- Maven 3.8+

## Build

```bash
mvn clean package
```

## Chay test

```bash
mvn test
```

## Chay ung dung console

```bash
java -jar target/flash-sale-simulator-1.0.0.jar
```

Hoac tren Windows, chay file o thu muc cha:

```bat
run_project.bat
```

## Chuc nang console

- Tao du lieu CSV theo yeu cau PDF.
- Xem danh sach Flash Sale.
- Dat hang Flash Sale bang Optimistic Lock.
- Chay simulator mot co che hoac tat ca co che.
- Chay benchmark 3 lan de lay trung binh theo yeu cau thuc nghiem.
- So sanh `NO_LOCK`, `FILE_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC_LOCK`.
- Dang ky va dang nhap khach hang bang du lieu CSV (kiem tra trang thai hoat dong ACTIVE/BANNED).
- Menu Admin: Quan ly tai khoan khach hang (CRUD + Ban/Unban), tao su kien & mat hang flash sale, xem bao cao doanh thu va phan tich voucher.
- Test tu dong bang JUnit cho DataGenerator, Repository CRUD/Search, FlashSaleService va Simulator.

## Cau truc chinh

- `src/model`: Entity, enum, base model.
- `src/repository`: Doc/ghi CSV va logic tru kho/lock.
- `src/service`: Business service, data generator, simulator.
- `src/controller`: Dieu phoi flow giua view va service/repository.
- `src/view`: Menu va hien thi console.
- `data`: File CSV du lieu mau va ket qua simulation.
- `docs`: Bao cao, slide, UML va flowchart.
- `ai_logs`: AI log tung thanh vien.
