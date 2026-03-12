# Spring AI — Harmonic Mixing & Camelot Wheel

## Harmonic Mixing Playlist

```java
@Tool(
    name = "generateHarmonicMixedPlaylist",
    description = "Genere playlist 50 tracks mixed in key via Camelot Wheel"
)
public HarmonicPlaylist generateHarmonicMixedPlaylist(
    @ToolParam(description = "Genre principal") String mainGenre,
    @ToolParam(description = "Sous-genre optionnel") String subGenre,
    @ToolParam(description = "BPM minimum") Integer minBpm,
    @ToolParam(description = "BPM maximum") Integer maxBpm,
    @ToolParam(description = "Energy level 0.0-1.0") Double targetEnergy
) {
    List<SpotifyTrackData> candidates = vectorStore.findSimilarWithFilters(
        buildGenreQuery(mainGenre, subGenre, targetEnergy), 200,
        Map.of("genre", mainGenre, "minTempo", minBpm, "maxTempo", maxBpm,
               "minEnergy", targetEnergy - 0.1, "maxEnergy", targetEnergy + 0.1));

    if (candidates.size() < 50)
        throw new IllegalStateException("Pas assez tracks: " + candidates.size());

    List<PlaylistTrack> playlist = buildHarmonicSequence(candidates, 50, targetEnergy);
    return new HarmonicPlaylist("Harmonic Mix - " + mainGenre, playlist,
        calculateStats(playlist), LocalDateTime.now());
}
```

## Camelot Wheel

```java
private static final Map<String, String> CAMELOT_WHEEL = Map.ofEntries(
    entry("C Major", "8B"), entry("A Minor", "8A"),
    entry("G Major", "9B"), entry("E Minor", "9A"),
    entry("D Major", "10B"), entry("B Minor", "10A"),
    entry("A Major", "11B"), entry("F# Minor", "11A"),
    entry("E Major", "12B"), entry("C# Minor", "12A"),
    entry("B Major", "1B"), entry("G# Minor", "1A"),
    entry("F# Major", "2B"), entry("D# Minor", "2A"),
    entry("Db Major", "3B"), entry("Bb Minor", "3A"),
    entry("Ab Major", "4B"), entry("F Minor", "4A"),
    entry("Eb Major", "5B"), entry("C Minor", "5A"),
    entry("Bb Major", "6B"), entry("G Minor", "6A"),
    entry("F Major", "7B"), entry("D Minor", "7A")
);
```

## Regles Camelot Wheel

```java
private List<String> getCompatibleKeys(String camelotKey) {
    String number = camelotKey.substring(0, camelotKey.length() - 1);
    String letter = camelotKey.substring(camelotKey.length() - 1);
    int num = Integer.parseInt(number);

    List<String> compatible = new ArrayList<>();
    compatible.add(camelotKey);                              // Meme cle (safest)
    compatible.add((num % 12 + 1) + letter);                // +1 (energy up)
    compatible.add((num == 1 ? 12 : num - 1) + letter);    // -1 (energy down)
    compatible.add(number + (letter.equals("A") ? "B" : "A")); // Mode swap
    return compatible;
}
```

## Sequence Harmonique

```java
private List<PlaylistTrack> buildHarmonicSequence(
    List<SpotifyTrackData> candidates, int targetCount, double targetEnergy
) {
    Map<String, List<SpotifyTrackData>> byKey = candidates.stream()
        .filter(t -> getCamelotKey(t) != null)
        .collect(Collectors.groupingBy(this::getCamelotKey));

    List<PlaylistTrack> playlist = new ArrayList<>();
    String currentKey = selectStartingKey(byKey, targetEnergy);
    SpotifyTrackData current = selectBestTrack(byKey.get(currentKey), targetEnergy);
    playlist.add(new PlaylistTrack(current, 1, currentKey, null, 0.0));

    for (int i = 1; i < targetCount; i++) {
        List<String> compatibleKeys = getCompatibleKeys(currentKey);
        String nextKey = selectNextKey(compatibleKeys, byKey, playlist);
        if (nextKey == null) nextKey = currentKey;

        List<SpotifyTrackData> available = byKey.getOrDefault(nextKey, List.of()).stream()
            .filter(t -> !isAlreadyInPlaylist(t, playlist)).toList();
        if (available.isEmpty()) continue;

        SpotifyTrackData next = selectBestTrack(available, targetEnergy);
        String transitionType = getTransitionType(currentKey, nextKey);
        double quality = calculateTransitionQuality(playlist.get(i - 1).track(), next);
        playlist.add(new PlaylistTrack(next, i + 1, nextKey, transitionType, quality));
        currentKey = nextKey;
    }
    return playlist;
}

private String getTransitionType(String fromKey, String toKey) {
    if (fromKey.equals(toKey)) return "PERFECT_MATCH";
    String fromLetter = fromKey.substring(fromKey.length() - 1);
    String toLetter = toKey.substring(toKey.length() - 1);
    if (!fromLetter.equals(toLetter)) return "MODE_CHANGE";
    int diff = Math.abs(Integer.parseInt(toKey.substring(0, toKey.length() - 1))
        - Integer.parseInt(fromKey.substring(0, fromKey.length() - 1)));
    return (diff == 1 || diff == 11) ? "ADJACENT_KEY" : "JUMP";
}

private double calculateTransitionQuality(SpotifyTrackData from, SpotifyTrackData to) {
    double bpmDiff = Math.abs(from.audioFeatures().tempo() - to.audioFeatures().tempo());
    double bpmScore = bpmDiff <= 6 ? 1.0 : Math.max(0, 1.0 - ((bpmDiff - 6) / 10.0));
    double energyScore = Math.max(0, 1.0 - Math.abs(
        from.audioFeatures().energy() - to.audioFeatures().energy()));
    return (bpmScore * 0.4 + energyScore * 0.3 + 0.3);
}
```

## Records

```java
public record HarmonicPlaylist(
    String name, List<PlaylistTrack> tracks, PlaylistStats stats, LocalDateTime createdAt
) {}

public record PlaylistTrack(
    SpotifyTrackData track, int position, String camelotKey,
    String transitionType, double transitionQuality
) {}

public record PlaylistStats(
    int totalTracks, double avgBpm, double avgEnergy,
    double avgTransitionQuality, Map<String, Long> keyDistribution,
    long perfectTransitions
) {
    public double harmonicCompatibility() { return avgTransitionQuality * 100; }
}
```
