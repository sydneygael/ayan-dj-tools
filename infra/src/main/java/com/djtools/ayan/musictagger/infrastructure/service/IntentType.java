package com.djtools.ayan.musictagger.infrastructure.service;

public enum IntentType {
    /** Scanner, analyser, enrichir des fichiers, tags, BPM, parcourir dossiers */
    FICHIERS,
    /** Créer/appliquer un plan de tags, mode manuel, prévisualiser */
    PLANIFICATION,
    /** Chercher par similarité, genre, BPM, énergie, ambiance */
    RECHERCHE,
    /** Générer une playlist loop mixing ou harmonique Camelot */
    PLAYLIST,
    /** Infos sur un artiste, album, morceau via sources externes */
    DECOUVERTE,
    /** Salutations, questions générales sur les capacités d'Ayan */
    GENERAL
}
