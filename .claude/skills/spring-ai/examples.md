# Spring AI — Exemples Complets

## 1. Creer un Nouveau @Tool Function End-to-End

```java
// --- Dans AyanMusicTools.java (adapter/in/mcp) ---

@Tool(name = "analyzeTrackCompatibility",
      description = "Analyse la compatibilite entre deux morceaux pour le mixage DJ (BPM, tonalite, energie)")
public TrackCompatibilityResult analyzeCompatibility(
    @ToolParam(description = "Chemin absolu du premier fichier audio") String filepathA,
    @ToolParam(description = "Chemin absolu du deuxieme fichier audio") String filepathB
) {
    try {
        MusicFileInfo infoA = audioFileReader.readTags(filepathA);
        MusicFileInfo infoB = audioFileReader.readTags(filepathB);

        int bpmDiff = Math.abs(parseBpm(infoA.bpm()) - parseBpm(infoB.bpm()));
        boolean harmonicMatch = harmonicMixingService.areCompatible(
            infoA.key(), infoB.key());
        double score = computeCompatibilityScore(bpmDiff, harmonicMatch);

        return new TrackCompatibilityResult(
            filepathA, filepathB, bpmDiff, harmonicMatch,
            score, suggestTransition(score));
    } catch (Exception e) {
        return TrackCompatibilityResult.error(e.getMessage());
    }
}

// --- Record resultat (domain/model) ---
public record TrackCompatibilityResult(
    String filepathA, String filepathB,
    int bpmDifference, boolean harmonicMatch,
    double compatibilityScore, String transitionSuggestion
) {
    public static TrackCompatibilityResult error(String message) {
        return new TrackCompatibilityResult(null, null, 0, false, 0, "Erreur: " + message);
    }
}

// --- Enregistrement automatique via AIConfig ---
// Le @Tool est detecte automatiquement car AyanMusicTools est passe
// en defaultTools() dans AIConfig.chatClient()
```

## 2. Structured Output avec Validation

```java
// --- Record pour structured output ---
public record PlaylistSuggestion(
    @JsonProperty("name") @JsonPropertyDescription("Nom de la playlist suggeree")
    String name,

    @JsonProperty("tracks") @JsonPropertyDescription("Liste ordonnee des morceaux")
    List<PlaylistTrack> tracks,

    @JsonProperty("totalDuration") @JsonPropertyDescription("Duree totale en minutes")
    @Min(1) @Max(480) int totalDuration,

    @JsonProperty("genre") @JsonPropertyDescription("Genre principal de la playlist")
    @Pattern(regexp = "^[A-Za-z\\s]+$") String genre,

    @JsonProperty("energy") @JsonPropertyDescription("Niveau d'energie moyen 0.0-1.0")
    @Min(0) @Max(1) double energy
) {
    public record PlaylistTrack(
        @JsonProperty("filepath") String filepath,
        @JsonProperty("order") int order,
        @JsonProperty("transitionNote") String transitionNote
    ) {}
}

// --- Utilisation dans service ---
@Service
public class PlaylistGenerationService {
    private final ChatClient chatClient;

    public PlaylistSuggestion generatePlaylist(List<MusicFileInfo> availableTracks) {
        String tracksContext = availableTracks.stream()
            .map(t -> String.format("- %s by %s (%s BPM, key %s)",
                t.title(), t.artist(), t.bpm(), t.key()))
            .collect(Collectors.joining("\n"));

        return chatClient.prompt()
            .system("Tu es Ayan, DJ expert en creation de playlists harmoniques.")
            .user("""
                Cree une playlist de 1h a partir de ces morceaux :
                %s

                Optimise l'ordre pour des transitions fluides (BPM progressif, cles harmoniques).
                """.formatted(tracksContext))
            .call()
            .entity(PlaylistSuggestion.class);
    }
}
```

## 3. Endpoint WebSocket STOMP

```java
// --- DTO ---
public record PlaylistRequest(String conversationId, List<String> filepaths, int durationMinutes) {}
public record PlaylistResponse(String conversationId, PlaylistSuggestion playlist,
    String agentMessage, LocalDateTime timestamp) {}

// --- Controller WebSocket ---
@Controller
public class PlaylistWebSocketController {
    private final PlaylistGenerationService playlistService;
    private final AudioFileReader audioFileReader;
    private final ConversationHistoryService historyService;

    @MessageMapping("/playlist")
    @SendTo("/topic/playlist-responses")
    public PlaylistResponse handlePlaylistRequest(PlaylistRequest request) {
        String conversationId = request.conversationId() != null
            ? request.conversationId() : UUID.randomUUID().toString();

        // Lire les tags des fichiers
        List<MusicFileInfo> tracks = request.filepaths().stream()
            .map(audioFileReader::readTags)
            .toList();

        // Generer playlist via IA
        PlaylistSuggestion playlist = playlistService.generatePlaylist(tracks);

        // Sauvegarder dans historique conversation
        historyService.saveMessage(conversationId,
            new ChatMessage("assistant",
                "Playlist generee: " + playlist.name(),
                LocalDateTime.now(), Map.of("type", "playlist")));

        return new PlaylistResponse(conversationId, playlist,
            "Voici ta playlist optimisee pour le mix!", LocalDateTime.now());
    }
}

// --- WebSocket config (deja existant, ajouter broker si besoin) ---
// /app/playlist → @MessageMapping("/playlist")
// Reponse sur /topic/playlist-responses

// --- Frontend Angular (appel) ---
// this.stompClient.publish({
//   destination: '/app/playlist',
//   body: JSON.stringify({ conversationId, filepaths, durationMinutes: 60 })
// });
// this.stompClient.subscribe('/topic/playlist-responses', callback);
```
