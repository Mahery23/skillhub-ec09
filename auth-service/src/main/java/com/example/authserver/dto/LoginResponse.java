package com.example.authserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Réponse JSON retournée après une authentification HMAC réussie.
 *
 * <p>Le client doit stocker le {@code accessToken} et l'inclure dans
 * toutes ses requêtes protégées via le header
 * {@code Authorization: Bearer <accessToken>}.</p>
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** JWT signé à transmettre dans le header {@code Authorization: Bearer <token>}. */
    private String accessToken;

    /** Timestamp epoch en secondes indiquant quand le token expire. */
    private long expiresAt;
}