package com.marketplacehelper.controller;

import com.marketplacehelper.dto.WbApiStatusReportDto;
import com.marketplacehelper.service.WbApiStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wb-status")
@CrossOrigin(origins = "*")
public class WbApiStatusController {

    private final WbApiStatusService statusService;

    public WbApiStatusController(WbApiStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    public ResponseEntity<WbApiStatusReportDto> getStatus() {
        return ResponseEntity.ok(statusService.checkAll());
    }
}


