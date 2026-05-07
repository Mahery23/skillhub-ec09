package com.example.authserver.controller;

import com.example.authserver.dto.LoginRequest;
import com.example.authserver.dto.LoginResponse;
import com.example.authserver.service.AuthService;
import com.example.authserver.service.CryptoException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Point d'entrée REST du microservice d'authentification forte.
 *
 * <h2>Endpoints exposés</h2>
 * <ul>
 *   <li>{@code POST /api/auth/register} — inscription d'un nouvel utilisateur</li>
 *   <li>{@code GET  /api/auth/challenge} — obtention d'un nonce pour le login HMAC</li>
 *   <li>{@code POST /api/auth/login}    — authentification HMAC et émission du JWT</li>
 * </ul>
 *
 * <p>Tous ces endpoints sont publics (aucun JWT requis).
 * Voir {@link com.example.authserver.config.SecurityConfig} pour la configuration.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * Inscrit un nouvel utilisateur dans le système.
     *
     * @param body JSON contenant {@code email}, {@code password}, {@code name}, {@code role}
     * @return 200 avec un message de confirmation, 409 si l'email est déjà utilisé
     * @throws CryptoException si le chiffrement du mot de passe échoue
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody Map<String, String> body) throws CryptoException {
        authService.register(
                body.get("email"),
                body.get("password"),
                body.getOrDefault("name", "Utilisateur"),
                body.getOrDefault("role", "apprenant")
        );
        return ResponseEntity.ok(Map.of("message", "Utilisateur créé avec succès."));
    }

    /**
     * Génère un nonce UUID pour initier le protocole HMAC.
     *
     * <p>Le client doit inclure ce nonce dans le calcul du HMAC lors du login.
     * Le nonce est à usage unique — toute réutilisation est rejetée.</p>
     *
     * @param email l'email de l'utilisateur souhaitant se connecter
     * @return 200 avec {@code { "nonce": "<uuid>" }}
     */
    @GetMapping("/challenge")
    public ResponseEntity<Map<String, String>> challenge(@RequestParam String email) {
        String nonce = authService.generateChallenge(email);
        return ResponseEntity.ok(Map.of("nonce", nonce));
    }

    /**
     * Authentifie un utilisateur via le protocole HMAC et retourne un JWT.
     *
     * @param request payload contenant {@code email}, {@code nonce}, {@code timestamp}, {@code hmac}
     * @return 200 avec {@link LoginResponse} contenant le JWT, 401 si l'authentification échoue
     * @throws CryptoException si le déchiffrement du mot de passe stocké échoue
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) throws CryptoException {
        return ResponseEntity.ok(authService.login(request));
    }
}