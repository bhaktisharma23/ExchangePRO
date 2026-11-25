package com.exchangepro.account;

import com.exchangepro.common.error.ResourceNotFoundException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountProperties properties;

    public AccountService(AccountRepository accountRepository, AccountProperties properties) {
        this.accountRepository = accountRepository;
        this.properties = properties;
    }

    /**
     * Opens the trading account for a newly registered user with the configured virtual opening balance.
     *
     * <p>{@link Propagation#MANDATORY}: must be called inside the caller's transaction (registration), so the
     * user and the account are created atomically. Either both rows exist or neither does.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AccountResponse openAccount(UUID userId) {
        Account account = accountRepository.save(Account.open(userId, properties.initialBalance()));
        log.info("Opened virtual account {} for user {} with opening balance {} {}",
                account.getId(), userId, account.getAvailableBalance(), account.getCurrency());
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountForUser(UUID userId) {
        return accountRepository.findByUserId(userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Trading account for user", userId));
    }
}
