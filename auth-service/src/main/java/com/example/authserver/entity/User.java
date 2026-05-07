package com.example.authserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant un utilisateur du système d'authentification.
 *
 * <h2>Stockage du mot de passe</h2>
 * <p>Le mot de passe n'est jamais stocké en clair. Il est chiffré avec AES-GCM
 * via {@link CryptoService} avant persistance. Ce chiffrement réversible est
 * nécessaire pour recalculer le HMAC côté serveur lors du login.</p>
 *
 * <h2>Table</h2>
 * <p>Mappée sur la table {@code auth_users} de la base {@code authdb},
 * séparée de la base applicative Laravel ({@code skillhub_db}).</p>
 */
@Entity
@Table(name = "auth_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** Identifiant technique auto-incrémenté. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Adresse email — identifiant métier unique de l'utilisateur. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Nom complet de l'utilisateur. */
    @Column(nullable = false)
    private String name;

    /**
     * Rôle de l'utilisateur dans la plateforme SkillHub.
     * Valeurs autorisées : {@code "apprenant"} ou {@code "formateur"}.
     */
    @Column(nullable = false)
    private String role = "apprenant";

    /**
     * Mot de passe chiffré en AES-GCM, encodé au format {@code v1:Base64(iv):Base64(ciphertext)}.
     * Ne jamais exposer ce champ dans une réponse API.
     */
    @Column(name = "password_encrypted", nullable = false)
    private String passwordEncrypted;

    /** Date et heure de création du compte (UTC). */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}