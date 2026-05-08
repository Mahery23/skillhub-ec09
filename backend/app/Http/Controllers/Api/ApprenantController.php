<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Enrollment;
use App\Models\Formation;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Permet au formateur de consulter la liste des apprenants
 * inscrits à ses formations.
 */
class ApprenantController extends Controller
{
    /**
     * Retourne la liste des apprenants inscrits à une formation.
     * GET /api/formations/{id}/apprenants
     * Accès : formateur authentifié et propriétaire de la formation
     */
    public function index(Request $request, Formation $formation): JsonResponse
    {
        $email = $request->attributes->get('auth_email');

        // Vérifier que le formateur est bien propriétaire de la formation
        if ($formation->formateur_id !== $email) {
            return response()->json([
                'message' => 'Vous n\'êtes pas propriétaire de cette formation.',
            ], 403);
        }

        // Récupérer les apprenants inscrits avec leurs informations
        $apprenants = Enrollment::where('formation_id', $formation->id)
            ->with('utilisateur')
            ->get()
            ->map(fn(Enrollment $enrollment) => [
                'id'               => $enrollment->utilisateur?->id,
                'nom'              => $enrollment->utilisateur?->nom,
                'email'            => $enrollment->utilisateur?->email,
                'progression'      => $enrollment->progression,
                'date_inscription' => $enrollment->date_inscription,
            ]);

        return response()->json([
            'apprenants' => $apprenants,
        ]);
    }
}