package com.djtools.ayan.musictagger.domain.exception;

/** Exception métier levée lors d'un problème de lecture/écriture de fichier audio. */
public class AudioProcessingException extends RuntimeException {

    public AudioProcessingException(String message) {
        super(message);
    }

    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
