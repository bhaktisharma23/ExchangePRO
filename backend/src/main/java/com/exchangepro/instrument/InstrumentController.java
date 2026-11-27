package com.exchangepro.instrument;

import com.exchangepro.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instruments")
@Tag(name = "Instruments")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    @Operation(summary = "Search instruments by symbol or company name")
    public PageResponse<InstrumentResponse> search(
            @RequestParam(required = false) @Size(max = 60) String query,
            @RequestParam(required = false) @Size(max = 60) String sector,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(instrumentService.search(query, sector, PageRequest.of(page, size)));
    }

    @GetMapping("/sectors")
    @Operation(summary = "List sectors of active instruments")
    public List<String> sectors() {
        return instrumentService.sectors();
    }

    @GetMapping("/{symbol}")
    @Operation(summary = "Instrument master data")
    public InstrumentResponse get(@PathVariable @Size(max = 20) String symbol) {
        return instrumentService.getBySymbol(symbol);
    }
}
