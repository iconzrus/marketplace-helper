package com.marketplacehelper.controller;

import com.marketplacehelper.dto.AutoFillRequestDto;
import com.marketplacehelper.dto.AutoFillResultDto;
import com.marketplacehelper.service.DemoDataService;
import com.marketplacehelper.service.WbApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    private final DemoDataService demoDataService;
    private final WbApiService wbApiService;

    public DemoController(DemoDataService demoDataService, WbApiService wbApiService) {
        this.demoDataService = demoDataService;
        this.wbApiService = wbApiService;
    }

    @PostMapping("/autofill-costs")
    public ResponseEntity<AutoFillResultDto> autoFillCosts(@RequestBody AutoFillRequestDto request) {
        if (!wbApiService.isMockMode()) {
            return ResponseEntity.status(403).body(null);
        }
        AutoFillResultDto result = demoDataService.autoFillMissingCosts(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/fill-random-all")
    public ResponseEntity<?> fillRandomAll() {
        if (!wbApiService.isMockMode()) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Demo is available only in Mock mode"));
        }
        int affected = demoDataService.fillRandomAll();
        return ResponseEntity.ok(java.util.Map.of("affected", affected));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestParam(name = "count") int count,
                                      @RequestParam(name = "type", defaultValue = "both") String type) {
        if (!wbApiService.isMockMode()) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Demo is available only in Mock mode"));
        }
        int created = demoDataService.generateDemo(count, type);
        return ResponseEntity.ok(java.util.Map.of("created", created));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam(name = "count", required = false) Integer count,
                                    @RequestParam(name = "all", defaultValue = "false") boolean all) {
        if (!wbApiService.isMockMode()) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Demo is available only in Mock mode"));
        }
        com.marketplacehelper.dto.DeleteResultDto result = demoDataService.deleteRandom(count != null ? count : 0, all);
        return ResponseEntity.ok(result);
    }
}
