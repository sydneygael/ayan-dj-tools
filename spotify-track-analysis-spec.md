# Spotify Track Analysis Service -- Technical Specification

## 🎯 Objective

Implement a backend service that:

1.  Searches for a track using **name + artist**
2.  Retrieves **Spotify audio features**
3.  Transforms the data:
    -   Convert metrics to `/100`
    -   Convert `key + mode` to musical notation
    -   Convert to Camelot notation
4.  Returns a formatted JSON object usable by a frontend

------------------------------------------------------------------------

# 1️⃣ Architecture

## Type

REST Backend Service

## External Dependency

-   Spotify Web API
-   OAuth2 (Client Credentials Flow)

## Global Flow

Client → Backend → Spotify Search API\
                      ↓\
                  Spotify Audio Features\
                  ↓\
                Data Transformation\
                ↓\
                Formatted JSON Response

------------------------------------------------------------------------

# 2️⃣ Spotify Authentication

## Endpoint

POST https://accounts.spotify.com/api/token

## Grant Type

grant_type=client_credentials

## Headers

Authorization: Basic base64(client_id:client_secret)\
Content-Type: application/x-www-form-urlencoded

## Response

``` json
{
  "access_token": "string",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### Requirements

-   Cache access token
-   Auto-refresh before expiration

------------------------------------------------------------------------

# 3️⃣ Track Search

## Endpoint

GET https://api.spotify.com/v1/search

## Query Parameters

q=\<track name + artist\>\
type=track\
limit=1

## Required Fields from Response

-   track.id
-   track.popularity
-   track.duration_ms

------------------------------------------------------------------------

# 4️⃣ Audio Features Retrieval

## Endpoint

GET https://api.spotify.com/v1/audio-features/{id}

## Example Response

``` json
{
  "danceability": 0.91,
  "energy": 0.62,
  "key": 1,
  "loudness": -7.0,
  "mode": 0,
  "speechiness": 0.10,
  "acousticness": 0.00,
  "instrumentalness": 0.00,
  "liveness": 0.10,
  "valence": 0.42,
  "tempo": 150.0,
  "duration_ms": 177000
}
```

------------------------------------------------------------------------

# 5️⃣ Business Transformations

## 5.1 Convert Scores

Spotify values range from 0 to 1.

Rule:

    display_score = round(value * 100)

------------------------------------------------------------------------

## 5.2 Musical Key Mapping

### Key Mapping

  key   Note
  ----- -------
  0     C
  1     C#/Db
  2     D
  3     D#/Eb
  4     E
  5     F
  6     F#/Gb
  7     G
  8     G#/Ab
  9     A
  10    A#/Bb
  11    B

### Mode Mapping

  mode   Scale
  ------ -------
  1      Major
  0      Minor

Example: key=1, mode=0 → C#/Db Minor

------------------------------------------------------------------------

## 5.3 Camelot Mapping

Static lookup table required.

Example:

  Key           Camelot
  ------------- ---------
  C Minor       5A
  C#/Db Minor   12A
  D Minor       7A
  C Major       8B
  C#/Db Major   3B

------------------------------------------------------------------------

# 6️⃣ Expected Output JSON

``` json
{
  "length": "2:57",
  "tempo": 150,
  "loudness": -7,
  "key": "C#/Db Minor",
  "camelot": "12A",
  "popularity": 85,
  "danceability": 91,
  "energy": 62,
  "happiness": 42,
  "acousticness": 0,
  "instrumentalness": 0,
  "liveness": 10,
  "speechiness": 10
}
```

------------------------------------------------------------------------

# 7️⃣ Internal API Proposal

GET /api/analyze?track=Blinding Lights&artist=The Weeknd

Response → formatted JSON (see above)

------------------------------------------------------------------------

# 8️⃣ Error Handling

  Case                       Behavior
  -------------------------- --------------------
  No search result           404
  Expired token              Auto refresh
  Spotify rate limit (429)   Retry with backoff
  Missing audio features     422

------------------------------------------------------------------------

# 9️⃣ Technical Constraints

-   OAuth token caching
-   Timeout \< 5s
-   Rate limit handling
-   Secure environment variables
-   Logging of Spotify errors

------------------------------------------------------------------------

# 🔟 Future Improvements

-   Support Spotify URL input
-   Batch analysis
-   Audio-analysis deep data
-   Persistence layer
-   Search history
