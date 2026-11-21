CREATE TABLE job_source_configs (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL,
  provider VARCHAR(30) NOT NULL,
  board_slug VARCHAR(255) NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_job_source_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uq_job_source_config UNIQUE(user_id, provider, board_slug)
);
