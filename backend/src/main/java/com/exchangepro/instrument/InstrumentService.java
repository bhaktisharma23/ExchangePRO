package com.exchangepro.instrument;

import com.exchangepro.common.error.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentService {

    private final InstrumentRepository repository;

    public InstrumentService(InstrumentRepository repository) {
        this.repository = repository;
    }

    /** Symbols are stored upper-case; user input is normalised the same way. */
    public static String normalizeSymbol(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> search(String query, String sector, Pageable pageable) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String escaped = escapeLike(q);
        String sectorFilter = sector == null || sector.isBlank() ? null : sector.trim();
        return repository.search("%" + escaped + "%", escaped + "%", q, sectorFilter, pageable)
                .map(InstrumentResponse::from);
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getBySymbol(String symbol) {
        return InstrumentResponse.from(load(symbol));
    }

    /** Internal: instrument with simulator parameters. Throws 404 for unknown/inactive symbols. */
    @Transactional(readOnly = true)
    public InstrumentDefinition getDefinition(String symbol) {
        return InstrumentDefinition.from(load(symbol));
    }

    @Transactional(readOnly = true)
    public List<String> sectors() {
        return repository.findActiveSectors();
    }

    /** Active instruments with simulator parameters, for the market-data module. */
    @Transactional(readOnly = true)
    public List<InstrumentDefinition> activeDefinitions() {
        return repository.findByActiveTrueOrderBySymbol().stream().map(InstrumentDefinition::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> activeInstruments() {
        return repository.findByActiveTrueOrderBySymbol().stream().map(InstrumentResponse::from).toList();
    }

    private Instrument load(String symbol) {
        String normalized = normalizeSymbol(symbol);
        return repository.findBySymbol(normalized)
                .filter(Instrument::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument", normalized));
    }

    /**
     * Escapes LIKE wildcards so user input is matched literally: searching for "50%" must not mean
     * "anything starting with 50". (The query itself is parameterised, so this is about correctness, not
     * SQL injection.)
     */
    static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
