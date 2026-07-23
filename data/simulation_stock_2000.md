# Báo cáo thực nghiệm Flash Sale Simulator

Kịch bản: Initial stock = 2000
Cấu hình: 1.000 requests x 4 cơ chế x 3 lần chạy; TPS = success / elapsed seconds.
Thời điểm xuất: 2026-07-18T08:00:18.273716100

## Tóm tắt theo cơ chế

| Cơ chế | Số lần | TPS TB | TPS trung vị | OK TB | Âm kho TB | Retry TB | vs Baseline TB | Nhất quán | Mục tiêu |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|
| NO_LOCK | 3 | 388.32 | 383.88 | 1000.0 | 0.0 | 0.0 | 0.0% | KHONG | BASELINE |
| FILE_LOCK | 3 | 284.53 | 284.90 | 1000.0 | 0.0 | 0.0 | -26.6% | CO | DAT |
| SYNCHRONIZED | 3 | 324.74 | 325.63 | 1000.0 | 0.0 | 0.0 | -16.3% | CO | DAT |
| OPTIMISTIC_LOCK | 3 | 3.79 | 3.90 | 9.0 | 0.0 | 2985.0 | -99.0% | CO | CHUA_DAT |

## Dữ liệu thô theo từng lần chạy

| Lần | Cơ chế | Luồng | OK | Fail | Kho cuối | Âm kho | Retry | Retry % | TPS | vs Baseline | Mục tiêu |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | NO_LOCK | 1000 | 1000 | 0 | 1998 | 0 | 0 | 0.00 | 403.88 | 0.0% | BASELINE |
| 1 | FILE_LOCK | 1000 | 1000 | 0 | 783 | 0 | 0 | 0.00 | 275.10 | -31.9% | CHUA_DAT |
| 1 | SYNCHRONIZED | 1000 | 1000 | 0 | 796 | 0 | 0 | 0.00 | 325.63 | -19.4% | DAT |
| 1 | OPTIMISTIC_LOCK | 1000 | 9 | 991 | 1988 | 0 | 2985 | 298.50 | 3.45 | -99.1% | CHUA_DAT |
| 2 | NO_LOCK | 1000 | 1000 | 0 | 1998 | 0 | 0 | 0.00 | 377.22 | 0.0% | BASELINE |
| 2 | FILE_LOCK | 1000 | 1000 | 0 | 809 | 0 | 0 | 0.00 | 284.90 | -24.5% | DAT |
| 2 | SYNCHRONIZED | 1000 | 1000 | 0 | 785 | 0 | 0 | 0.00 | 318.57 | -15.5% | DAT |
| 2 | OPTIMISTIC_LOCK | 1000 | 9 | 991 | 1987 | 0 | 2985 | 298.50 | 4.01 | -98.9% | CHUA_DAT |
| 3 | NO_LOCK | 1000 | 1000 | 0 | 1997 | 0 | 0 | 0.00 | 383.88 | 0.0% | BASELINE |
| 3 | FILE_LOCK | 1000 | 1000 | 0 | 786 | 0 | 0 | 0.00 | 293.60 | -23.5% | DAT |
| 3 | SYNCHRONIZED | 1000 | 1000 | 0 | 788 | 0 | 0 | 0.00 | 330.03 | -14.0% | DAT |
| 3 | OPTIMISTIC_LOCK | 1000 | 9 | 991 | 1990 | 0 | 2985 | 298.50 | 3.90 | -99.0% | CHUA_DAT |
