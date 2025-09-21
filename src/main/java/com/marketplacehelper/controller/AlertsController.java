package com.marketplacehelper.controller;

import com.marketplacehelper.dto.AlertDto;
import com.marketplacehelper.service.AlertService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts() {
        SseEmitter emitter = new SseEmitter(0L);
        // Initial push
        try {
            emitter.send(alertService.buildAlerts());
        } catch (Exception ignored) {}

        final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        final Runnable task = () -> {
            try {
                emitter.send(alertService.buildAlerts());
            } catch (Exception e) {
                try { emitter.complete(); } catch (Exception ignored) {}
                scheduler.shutdownNow();
            }
        };
        // Poll every 10 seconds; simple MVP approach
        scheduler.scheduleAtFixedRate(task, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
        emitter.onCompletion(scheduler::shutdown);
        emitter.onTimeout(() -> {
            try { emitter.complete(); } catch (Exception ignored) {}
            scheduler.shutdown();
        });
        return emitter;
    }
}


