package com.exchangepro.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin controller: delegates all work to {@link SystemStatusService}. */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Operational information about the running platform")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/status")
    @Operation(summary = "Application version, database/migration state and active market-data mode")
    public SystemStatusResponse status() {
        return systemStatusService.currentStatus();
    }
}
