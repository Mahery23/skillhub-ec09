package com.example.authserver.service;

/**
 * Exception levée lors d'une erreur de chiffrement ou déchiffrement AES-GCM.
 *
 * <p>Utilisée par {@link CryptoService} pour signaler toute anomalie
 * lors du chiffrement ou du déchiffrement du mot de passe utilisateur.</p>
 *
 * <p>Exemples de cas déclencheurs :</p>
 * <ul>
 *   <li>Format du mot de passe chiffré invalide (pas {@code v1:iv:ciphertext})</li>
 *   <li>Clé AES incorrecte ou corrompue</li>
 *   <li>Données chiffrées altérées (échec de l'authentification GCM)</li>
 * </ul>
 */
public class CryptoException extends Exception {

    /**
     * Construit une CryptoException avec un message descriptif et la cause originale.
     *
     * @param message description de l'erreur de chiffrement
     * @param cause   exception originale ayant provoqué l'erreur (peut être {@code null})
     */
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}