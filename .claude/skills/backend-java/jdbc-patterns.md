# Backend Java — Spring JDBC Patterns

**Spring JDBC uniquement** — SQL explicite, controle total, zero magie.

## Conventions

- `JdbcClient` (moderne) ou `JdbcTemplate` (classique)
- RowMapper en methode privee ou lambda
- `Optional` pour requetes single-result
- SQL en text blocks `"""`
- `ON CONFLICT` pour upserts
- Pas d'entite JPA, pas de `@Entity`, pas de `@Table`

## Repository avec JdbcClient (recommande Spring Boot 4.x)

```java
@Repository
public class SpotifyTrackRepository {
    private final JdbcClient jdbcClient;

    public Optional<SpotifyTrackData> findBySpotifyId(String spotifyId) {
        return jdbcClient.sql("""
            SELECT spotify_id, local_file_path, artist, title, bpm,
                   musical_key, release_year, popularity
            FROM spotify_tracks WHERE spotify_id = ?
            """)
            .param(spotifyId)
            .query(this::mapRow)
            .optional();
    }

    public void save(SpotifyTrackData track) {
        jdbcClient.sql("""
            INSERT INTO spotify_tracks (spotify_id, local_file_path, artist, title, bpm,
                musical_key, release_year, popularity)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (spotify_id) DO UPDATE SET
                local_file_path = EXCLUDED.local_file_path, artist = EXCLUDED.artist,
                title = EXCLUDED.title, bpm = EXCLUDED.bpm
            """)
            .params(track.spotifyId(), track.localFilePath(), track.artist(),
                track.title(), track.bpm(), track.musicalKey(),
                track.releaseYear(), track.popularity())
            .update();
    }

    private SpotifyTrackData mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpotifyTrackData(
            rs.getString("spotify_id"), rs.getString("local_file_path"),
            rs.getString("artist"), rs.getString("title"),
            rs.getInt("bpm"), rs.getString("musical_key"),
            rs.getInt("release_year"), rs.getInt("popularity"));
    }
}
```

## Repository avec JdbcTemplate (alternative classique)

```java
@Repository
public class TaggingHistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public Optional<TaggingHistory> findById(long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT id, filepath, operation, status, created_at
                FROM tagging_history WHERE id = ?
                """, this::mapRow, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public long save(TaggingHistory history) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                INSERT INTO tagging_history (filepath, operation, status, created_at)
                VALUES (?, ?, ?, ?)
                """, new String[]{"id"});
            ps.setString(1, history.filepath());
            ps.setString(2, history.operation());
            ps.setString(3, history.status());
            ps.setTimestamp(4, Timestamp.valueOf(history.createdAt()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
```

## Batch Operations

```java
public void saveBatch(List<SpotifyTrackData> tracks) {
    jdbcTemplate.batchUpdate("""
        INSERT INTO spotify_tracks (spotify_id, local_file_path, artist, title, bpm, musical_key)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (spotify_id) DO UPDATE SET
            local_file_path = EXCLUDED.local_file_path, artist = EXCLUDED.artist
        """, tracks, tracks.size(), (ps, track) -> {
        ps.setString(1, track.spotifyId());
        ps.setString(2, track.localFilePath());
        ps.setString(3, track.artist());
        ps.setString(4, track.title());
        ps.setInt(5, track.bpm());
        ps.setString(6, track.musicalKey());
    });
}
```

## Transactions explicites

```java
@Service
public class TrackImportService {
    private final TransactionTemplate txTemplate;

    public void importTrackWithHistory(SpotifyTrackData track) {
        txTemplate.executeWithoutResult(status -> {
            trackRepo.save(track);
            historyRepo.save(new TaggingHistory(
                null, track.localFilePath(), "IMPORT", "COMPLETED", LocalDateTime.now()));
        });
    }
}
```

## Choix JdbcClient vs JdbcTemplate

| Critere             | JdbcClient             | JdbcTemplate           |
|---------------------|------------------------|------------------------|
| API                 | Fluent, moderne        | Classique, callback    |
| `Optional` natif    | `.optional()`          | try/catch manuel       |
| Batch               | Non supporte           | `batchUpdate()`        |
| GeneratedKeys       | Limite                 | `KeyHolder`            |
| Cas simple (CRUD)   | **Prefere**            | Ok                     |
| Cas complexe        | Ok                     | **Prefere**            |

**Regle : `JdbcClient` par defaut, `JdbcTemplate` quand batch/keys/callbacks necessaires.**

## Tests Repository

```java
@JdbcTest
@Import(SpotifyTrackRepository.class)
class SpotifyTrackRepositoryTest {
    @Autowired private SpotifyTrackRepository repository;

    @Test
    void save_thenFindBySpotifyId_shouldReturnTrack() {
        var track = new SpotifyTrackData("spotify:123", "/music/test.mp3",
            "Artist", "Title", 128, "Am", 2024, 85);
        repository.save(track);
        Optional<SpotifyTrackData> found = repository.findBySpotifyId("spotify:123");
        assertThat(found).isPresent();
        assertThat(found.get().artist()).isEqualTo("Artist");
    }
}
```
