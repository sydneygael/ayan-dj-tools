CREATE TABLE IF NOT EXISTS scanned_tracks (
    filepath      VARCHAR(1000) PRIMARY KEY,
    filename      VARCHAR(255),
    artist        VARCHAR(255),
    title         VARCHAR(255),
    album         VARCHAR(255),
    genre         VARCHAR(255),
    bpm           VARCHAR(50),
    key_name      VARCHAR(50),
    file_size     BIGINT NOT NULL,
    last_modified BIGINT NOT NULL,
    scanned_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Persistance durable des métadonnées enrichies (Soundcharts + fallback Spotify).
-- Clé logique : artiste + titre normalisés. Listes & audio_features stockés en JSON (TEXT).
CREATE TABLE IF NOT EXISTS enriched_track_metadata (
    artist_key     VARCHAR(512) NOT NULL,
    title_key      VARCHAR(512) NOT NULL,
    source_id      VARCHAR(255),
    artist         VARCHAR(512),
    title          VARCHAR(512),
    album          VARCHAR(512),
    genres         TEXT,
    styles         TEXT,
    label          VARCHAR(255),
    country        VARCHAR(255),
    isrc           VARCHAR(64),
    tags           TEXT,
    release_year   INTEGER,
    popularity     INTEGER,
    duration_ms    BIGINT,
    audio_features TEXT,
    fetched_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (artist_key, title_key)
);
