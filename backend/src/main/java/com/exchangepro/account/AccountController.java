package com.exchangepro.account;

import com.exchangepro.auth.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** The caller's own account. The user id comes from the access token, never from the request. */
    @GetMapping("/me")
    @Operation(summary = "Get the current user's virtual trading account and balances")
    public AccountResponse myAccount(@CurrentUserId UUID userId) {
        return accountService.getAccountForUser(userId);
    }
}
