package com.example.authserver.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service de génération et validation des JSON Web Tokens (JWT).
 *
 * <h2>Algorithme</h2>
 * <p>Les tokens sont signés avec HMAC-SHA256 (HS256) ou HS384/HS512 selon la
 * longueur du secret. La clé est dérivée de {@code app.jwt.secret}.</p>
 *
 * <h2>Structure du token</h2>
 * <ul>
 *   <li>{@code sub} — email de l'utilisateur</li>
 *   <li>{@code role} — rôle : "apprenant" ou "formateur"</li>
 *   <li>{@code name} — nom complet de l'utilisateur</li>
 *   <li>{@code iat} — date d'émission</li>
 *   <li>{@code exp} — date d'expiration</li>
 * </ul>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Construit le service JWT à partir de la configuration Spring.
     *
     * @param secret       secret HMAC injecté depuis {@code app.jwt.secret}
     * @param expirationMs durée de vie du token en millisecondes
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Génère un JWT signé contenant l'email, le rôle et le nom de l'utilisateur.
     *
     * @param email l'email de l'utilisateur (claim {@code sub})
     * @param role  le rôle de l'utilisateur ("apprenant" ou "formateur")
     * @param name  le nom complet de l'utilisateur
     * @return le token JWT signé sous forme de chaîne compacte
     */
    public String generateToken(String email, String role, String name) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("name", name)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Génère un JWT avec uniquement l'email (rôle et nom vides).
     * Conservé pour la compatibilité avec les anciens appels.
     *
     * @param email l'email de l'utilisateur
     * @return le token JWT signé
     */
    public String generateToken(String email) {
        return generateToken(email, "", "");
    }

    /**
     * Extrait l'email (claim {@code sub}) d'un token JWT valide.
     *
     * @param token le token JWT
     * @return l'email de l'utilisateur
     * @throws io.jsonwebtoken.JwtException si le token est invalide ou expiré
     */
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrait le rôle (claim {@code role}) d'un token JWT valide.
     *
     * @param token le token JWT
     * @return le rôle de l'utilisateur ("apprenant" ou "formateur")
     * @throws io.jsonwebtoken.JwtException si le token est invalide ou expiré
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Extrait le nom complet (claim {@code name}) d'un token JWT valide.
     *
     * @param token le token JWT
     * @return le nom de l'utilisateur
     * @throws io.jsonwebtoken.JwtException si le token est invalide ou expiré
     */
    public String extractName(String token) {
        return getClaims(token).get("name", String.class);
    }

    /**
     * Calcule le timestamp d'expiration du prochain token émis.
     *
     * @return timestamp epoch en secondes
     */
    public long computeExpiresAt() {
        return (System.currentTimeMillis() + expirationMs) / 1000L;
    }

    /**
     * Vérifie si un token JWT est valide (signature correcte et non expiré).
     *
     * @param token le token JWT à vérifier
     * @return {@code true} si le token est valide, {@code false} sinon
     */
    public boolean isTokenValid(String token) {
        try {
            extractEmail(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse le token JWT et retourne ses claims.
     *
     * @param token le token JWT signé
     * @return les claims du token
     * @throws io.jsonwebtoken.JwtException si la signature est invalide ou le token expiré
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}