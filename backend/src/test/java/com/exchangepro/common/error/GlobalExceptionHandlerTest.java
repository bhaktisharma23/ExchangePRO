package com.exchangepro.common.error;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Verifies that each kind of failure produces the agreed Problem Details shape and never leaks internals. */
class GlobalExceptionHandlerTest {

    record SampleRequest(@NotBlank String symbol, @Positive int quantity) {}

    @RestController
    static class SampleController {
        @PostMapping("/sample")
        String create(@Valid @RequestBody SampleRequest request) {
            return "ok";
        }

        @GetMapping("/missing")
        String missing() {
            throw new ResourceNotFoundException("Instrument", "XYZ");
        }

        @GetMapping("/rule")
        String rule() {
            throw new ExchangeProException(ErrorCode.BUSINESS_RULE_VIOLATION, "Insufficient virtual funds");
        }

        @GetMapping("/crash")
        String crash() {
            throw new IllegalStateException("SELECT * FROM secret_table failed");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validationErrorsListEveryInvalidField() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"\",\"quantity\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void notFoundDomainExceptionMapsTo404() throws Exception {
        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Instrument not found: XYZ"));
    }

    @Test
    void businessRuleViolationMapsTo422() throws Exception {
        mockMvc.perform(get("/rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void unexpectedErrorsAreGenericAndDoNotLeakDetails() throws Exception {
        mockMvc.perform(get("/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret_table"))));
    }
}
