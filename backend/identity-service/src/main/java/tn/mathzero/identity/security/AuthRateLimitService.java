package tn.mathzero.identity.security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {
    private final ConcurrentHashMap<String, Deque<Instant>> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> registerAttempts = new ConcurrentHashMap<>();

    public void checkLogin(String clientKey) {
        check(loginAttempts, "login:" + clientKey, 10);
    }

    public void checkRegister(String clientKey) {
        check(registerAttempts, "register:" + clientKey, 5);
    }

    private void check(ConcurrentHashMap<String, Deque<Instant>> attempts, String key, int limit) {
        Instant cutoff = Instant.now().minusSeconds(60);
        Deque<Instant> window = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.removeFirst();
            }
            if (window.size() >= limit) {
                throw new AuthException("RATE_LIMITED", "Too many attempts. Please try again later.");
            }
            window.addLast(Instant.now());
        }
    }
}
