# Spotify Integration — Reference Rapide

## Endpoints API Utilises

| Endpoint | Methode | Usage |
|----------|---------|-------|
| `/v1/search?q=&type=track&limit=` | GET | Recherche tracks |
| `/v1/audio-features/{id}` | GET | Danceability, energy, tempo, key... |
| `/v1/tracks/{id}` | GET | Details track (album, popularity...) |
| `/v1/artists/{id}` | GET | Genres, popularite artiste |
| `/v1/recommendations?seed_tracks=&seed_artists=` | GET | Recommendations |
| `https://accounts.spotify.com/api/token` | POST | OAuth2 client_credentials |

## SpotifyAudioFeatures — Champs et Ranges

| Champ | Type | Range | Description |
|-------|------|-------|-------------|
| `danceability` | double | 0.0–1.0 | Aptitude a la danse |
| `energy` | double | 0.0–1.0 | Intensite/activite |
| `speechiness` | double | 0.0–1.0 | Presence de paroles |
| `acousticness` | double | 0.0–1.0 | Probabilite acoustique |
| `instrumentalness` | double | 0.0–1.0 | Probabilite instrumentale |
| `liveness` | double | 0.0–1.0 | Probabilite live |
| `valence` | double | 0.0–1.0 | Positivite musicale (mood) |
| `loudness` | double | -60–0 dB | Volume moyen |
| `tempo` | double | BPM | Battements par minute |
| `key` | int | 0–11 | Tonalite (pitch class) |
| `mode` | int | 0 ou 1 | Mineur (0) / Majeur (1) |
| `time_signature` | int | 3–7 | Signature rythmique |
| `duration_ms` | int | ms | Duree en millisecondes |

## Musical Key Mapping

| key | Note | key | Note |
|-----|------|-----|------|
| 0 | C | 6 | F# |
| 1 | C# | 7 | G |
| 2 | D | 8 | G# |
| 3 | D# | 9 | A |
| 4 | E | 10 | A# |
| 5 | F | 11 | B |

- `mode = 1` → Major, `mode = 0` → Minor
- Full key : `KEYS[key] + " " + (mode == 1 ? "Major" : "Minor")`

## Error Codes Spotify

| Code | Exception | Action |
|------|-----------|--------|
| 401 | `SpotifyAuthException` | Token expire → refresh automatique |
| 403 | `SpotifyApiException` | Acces interdit (scope manquant) |
| 429 | `SpotifyRateLimitException` | Rate limit → `Retry-After` header |
| 404 | `SpotifyApiException` | Track/artist non trouve |

## Config Keys (`spotify.*`)

```yaml
spotify:
  client-id: ${SPOTIFY_CLIENT_ID}
  client-secret: ${SPOTIFY_CLIENT_SECRET}
  auth-url: https://accounts.spotify.com/api/token
  rate-limit:
    requests-per-second: 10
    cache-ttl-minutes: 60
```

## Cache et Rate Limit

| Parametre | Valeur | Implementation |
|-----------|--------|----------------|
| Cache max entries | 10 000 | Caffeine `maximumSize` |
| Cache TTL | 60 min (configurable) | Caffeine `expireAfterWrite` |
| Cache key | `"artist:title".toLowerCase()` | Concatenation normalisee |
| Rate limit | 10 req/s (configurable) | Guava `RateLimiter.create()` |
| Token cache | Expire 60s avant expiry reelle | `Instant.now().plusSeconds(expires_in - 60)` |

## Records Spotify (Principaux)

| Record | Champs cles |
|--------|-------------|
| `SpotifySearchResponse` | `tracks` (SpotifySearchTracks) |
| `SpotifySearchTracks` | `items`, `total`, `limit`, `offset` |
| `SpotifyTrackItem` | `id`, `name`, `artists`, `album`, `duration_ms`, `popularity` |
| `SpotifyArtistItem` | `id`, `name`, `type` |
| `SpotifyAlbum` | `id`, `name`, `release_date`, `images`, `album_type` |
| `SpotifyAudioFeatures` | Voir tableau ci-dessus |
| `SpotifyArtist` | `id`, `name`, `genres`, `popularity`, `followers` |

## EnrichedTrackMetadata (14 champs)

`sourceId`, `artist`, `title`, `album`, `genres`, `styles`, `label`, `country`, `isrc`, `tags`, `releaseYear`, `popularity`, `durationMs`, `audioFeatures`
