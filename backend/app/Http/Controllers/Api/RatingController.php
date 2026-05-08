<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Enrollment;
use App\Models\Formation;
use App\Models\Rating;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Gère la notation des formations par les apprenants.
 * Un apprenant ne peut noter qu'une formation à laquelle il est inscrit,
 * et ne peut la noter qu'une seule fois.
 */
class RatingController extends Controller
{
    /**
     * Soumet une note pour une formation.
     * POST /api/formations/{id}/noter
     * Accès : apprenant authentifié et inscrit à la formation
     */
    public function store(Request $request, Formation $formation): JsonResponse
    {
        $email = $request->attributes->get('auth_email');
        $userId = User::where('email', $email)->value('id');

        if (!$userId) {
            return response()->json(['message' => 'Utilisateur introuvable.'], 404);
        }

        // Vérifier que l'apprenant est bien inscrit à cette formation
        $enrolled = Enrollment::where('utilisateur_id', $userId)
            ->where('formation_id', $formation->id)
            ->exists();

        if (!$enrolled) {
            return response()->json([
                'message' => 'Vous devez être inscrit à cette formation pour la noter.',
            ], 403);
        }

        // Valider la note (1 à 5) et le commentaire optionnel
        $validated = $request->validate([
            'note'        => 'required|integer|min:1|max:5',
            'commentaire' => 'nullable|string|max:1000',
        ]);

        // Vérifier qu'il n'a pas déjà noté cette formation
        $alreadyRated = Rating::where('user_id', $userId)
            ->where('formation_id', $formation->id)
            ->exists();

        if ($alreadyRated) {
            return response()->json([
                'message' => 'Vous avez déjà noté cette formation.',
            ], 400);
        }

        // Créer la notation
        $rating = Rating::create([
            'user_id'      => $userId,
            'formation_id' => $formation->id,
            'note'         => $validated['note'],
            'commentaire'  => $validated['commentaire'] ?? null,
        ]);

        return response()->json([
            'message' => 'Note soumise avec succès.',
            'rating'  => $rating,
        ], 201);
    }
}