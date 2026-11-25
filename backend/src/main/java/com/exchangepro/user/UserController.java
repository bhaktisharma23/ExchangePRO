package com.exchangepro.user;

import com.exchangepro.auth.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own profile. There is deliberately no {@code /users/{id}} endpoint for regular users:
 * the user id always comes from the verified access token, so one user can never address another's data.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public UserProfileResponse me(@CurrentUserId UUID userId) {
        return userService.getProfile(userId);
    }

    @PatchMapping
    @Operation(summary = "Update the current user's display name")
    public UserProfileResponse update(@CurrentUserId UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }
}
