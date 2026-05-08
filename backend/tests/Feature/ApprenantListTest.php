<?php

namespace Tests\Feature;

use App\Models\Enrollment;
use App\Models\Formation;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Tests de la fonctionnalité liste des apprenants inscrits (vue formateur).
 */
class ApprenantListTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Formateur propriétaire → 200 avec la liste des apprenants.
     */
    public function test_formateur_proprietaire_voit_les_apprenants(): void
    {
        $formateur = User::factory()->formateur()->create();
        $formation = Formation::factory()->create(['formateur_id' => $formateur->email]);
        $apprenant = User::factory()->apprenant()->create();

        Enrollment::factory()->create([
            'utilisateur_id' => $apprenant->id,
            'formation_id'   => $formation->id,
        ]);

        $response = $this->withSpringAuth($formateur->email, 'formateur')
            ->getJson("/api/formations/{$formation->id}/apprenants");

        $response->assertStatus(200)
            ->assertJsonStructure([
                'apprenants' => [
                    '*' => ['id', 'nom', 'email', 'progression', 'date_inscription']
                ]
            ]);
    }

    /**
     * Formateur non propriétaire → 403.
     */
    public function test_formateur_non_proprietaire_recoit_403(): void
    {
        $formateur = User::factory()->formateur()->create();
        $autreFormateur = User::factory()->formateur()->create();
        $formation = Formation::factory()->create(['formateur_id' => $autreFormateur->email]);

        $response = $this->withSpringAuth($formateur->email, 'formateur')
            ->getJson("/api/formations/{$formation->id}/apprenants");

        $response->assertStatus(403);
    }

    /**
     * Formation sans apprenant → 200 avec tableau vide.
     */
    public function test_formation_sans_apprenant_retourne_tableau_vide(): void
    {
        $formateur = User::factory()->formateur()->create();
        $formation = Formation::factory()->create(['formateur_id' => $formateur->email]);

        $response = $this->withSpringAuth($formateur->email, 'formateur')
            ->getJson("/api/formations/{$formation->id}/apprenants");

        $response->assertStatus(200)
            ->assertJson(['apprenants' => []]);
    }

    /**
     * Requête sans token JWT → 401.
     */
    public function test_sans_token_jwt_retourne_401(): void
    {
        $formation = Formation::factory()->create();

        $response = $this->getJson("/api/formations/{$formation->id}/apprenants");

        $response->assertStatus(401);
    }
}