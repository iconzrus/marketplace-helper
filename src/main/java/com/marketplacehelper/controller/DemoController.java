package com.marketplacehelper.controller;

import com.marketplacehelper.dto.AutoFillRequestDto;
import com.marketplacehelper.dto.AutoFillResultDto;
import com.marketplacehelper.service.DemoDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    private final DemoDataService demoDataService;

    public DemoController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/autofill-costs")
    public ResponseEntity<AutoFillResultDto> autoFillCosts(@RequestBody AutoFillRequestDto request) {
        AutoFillResultDto result = demoDataService.autoFillMissingCosts(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/fill-random-all")
    public ResponseEntity<?> fillRandomAll() {
        int affected = demoDataService.fillRandomAll();
        return ResponseEntity.ok(java.util.Map.of("affected", affected));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestParam(name = "count") int count,
                                      @RequestParam(name = "type", defaultValue = "both") String type) {
        int created = demoDataService.generateDemo(count, type);
        return ResponseEntity.ok(java.util.Map.of("created", created));
    }
}
