# Báo cáo thực nghiệm Flash Sale Simulator

Kịch bản: Initial stock = 100
Cấu hình: 1.000 requests x 4 cơ chế x 3 lần chạy; TPS = success / elapsed seconds.
Thời điểm xuất: 2026-07-18T07:59:39.137919500

## Tóm tắt theo cơ chế

| Cơ chế | Số lần | TPS TB | TPS trung vị | OK TB | Âm kho TB | Retry TB | vs Baseline TB | Nhất quán | Mục tiêu |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|
| NO_LOCK | 3 | 566.06 | 365.76 | 1000.0 | 1093.0 | 0.0 | 0.0% | KHONG | BASELINE |
| FILE_LOCK | 3 | 100.91 | 79.28 | 82.3 | 0.0 | 0.0 | -81.5% | CO | CHUA_DAT |
| SYNCHRONIZED | 3 | 174.90 | 123.33 | 84.3 | 0.0 | 0.0 | -68.2% | CO | CHUA_DAT |
| OPTIMISTIC_LOCK | 3 | 4.47 | 4.12 | 8.7 | 0.0 | 2986.0 | -99.1% | CO | CHUA_DAT |

## Dữ liệu thô theo từng lần chạy

| Lần | Cơ chế | Luồng | OK | Fail | Kho cuối | Âm kho | Retry | Retry % | TPS | vs Baseline | Mục tiêu |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | NO_LOCK | 1000 | 1000 | 0 | 97 | 1100 | 0 | 0.00 | 970.87 | 0.0% | BASELINE |
| 1 | FILE_LOCK | 1000 | 81 | 919 | 0 | 0 | 0 | 0.00 | 160.40 | -83.5% | CHUA_DAT |
| 1 | SYNCHRONIZED | 1000 | 82 | 918 | 0 | 0 | 0 | 0.00 | 283.74 | -70.8% | CHUA_DAT |
| 1 | OPTIMISTIC_LOCK | 1000 | 8 | 992 | 91 | 0 | 2988 | 298.80 | 5.26 | -99.5% | CHUA_DAT |
| 2 | NO_LOCK | 1000 | 1000 | 0 | 98 | 1107 | 0 | 0.00 | 361.53 | 0.0% | BASELINE |
| 2 | FILE_LOCK | 1000 | 78 | 922 | 0 | 0 | 0 | 0.00 | 63.06 | -82.6% | CHUA_DAT |
| 2 | SYNCHRONIZED | 1000 | 83 | 917 | 0 | 0 | 0 | 0.00 | 123.33 | -65.9% | CHUA_DAT |
| 2 | OPTIMISTIC_LOCK | 1000 | 9 | 991 | 91 | 0 | 2985 | 298.50 | 4.03 | -98.9% | CHUA_DAT |
| 3 | NO_LOCK | 1000 | 1000 | 0 | 98 | 1072 | 0 | 0.00 | 365.76 | 0.0% | BASELINE |
| 3 | FILE_LOCK | 1000 | 88 | 912 | 0 | 0 | 0 | 0.00 | 79.28 | -78.3% | CHUA_DAT |
| 3 | SYNCHRONIZED | 1000 | 88 | 912 | 0 | 0 | 0 | 0.00 | 117.65 | -67.8% | CHUA_DAT |
| 3 | OPTIMISTIC_LOCK | 1000 | 9 | 991 | 88 | 0 | 2985 | 298.50 | 4.12 | -98.9% | CHUA_DAT |
