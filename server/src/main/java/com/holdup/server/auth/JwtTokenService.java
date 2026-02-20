package com.holdup.server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtTokenService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${holdup.security.jwt-secret:change-this-secret-in-production}")
    private String secret;

    @Value("${holdup.security.jwt-exp-seconds:43200}")
    private long expSeconds;

    public String createToken(UserAccount account) {
        try {
            long exp = Instant.now().getEpochSecond() + expSeconds;
            String header = toBase64Url(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = toBase64Url(objectMapper.writeValueAsBytes(Map.of(
                    "sub", account.username(),
                    "displayName", account.displayName(),
                    "exp", exp
            )));
            String signature = sign(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("토큰 생성 실패", e);
        }
    }

    public Optional<TokenPrincipal> parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();
            String signed = parts[0] + "." + parts[1];
            String expected = sign(signed);
            if (!expected.equals(parts[2])) return Optional.empty();

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, Map.class);

            String username = String.valueOf(payload.get("sub"));
            String displayName = String.valueOf(payload.get("displayName"));
            long exp = Long.parseLong(String.valueOf(payload.get("exp")));
            if (Instant.now().getEpochSecond() > exp) return Optional.empty();

            return Optional.of(new TokenPrincipal(username, displayName));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return toBase64Url(raw);
    }

    private static String toBase64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TokenPrincipal(String username, String displayName) {}
}
