package com.exchangepro.admin;

import com.exchangepro.common.web.PageResponse;
import com.exchangepro.user.UserProfileResponse;
import com.exchangepro.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only user listing. Protected twice: the URL rule {@code /api/v1/admin/**} in SecurityConfig, and
 * {@code @PreAuthorize} here (defence in depth: a future change to the URL rules cannot expose it by accident).
 * The full admin dashboard is built in Phase 9.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users, newest first (ADMIN only)")
    public PageResponse<UserProfileResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(userService.listUsers(pageable));
    }
}
