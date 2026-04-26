package com.examscheduler.api;

import java.sql.SQLException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examscheduler.api.dto.PlanningDatasetDto;
import com.examscheduler.api.dto.PlanningDatasetResponse;
import com.examscheduler.service.PlanningService;

@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @GetMapping("/dataset")
    public ResponseEntity<PlanningDatasetResponse> dataset() throws SQLException {
        return ResponseEntity.ok(planningService.currentDataset());
    }

    @PutMapping("/dataset")
    public ResponseEntity<PlanningDatasetResponse> saveDataset(@RequestBody PlanningDatasetDto request,
                                                               Authentication authentication) throws SQLException {
        String actor = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(planningService.saveDataset(request, actor));
    }

    @GetMapping("/template")
    public ResponseEntity<PlanningDatasetResponse> template() {
        return ResponseEntity.ok(planningService.starterTemplate());
    }
}
