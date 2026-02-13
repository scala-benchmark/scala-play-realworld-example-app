# Add legacy fingerprint (MD5) for legacy auth

# --- !Ups

ALTER TABLE security_users ADD COLUMN legacy_fingerprint VARCHAR(64) NULL;

# --- !Downs

ALTER TABLE security_users DROP COLUMN legacy_fingerprint;
