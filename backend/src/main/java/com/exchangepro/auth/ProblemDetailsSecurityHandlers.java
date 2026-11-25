package com.exchangepro.auth;

import com.exchangepro.common.error.ApiProblems;
import com.exchangepro.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Spring Security rejects unauthenticated/unauthorized requests in a servlet filter, before any controller
 * runs. These handlers make those rejections use the same Problem Details body as every other API error.
 */
@Component
public class ProblemDetailsSecurityHandlers {

    private final ObjectMapper objectMapper;

    public ProblemDetailsSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: no token, or the token is invalid/expired. */
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            write(response, ErrorCode.UNAUTHORIZED, "Authentication is required or the access token is invalid");
        };
    }

    /** 403: valid token, but the role is not allowed (e.g. USER calling an ADMIN endpoint). */
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) ->
                write(response, ErrorCode.FORBIDDEN, "You do not have permission to perform this action");
    }

    private void write(HttpServletResponse response, ErrorCode code, String detail) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiProblems.create(code, detail));
    }
}
