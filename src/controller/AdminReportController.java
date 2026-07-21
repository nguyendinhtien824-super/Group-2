package controller;

import service.AdminReportService;

import java.util.Objects;

/** Presentation adapter for administrator reports. */
public class AdminReportController {
    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = Objects.requireNonNull(adminReportService, "adminReportService");
    }

    public Result getReport() {
        try {
            return new Result(true, "Đã tạo báo cáo.", adminReportService.generate());
        } catch (IllegalStateException exception) {
            return new Result(false, "Không thể tạo báo cáo từ dữ liệu hiện tại.", null);
        }
    }

    public record Result(boolean success, String message, AdminReportService.Report report) {
    }
}

// Member 3
