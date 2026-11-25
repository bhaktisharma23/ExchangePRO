package com.exchangepro.user;

import com.exchangepro.common.error.ResourceNotFoundException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public API of the user module. Other modules use this service, never {@link UserRepository} directly. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Canonical form used for storage and lookup: trimmed and lower-cased. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Creates a user. The caller supplies an already-hashed password.
     *
     * <p>Duplicate e-mails are detected twice: a friendly pre-check, and the database UNIQUE constraint, which is
     * the only check that is safe when two registrations for the same e-mail race each other.
     */
    @Transactional
    public UserCredentials createUser(String email, String passwordHash, String displayName, Role role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(normalizedEmail, passwordHash, displayName.trim(), role);
        try {
            userRepository.saveAndFlush(user); // flush now so a constraint violation surfaces here
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyRegisteredException();
        }
        return toCredentials(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email)).map(UserService::toCredentials);
    }

    @Transactional(readOnly = true)
    public Optional<UserCredentials> findCredentialsById(UUID userId) {
        return userRepository.findById(userId).map(UserService::toCredentials);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return UserProfileResponse.from(load(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = load(userId);
        user.changeDisplayName(request.displayName().trim());
        userRepository.flush(); // apply now so updatedAt/version in the response are current
        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserProfileResponse::from);
    }

    private User load(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private static UserCredentials toCredentials(User user) {
        return new UserCredentials(user.getId(), user.getEmail(), user.getPasswordHash(), user.getDisplayName(),
                user.getRole(), user.isEnabled());
    }
}
