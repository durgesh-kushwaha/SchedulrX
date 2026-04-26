package com.examscheduler.api;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.examscheduler.service.OpsService;

class OpsControllerTest {

    @Test
    void deleteNotificationReturnsNoContentAndDelegatesToService() throws SQLException {
        OpsService opsService = mock(OpsService.class);
        OpsController controller = new OpsController(opsService);
        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "x");

        ResponseEntity<Void> response = controller.deleteNotification(42L, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(opsService).deleteNotification("admin", 42L);
    }

    @Test
    void markReadReturnsNoContentAndDelegatesToService() throws SQLException {
        OpsService opsService = mock(OpsService.class);
        OpsController controller = new OpsController(opsService);
        Authentication authentication = new UsernamePasswordAuthenticationToken("student", "x");

        ResponseEntity<Void> response = controller.markRead(7L, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(opsService).markNotificationRead("student", 7L);
    }
}