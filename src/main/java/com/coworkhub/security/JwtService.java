package com.coworkhub.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;
    private final Clock clock;

    public JwtService(@Value("${coworkhub.jwt.secret}") String secret,
                      @Value("${coworkhub.jwt.expiration-minutes:60}") long expirationMinutes, Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.clock = clock;
    }

    public String createToken(AccountUserDetails user) {
        Instant now = clock.instant();
        var builder = Jwts.builder().subject(user.getUsername()).claim("role", user.account().getRole().name())
            .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationMinutes * 60)));
        if (user.memberId() != null) builder.claim("memberId", user.memberId());
        return builder.signWith(key).compact();
    }

    public String username(String token) {
        return Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant()))
            .build().parseSignedClaims(token).getPayload().getSubject();
    }
    public long expirationMinutes() { return expirationMinutes; }
}
