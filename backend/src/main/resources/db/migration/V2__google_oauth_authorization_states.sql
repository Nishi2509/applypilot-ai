CREATE TABLE oauth_authorization_states (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  state_hash CHAR(64) NOT NULL UNIQUE,
  provider VARCHAR(50) NOT NULL,
  requested_scopes VARCHAR(2000) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_oauth_state_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX ix_oauth_state_expiry (expires_at)
);
