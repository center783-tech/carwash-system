package com.carwash.security;

import com.carwash.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    /**
     * application.yml 中的 jwt.secret，長度至少 32 bytes（256 bits）才符合 HS256 要求。
     * 範例：用 `openssl rand -base64 32` 產生一組隨機字串。
     */
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(SecurityConstants.CLAIM_USER_ID, principal.getId())
                .claim(SecurityConstants.CLAIM_ROLE, principal.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String getUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String getRole(String token) {
        return extractClaim(token, claims -> claims.get(SecurityConstants.CLAIM_ROLE, String.class));
    }

    public Long getUserId(String token) {
        return extractClaim(token, claims -> claims.get(SecurityConstants.CLAIM_USER_ID, Long.class).longValue());
    }

    public boolean isValid(String token)  {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
