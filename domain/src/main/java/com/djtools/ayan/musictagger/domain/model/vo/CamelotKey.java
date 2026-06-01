package com.djtools.ayan.musictagger.domain.model.vo;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Position sur la roue de Camelot (mixage harmonique « Mixed In Key »).
 * {@code number} 1–12, {@code letter} 'A' (mineur) ou 'B' (majeur). Code : ex "8A", "12B".
 *
 * <p>Deux clés sont compatibles si elles sont identiques, adjacentes sur la roue (±1, même lettre),
 * ou relatives majeure/mineure (même numéro, lettre opposée).
 */
public record CamelotKey(int number, char letter) {

    /** Index pitch class (0=C … 11=B) → numéro Camelot pour les tonalités MAJEURES (lettre B). */
    private static final int[] MAJOR_NUMBER = {8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1};
    /** Index pitch class (0=C … 11=B) → numéro Camelot pour les tonalités MINEURES (lettre A). */
    private static final int[] MINOR_NUMBER = {5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10};

    /** Note (normalisée majuscule, sharps + alias bémols) → index pitch class 0–11. */
    private static final Map<String, Integer> PITCH_CLASS = Map.ofEntries(
            Map.entry("C", 0), Map.entry("B#", 0),
            Map.entry("C#", 1), Map.entry("DB", 1),
            Map.entry("D", 2),
            Map.entry("D#", 3), Map.entry("EB", 3),
            Map.entry("E", 4), Map.entry("FB", 4),
            Map.entry("F", 5), Map.entry("E#", 5),
            Map.entry("F#", 6), Map.entry("GB", 6),
            Map.entry("G", 7),
            Map.entry("G#", 8), Map.entry("AB", 8),
            Map.entry("A", 9),
            Map.entry("A#", 10), Map.entry("BB", 10),
            Map.entry("B", 11), Map.entry("CB", 11)
    );

    public CamelotKey {
        if (number < 1 || number > 12) {
            throw new IllegalArgumentException("Camelot number must be 1–12: " + number);
        }
        letter = Character.toUpperCase(letter);
        if (letter != 'A' && letter != 'B') {
            throw new IllegalArgumentException("Camelot letter must be A or B: " + letter);
        }
    }

    /** Code Camelot, ex "8A". */
    public String code() {
        return number + String.valueOf(letter);
    }

    /**
     * Construit une clé Camelot à partir d'une tonalité (ex "C#", "Db") et d'un mode ("major"/"minor").
     * Retourne {@link Optional#empty()} si la tonalité ou le mode est absent/non reconnu.
     */
    public static Optional<CamelotKey> from(String musicalKey, String mode) {
        if (musicalKey == null || musicalKey.isBlank() || mode == null || mode.isBlank()) {
            return Optional.empty();
        }
        final var normalizedNote = musicalKey.trim()
                .replace('♯', '#')
                .replace('♭', 'b')
                .toUpperCase(java.util.Locale.ROOT);
        final var pitchClass = PITCH_CLASS.get(normalizedNote);
        if (pitchClass == null) {
            return Optional.empty();
        }
        final var normalizedMode = mode.trim().toLowerCase(java.util.Locale.ROOT);
        final boolean major = normalizedMode.startsWith("maj");
        final boolean minor = normalizedMode.startsWith("min");
        if (!major && !minor) {
            return Optional.empty();
        }
        return major
                ? Optional.of(new CamelotKey(MAJOR_NUMBER[pitchClass], 'B'))
                : Optional.of(new CamelotKey(MINOR_NUMBER[pitchClass], 'A'));
    }

    /** Dérive la clé Camelot depuis des {@link AudioFeatures} (musicalKey + mode). */
    public static Optional<CamelotKey> fromAudioFeatures(AudioFeatures features) {
        if (features == null) {
            return Optional.empty();
        }
        return from(features.musicalKey(), features.mode());
    }

    /**
     * Clés compatibles pour une transition harmonique :
     * elle-même, +1 et −1 sur la roue (même lettre), et la relative (mode swap).
     */
    public List<CamelotKey> compatible() {
        final List<CamelotKey> result = new ArrayList<>(4);
        result.add(this);
        result.add(new CamelotKey(number % 12 + 1, letter));          // +1 (énergie up)
        result.add(new CamelotKey(number == 1 ? 12 : number - 1, letter)); // -1 (énergie down)
        result.add(new CamelotKey(number, letter == 'A' ? 'B' : 'A')); // relative majeure/mineure
        return result;
    }

    public boolean isCompatibleWith(CamelotKey other) {
        return other != null && compatible().contains(other);
    }
}
