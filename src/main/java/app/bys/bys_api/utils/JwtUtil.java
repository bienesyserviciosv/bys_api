package app.bys.bys_api.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret.key}")
    private String jwtKey;

    public String generateToken(Authentication authentication) {
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE"))
                .collect(Collectors.joining(" "));
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("self")
                .issuedAt(Date.from(now))
                .subject(authentication.getName())
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .claim("authorities", scope)
                .signWith(getSignatureKey(), Jwts.SIG.HS256)
                .compact();

    }

    public String generateToken(String subject, Collection<? extends GrantedAuthority> authorities) {
        String scope = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE"))
                .collect(Collectors.joining(" "));

        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("self")
                .issuedAt(Date.from(now))
                .subject(subject)
                .expiration(Date.from(now.plus(24, ChronoUnit.HOURS)))
                .claim("authorities", scope)
                .signWith(getSignatureKey(), Jwts.SIG.HS256)
                .compact();
    }

    public SecretKey getSignatureKey(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }


    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignatureKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }


}
