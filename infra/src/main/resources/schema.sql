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
