package com.marketplacehelper.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimpleAuthService {

    private static final String DEFAULT_USERNAME = "iconzrus";
    private static final String DEFAULT_PASSWORD = "iconzrus";

    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    public Optional<AuthResponse> login(String username, String password) {
        if (DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password)) {
            String token = UUID.randomUUID().toString();
            activeSessions.put(token, new Session(username, Instant.now()));
            return Optional.of(new AuthResponse(token, username));
        }
        return Optional.empty();
    }

    public boolean isTokenValid(String token) {
        return activeSessions.containsKey(token);
    }

    public Optional<String> getUsernameByToken(String token) {
        return Optional.ofNullable(activeSessions.get(token)).map(Session::username);
    }

    public void logout(String token) {
        activeSessions.remove(token);
    }

    private record Session(String username, Instant issuedAt) {
    }
}
