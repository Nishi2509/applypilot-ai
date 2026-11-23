CREATE TABLE email_templates (
  id BINARY(16) PRIMARY KEY,
  user_id BINARY(16) NOT NULL UNIQUE,
  subject VARCHAR(998) NOT NULL,
  body MEDIUMTEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_email_template_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE master_resumes ADD COLUMN target_roles VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE applications ADD COLUMN master_resume_id BINARY(16) NULL;
ALTER TABLE applications ADD CONSTRAINT fk_application_master_resume FOREIGN KEY (master_resume_id) REFERENCES master_resumes(id);
