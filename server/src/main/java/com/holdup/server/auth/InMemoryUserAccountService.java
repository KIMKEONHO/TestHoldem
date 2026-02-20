package com.holdup.server.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryUserAccountService {

    private final ConcurrentHashMap<String, UserAccount> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public InMemoryUserAccountService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(users.get(username));
    }

    public UserAccount register(String username, String rawPassword, String displayName) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        UserAccount account = new UserAccount(
                username,
                passwordEncoder.encode(rawPassword),
                displayName
        );
        users.put(username, account);
        return account;
    }

    public Optional<UserAccount> authenticate(String username, String rawPassword) {
        UserAccount account = users.get(username);
        if (account == null) return Optional.empty();
        if (!passwordEncoder.matches(rawPassword, account.passwordHash())) return Optional.empty();
        return Optional.of(account);
    }
}
