<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * Représente la notation d'une formation par un apprenant.
 * Un apprenant ne peut noter une formation qu'une seule fois.
 */
class Rating extends Model
{
    protected $fillable = ['user_id', 'formation_id', 'note', 'commentaire'];

    /**
     * Retourne la formation associée à cette notation.
     */
    public function formation(): BelongsTo
    {
        return $this->belongsTo(Formation::class);
    }

    /**
     * Retourne l'utilisateur ayant soumis cette notation.
     */
    public function utilisateur(): BelongsTo
    {
        return $this->belongsTo(User::class, 'user_id');
    }
}