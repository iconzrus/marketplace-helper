package com.marketplacehelper.controller;

import com.marketplacehelper.dto.AlertDto;
import com.marketplacehelper.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertsController {

    private final AlertService alertService;

    public AlertsController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<AlertDto>> getAlerts() {
        return ResponseEntity.ok(alertService.buildAlerts());
    }
}


