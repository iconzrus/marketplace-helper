package com.marketplacehelper.controller;

import com.marketplacehelper.model.DailySnapshot;
import com.marketplacehelper.service.SnapshotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/snapshots")
@CrossOrigin(origins = "*")
public class SnapshotController {

    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping("/take")
    public ResponseEntity<Void> takeSnapshot(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        snapshotService.takeDailySnapshot(date != null ? date : LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<DailySnapshot>> getSnapshots(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(snapshotService.getSnapshots(from, to));
    }
}


