package app.bys.bys_api.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifica el identityToken (JWT) que manda el SDK de "Sign in with Apple"
 * del lado del cliente. Apple no publica una librería Java oficial (a
 * diferencia de Google, que sí tiene GoogleIdTokenVerifier ya usado en
 * AuthController) — esto replica a mano lo mismo que esa librería hace:
 *
 *  1. Lee el "kid" del header del JWT (sin verificar nada todavía).
 *  2. Busca la clave pública de Apple con ese kid (cacheada en memoria,
 *     se refresca sola si aparece un kid nuevo o pasaron más de 24h).
 *  3. Verifica la firma RS256 del token con esa clave pública usando jjwt
 *     (misma librería que ya usa JwtUtil para los JWT propios de BYS —
 *     no hace falta ninguna dependencia nueva en pom.xml).
 *  4. Valida issuer y audience (bundle id de la app iOS).
 *
 * Referencia de Apple: https://appleid.apple.com/auth/keys (JWKS público,
 * sin autenticación) y https://developer.apple.com/documentation/sign_in_with_apple/verify_identity_token.
 * Parte del fix de Guideline 4.8 (2026-10-04).
 */
@Component
@Slf4j
public class AppleIdTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    // Bundle id de la app iOS (com.soytubys.app) — es la "audience" que
    // Apple pone dentro del identityToken. No es secreto.
    @Value("${apple.oauth2.bundle-id}")
    private String bundleId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile Instant lastFetch = Instant.EPOCH;

    public Claims verify(String identityToken) {
        if (identityToken == null || identityToken.isBlank()) {
            throw new IllegalArgumentException("identityToken de Apple vacío");
        }

        String kid = extractKid(identityToken);
        PublicKey key = getKey(kid);

        // parseSignedClaims ya valida la firma y lanza una excepción si el
        // token está vencido (exp) — no hace falta chequear eso a mano.
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(identityToken)
                .getPayload();

        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new RuntimeException("Apple identityToken: issuer inesperado (" + claims.getIssuer() + ")");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(bundleId)) {
            throw new RuntimeException("Apple identityToken: audience inesperada");
        }

        return claims;
    }

    private String extractKid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new RuntimeException("identityToken de Apple con formato inválido");
        }
        try {
            String headerJson = new String(Decoders.BASE64URL.decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.path("kid").asText(null);
            if (kid == null) {
                throw new RuntimeException("identityToken de Apple sin kid en el header");
            }
            return kid;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer el header del identityToken de Apple", e);
        }
    }

    private PublicKey getKey(String kid) {
        PublicKey key = keyCache.get(kid);
        if (key == null || isStale()) {
            fetchKeys();
            key = keyCache.get(kid);
        }
        if (key == null) {
            throw new RuntimeException("No se encontró la clave pública de Apple para kid=" + kid);
        }
        return key;
    }

    private boolean isStale() {
        return Instant.now().isAfter(lastFetch.plus(24, ChronoUnit.HOURS));
    }

    private synchronized void fetchKeys() {
        try {
            String json = restTemplate.getForObject(APPLE_KEYS_URL, String.class);
            JsonNode keysNode = objectMapper.readTree(json).path("keys");

            Map<String, PublicKey> freshKeys = new ConcurrentHashMap<>();
            for (JsonNode jwk : keysNode) {
                String kid = jwk.path("kid").asText();
                String n = jwk.path("n").asText();
                String e = jwk.path("e").asText();
                freshKeys.put(kid, buildRsaPublicKey(n, e));
            }

            keyCache.clear();
            keyCache.putAll(freshKeys);
            lastFetch = Instant.now();
            log.info("Claves públicas de Apple refrescadas ({} keys)", freshKeys.size());
        } catch (Exception e) {
            log.error("No se pudieron obtener las claves públicas de Apple: {}", e.getMessage());
            throw new RuntimeException("No se pudieron obtener las claves públicas de Apple", e);
        }
    }

    private PublicKey buildRsaPublicKey(String modulusBase64Url, String exponentBase64Url) throws Exception {
        BigInteger modulus = new BigInteger(1, Decoders.BASE64URL.decode(modulusBase64Url));
        BigInteger exponent = new BigInteger(1, Decoders.BASE64URL.decode(exponentBase64Url));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
