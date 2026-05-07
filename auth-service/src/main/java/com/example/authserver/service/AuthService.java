package com.example.authserver.service;

import com.example.authserver.dto.LoginRequest;
import com.example.authserver.dto.LoginResponse;
import com.example.authserver.entity.AuthNonce;
import com.example.authserver.entity.User;
import com.example.authserver.repository.AuthNonceRepository;
import com.example.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Service principal du microservice d'authentification forte (protocole HMAC).
 *
 * <h2>Flux d'authentification</h2>
 * <ol>
 *   <li>Le client demande un nonce via {@code GET /api/auth/challenge}</li>
 *   <li>Le client calcule {@code HMAC(password, email:nonce:timestamp)}</li>
 *   <li>Le client envoie {@code POST /api/auth/login} sans transmettre le mot de passe</li>
 *   <li>Le serveur vérifie le timestamp, le nonce (anti-rejeu) et le HMAC, puis émet un JWT</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthNonceRepository nonceRepository;
    private final CryptoService cryptoService;
    private final HmacService hmacService;
    private final JwtService jwtService;

    @Value("${app.auth.timestamp-window-seconds:60}")
    private long timestampWindowSeconds;

    @Value("${app.auth.nonce-ttl-seconds:120}")
    private long nonceTtlSeconds;

    /**
     * Inscrit un nouvel utilisateur en chiffrant son mot de passe avec AES-GCM.
     *
     * @param email    adresse email (identifiant unique)
     * @param password mot de passe en clair — chiffré avant stockage
     * @param name     nom complet de l'utilisateur
     * @param role     rôle : {@code "apprenant"} ou {@code "formateur"}
     * @throws CryptoException             si le chiffrement du mot de passe échoue
     * @throws ResponseStatusException 409 si l'email est déjà utilisé
     * @throws ResponseStatusException 400 si le rôle est invalide
     */
    @Transactional
    public void register(String email, String password, String name, String role) throws CryptoException {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé.");
        }
        if (!role.equals("apprenant") && !role.equals("formateur")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide.");
        }
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setPasswordEncrypted(cryptoService.encrypt(password));
        userRepository.save(user);
    }

    /**
     * Génère un nonce UUID aléatoire pour initier le protocole HMAC.
     *
     * @param email l'email de l'utilisateur (non utilisé, conservé pour cohérence de l'API)
     * @return un UUID sous forme de chaîne
     */
    public String generateChallenge(String email) {
        return UUID.randomUUID().toString();
    }

    /**
     * Authentifie un utilisateur via le protocole HMAC en 7 étapes.
     *
     * <p>Le mot de passe ne circule jamais sur le réseau. Le serveur recalcule
     * le HMAC à partir du mot de passe déchiffré et compare en temps constant
     * pour résister aux timing attacks.</p>
     *
     * @param request payload contenant email, nonce, timestamp et hmac
     * @return un {@link LoginResponse} contenant le JWT et son expiration
     * @throws CryptoException         si le déchiffrement du mot de passe échoue
     * @throws ResponseStatusException 401 si le timestamp, le nonce ou le HMAC est invalide
     */
    @Transactional
    public LoginResponse login(LoginRequest request) throws CryptoException {

        // 1. Vérifier que l'email existe
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access Denied"));

        // 2. Vérifier la fenêtre de timestamp (±60 secondes)
        long diff = Math.abs(Instant.now().getEpochSecond() - request.getTimestamp());
        if (diff > timestampWindowSeconds) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access Denied");
        }

        // 3. Vérifier le nonce (anti-rejeu)
        Optional<AuthNonce> existingNonce = nonceRepository.findByUserAndNonce(user, request.getNonce());
        if (existingNonce.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access Denied");
        }

        // 4. Réserver le nonce avant la vérification HMAC (évite les race conditions)
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(nonceTtlSeconds);
        AuthNonce newNonce = new AuthNonce(user, request.getNonce(), expiresAt);
        nonceRepository.save(newNonce);

        // 5. Recalculer le HMAC côté serveur
        String passwordPlain = cryptoService.decrypt(user.getPasswordEncrypted());
        String message = hmacService.buildMessage(request.getEmail(), request.getNonce(), request.getTimestamp());
        String expectedHmac = hmacService.compute(passwordPlain, message);

        // 6. Comparaison en temps constant (résistant aux timing attacks)
        if (!hmacService.verifyConstantTime(expectedHmac, request.getHmac())) {
            newNonce.setConsumed(true);
            nonceRepository.save(newNonce);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access Denied");
        }

        // 7. Émettre le JWT avec email, rôle et nom
        newNonce.setConsumed(true);
        nonceRepository.save(newNonce);

        String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getName());
        return new LoginResponse(token, jwtService.computeExpiresAt());
    }
}