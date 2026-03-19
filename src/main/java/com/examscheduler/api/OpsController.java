package com.examscheduler.api;

import java.sql.SQLException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examscheduler.api.dto.AnalyticsOverviewResponse;
import com.examscheduler.api.dto.AuditLogResponse;
import com.examscheduler.api.dto.NotificationResponse;
import com.examscheduler.api.dto.PagedResponse;
import com.examscheduler.service.OpsService;

@RestController
@RequestMapping("/api/v1")
public class OpsController {

    private final OpsService opsService;

    public OpsController(OpsService opsService) {
        this.opsService = opsService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<PagedResponse<AuditLogResponse>> auditLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) throws SQLException {
        return ResponseEntity.ok(opsService.auditLogs(page, size));
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<AnalyticsOverviewResponse> analyticsOverview() throws SQLException {
        return ResponseEntity.ok(opsService.analyticsOverview());
    }

    @GetMapping("/notifications")
    public ResponseEntity<PagedResponse<NotificationResponse>> notifications(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) throws SQLException {
        return ResponseEntity.ok(opsService.notificationsForUser(authentication.getName(), page, size));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable long id, Authentication authentication) throws SQLException {
        opsService.markNotificationRead(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules/export/csv")
    public ResponseEntity<byte[]> exportCsv() throws SQLException {
        byte[] bytes = opsService.exportCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schedule.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(bytes);
    }

    @GetMapping("/schedules/export/pdf")
    public ResponseEntity<byte[]> exportPdf() throws SQLException {
        byte[] bytes = opsService.exportPdf();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schedule.pdf")
            .contentType(MediaType.parseMediaType("application/pdf"))
            .body(bytes);
    }
}
