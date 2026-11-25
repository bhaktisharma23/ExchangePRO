package com.exchangepro.system;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exchangepro.support.WebSliceSecurityConfig;
import com.exchangepro.system.SystemStatusResponse.ComponentState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer slice test: only MVC infrastructure is started; the service is mocked. */
@WebMvcTest(SystemStatusController.class)
@Import(WebSliceSecurityConfig.class)
class SystemStatusControllerTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemStatusService systemStatusService;

    @Test
    void returnsStatusJsonWithCorrelationHeader() throws Exception {
        when(systemStatusService.currentStatus()).thenReturn(new SystemStatusResponse(
                "EXCHANGE PRO", "0.1.0", List.of(), Instant.parse("2026-01-15T09:15:00Z"),
                new SystemStatusResponse.DatabaseStatus(ComponentState.UP, "PostgreSQL 16.10", "1"),
                new SystemStatusResponse.MarketDataStatus("SIMULATION", "SIMULATED DATA", false)));

        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.application").value("EXCHANGE PRO"))
                .andExpect(jsonPath("$.serverTime").value("2026-01-15T09:15:00Z"))
                .andExpect(jsonPath("$.database.state").value("UP"))
                .andExpect(jsonPath("$.marketData.label").value("SIMULATED DATA"))
                .andExpect(jsonPath("$.marketData.realTime").value(false));
    }

    @Test
    void corsAllowsConfiguredFrontendOrigin() throws Exception {
        when(systemStatusService.currentStatus()).thenReturn(null);

        mockMvc.perform(get("/api/v1/system/status").header("Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void statusEndpointIsPublic() throws Exception {
        when(systemStatusService.currentStatus()).thenReturn(null);

        mockMvc.perform(get("/api/v1/system/status")).andExpect(status().isOk());
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/system/status").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
    }
}
