package com.holdup.server.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return repository.findById(username).map(this::toDomain);
    }

    public UserAccount register(String username, String rawPassword, String displayName, String email) {
        if (repository.existsById(username)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        UserAccountEntity entity = new UserAccountEntity();
        entity.setUsername(username);
        entity.setDisplayName(displayName);
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(rawPassword));

        return toDomain(repository.save(entity));
    }

    public Optional<UserAccount> authenticate(String username, String rawPassword) {
        Optional<UserAccountEntity> found = repository.findById(username);
        if (found.isEmpty()) return Optional.empty();
        UserAccountEntity user = found.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) return Optional.empty();
        return Optional.of(toDomain(user));
    }

    public Optional<String> findIdByEmail(String email) {
        return repository.findByEmail(email).map(UserAccountEntity::getUsername);
    }

    public boolean resetPassword(String username, String email, String newPassword) {
        Optional<UserAccountEntity> found = repository.findById(username);
        if (found.isEmpty()) return false;
        UserAccountEntity user = found.get();
        if (!user.getEmail().equalsIgnoreCase(email)) return false;
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(user);
        return true;
    }

    private UserAccount toDomain(UserAccountEntity e) {
        return new UserAccount(e.getUsername(), e.getPasswordHash(), e.getDisplayName(), e.getEmail());
    }
}
