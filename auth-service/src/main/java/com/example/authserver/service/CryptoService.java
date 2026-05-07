package com.example.authserver.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service de chiffrement/déchiffrement AES-GCM des mots de passe utilisateur.
 *
 * <h2>Algorithme</h2>
 * <p>AES-256-GCM avec IV aléatoire de 12 octets et tag d'authentification de 128 bits.
 * Chaque chiffrement produit un IV différent, rendant deux chiffrements du même
 * mot de passe indiscernables.</p>
 *
 * <h2>Format de stockage</h2>
 * <pre>v1:Base64(iv):Base64(ciphertext+tag)</pre>
 *
 * <h2>Clé</h2>
 * <p>Dérivée des 32 premiers caractères de {@code APP_MASTER_KEY}.
 * L'application refuse de démarrer si cette variable est absente ou trop courte.</p>
 */
@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int KEY_SIZE = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${APP_MASTER_KEY:#{null}}")
    private String masterKey;

    private SecretKeySpec secretKey;

    /**
     * Initialise la clé AES à partir de {@code APP_MASTER_KEY}.
     * Appelé automatiquement par Spring au démarrage.
     *
     * @throws IllegalStateException si la clé est absente ou fait moins de 32 caractères
     */
    @PostConstruct
    public void init() {
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException(
                    "APP_MASTER_KEY est absente. L'application ne peut pas démarrer sans clé de chiffrement.");
        }
        if (masterKey.length() < KEY_SIZE) {
            throw new IllegalStateException(
                    "APP_MASTER_KEY doit faire au moins 32 caractères.");
        }
        byte[] keyBytes = masterKey.substring(0, KEY_SIZE)
                .getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Chiffre un mot de passe en clair avec AES-256-GCM.
     *
     * @param plainPassword le mot de passe en clair à chiffrer
     * @return la valeur chiffrée au format {@code v1:Base64(iv):Base64(ciphertext)}
     * @throws CryptoException si le chiffrement échoue
     */
    public String encrypt(String plainPassword) throws CryptoException {
        try {
            byte[] iv = new byte[IV_SIZE];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] encrypted = cipher.doFinal(
                    plainPassword.getBytes(StandardCharsets.UTF_8));

            String ivB64 = Base64.getEncoder().encodeToString(iv);
            String cipherB64 = Base64.getEncoder().encodeToString(encrypted);

            return "v1:" + ivB64 + ":" + cipherB64;
        } catch (Exception e) {
            throw new CryptoException("Erreur lors du chiffrement", e);
        }
    }

    /**
     * Déchiffre un mot de passe stocké au format {@code v1:Base64(iv):Base64(ciphertext)}.
     *
     * @param encryptedValue la valeur chiffrée à déchiffrer
     * @return le mot de passe en clair
     * @throws CryptoException si le format est invalide, la clé incorrecte ou les données altérées
     */
    public String decrypt(String encryptedValue) throws CryptoException {
        try {
            String[] parts = encryptedValue.split(":");
            if (parts.length != 3 || !parts[0].equals("v1")) {
                throw new CryptoException("Format de chiffrement invalide", null);
            }

            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Erreur lors du déchiffrement", e);
        }
    }
}