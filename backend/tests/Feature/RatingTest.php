<?php

namespace Tests\Feature;

use App\Models\Enrollment;
use App\Models\Formation;
use App\Models\Rating;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Tests de la fonctionnalité de notation des formations.
 */
class RatingTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Un apprenant inscrit soumet une note valide → 201.
     */
    public function test_apprenant_inscrit_peut_noter(): void
    {
        $apprenant = User::factory()->apprenant()->create();
        $formation = Formation::factory()->create();

        // Inscrire l'apprenant à la formation
        Enrollment::factory()->create([
            'utilisateur_id' => $apprenant->id,
            'formation_id'   => $formation->id,
        ]);

        $response = $this->withSpringAuth($apprenant->email, 'apprenant')
            ->postJson("/api/formations/{$formation->id}/noter", [
                'note'        => 4,
                'commentaire' => 'Très bonne formation',
            ]);

        $response->assertStatus(201);
        $this->assertDatabaseHas('ratings', [
            'user_id'      => $apprenant->id,
            'formation_id' => $formation->id,
            'note'         => 4,
        ]);
    }

    /**
     * Le même apprenant tente de noter une deuxième fois → 400.
     */
    public function test_apprenant_ne_peut_pas_noter_deux_fois(): void
    {
        $apprenant = User::factory()->apprenant()->create();
        $formation = Formation::factory()->create();

        Enrollment::factory()->create([
            'utilisateur_id' => $apprenant->id,
            'formation_id'   => $formation->id,
        ]);

        Rating::create([
            'user_id'      => $apprenant->id,
            'formation_id' => $formation->id,
            'note'         => 3,
        ]);

        $response = $this->withSpringAuth($apprenant->email, 'apprenant')
            ->postJson("/api/formations/{$formation->id}/noter", [
                'note' => 5,
            ]);

        $response->assertStatus(400);
    }

    /**
     * Note hors intervalle (0 ou 6) → 422.
     */
    public function test_note_hors_intervalle(): void
    {
        $apprenant = User::factory()->apprenant()->create();
        $formation = Formation::factory()->create();

        Enrollment::factory()->create([
            'utilisateur_id' => $apprenant->id,
            'formation_id'   => $formation->id,
        ]);

        $response = $this->withSpringAuth($apprenant->email, 'apprenant')
            ->postJson("/api/formations/{$formation->id}/noter", [
                'note' => 6,
            ]);

        $response->assertStatus(422);
    }

    /**
     * Apprenant non inscrit à la formation → 403.
     */
    public function test_apprenant_non_inscrit_ne_peut_pas_noter(): void
    {
        $apprenant = User::factory()->apprenant()->create();
        $formation = Formation::factory()->create();

        $response = $this->withSpringAuth($apprenant->email, 'apprenant')
            ->postJson("/api/formations/{$formation->id}/noter", [
                'note' => 4,
            ]);

        $response->assertStatus(403);
    }

    /**
     * Requête sans token JWT → 401.
     */
    public function test_sans_token_jwt_retourne_401(): void
    {
        $formation = Formation::factory()->create();

        $response = $this->postJson("/api/formations/{$formation->id}/noter", [
            'note' => 4,
        ]);

        $response->assertStatus(401);
    }
}