CREATE TABLE site
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    url            TEXT    NOT NULL,
    name           TEXT,
    thumbnail_uuid TEXT,
    enabled        INTEGER NOT NULL DEFAULT 1,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NOT NULL
);

CREATE UNIQUE INDEX idx_site_url ON site (url);
