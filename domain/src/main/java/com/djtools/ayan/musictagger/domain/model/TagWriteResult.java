package com.djtools.ayan.musictagger.domain.model;

/** Résultat de l'écriture de tags dans un fichier : APPLIED ou ERROR + message. */
public record TagWriteResult(String filepath, OperationStatus status, String message) {}
