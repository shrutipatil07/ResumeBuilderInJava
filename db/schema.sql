-- ============================================================
-- Resume Builder — MySQL Database Schema (Normalized)
-- ============================================================

CREATE DATABASE IF NOT EXISTS resume_builder;
USE resume_builder;

-- ============================================================
-- 1. users
-- ============================================================
-- WHY: Central entity. Every resume belongs to a user.
--      Stores identity & contact info that is shared across
--      all of a user's resumes (name, email, phone).
--      The email column doubles as the login identifier,
--      and password_hash supports future authentication.
-- ============================================================
CREATE TABLE users (
    user_id       INT            AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)   NOT NULL,
    email         VARCHAR(150)   NOT NULL UNIQUE,
    phone         VARCHAR(15)    NOT NULL,
    password_hash VARCHAR(255)   NULL     COMMENT 'BCrypt hash; NULL until auth is implemented',
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- 2. resumes
-- ============================================================
-- WHY: A user can create many resumes (fresher vs experienced,
--      or tailored to different jobs). This table holds the
--      per-resume metadata: title, type, and the objective
--      statement — all of which can differ between resumes.
-- ============================================================
CREATE TABLE resumes (
    resume_id       INT            AUTO_INCREMENT PRIMARY KEY,
    user_id         INT            NOT NULL,
    title           VARCHAR(150)   NOT NULL      COMMENT 'e.g. "Backend Developer Resume"',
    resume_type     ENUM('FRESHER','EXPERIENCED') NOT NULL,
    objective       TEXT           NULL           COMMENT 'Career objective / summary',
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 3. education
-- ============================================================
-- WHY: A user can hold multiple degrees (10th, 12th, B.Tech,
--      M.Tech, etc.). Storing them in a separate table avoids
--      cramming everything into a single text column — which
--      your current User model does — and lets each resume
--      pick which degrees to show.
-- ============================================================
CREATE TABLE education (
    education_id    INT            AUTO_INCREMENT PRIMARY KEY,
    resume_id       INT            NOT NULL,
    institution     VARCHAR(200)   NOT NULL,
    degree          VARCHAR(150)   NOT NULL      COMMENT 'e.g. "B.Tech Computer Science"',
    field_of_study  VARCHAR(150)   NULL,
    start_year      YEAR           NOT NULL,
    end_year        YEAR           NULL           COMMENT 'NULL = currently pursuing',
    grade           VARCHAR(20)    NULL           COMMENT 'CGPA or percentage',
    display_order   TINYINT        DEFAULT 0     COMMENT 'Controls rendering order on the PDF',

    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 4. skills
-- ============================================================
-- WHY: Your current model stores skills as an ArrayList<String>.
--      A dedicated table removes duplicates across users
--      (e.g. "Java" stored once, shared by many) and makes
--      skill-based searching possible.
-- ============================================================
CREATE TABLE skills (
    skill_id    INT            AUTO_INCREMENT PRIMARY KEY,
    skill_name  VARCHAR(100)   NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ============================================================
-- 5. resume_skills  (junction / bridge table)
-- ============================================================
-- WHY: Many-to-many relationship — one resume can list many
--      skills, and the same skill can appear on many resumes.
--      proficiency_level is optional metadata per association.
-- ============================================================
CREATE TABLE resume_skills (
    resume_id         INT        NOT NULL,
    skill_id          INT        NOT NULL,
    proficiency_level ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT') NULL,
    display_order     TINYINT    DEFAULT 0,

    PRIMARY KEY (resume_id, skill_id),
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id)  REFERENCES skills(skill_id)   ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 6. experience
-- ============================================================
-- WHY: An experienced user can have multiple jobs. Normalizing
--      each position into its own row lets the app render a
--      proper work-history timeline. For fresher resumes this
--      table will simply be empty — no NULLs, no wasted space.
-- ============================================================
CREATE TABLE experience (
    experience_id   INT            AUTO_INCREMENT PRIMARY KEY,
    resume_id       INT            NOT NULL,
    company_name    VARCHAR(200)   NOT NULL,
    job_title       VARCHAR(150)   NOT NULL,
    location        VARCHAR(150)   NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE           NULL           COMMENT 'NULL = currently working',
    description     TEXT           NULL           COMMENT 'Responsibilities / achievements',
    display_order   TINYINT        DEFAULT 0,

    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 7. projects
-- ============================================================
-- WHY: Both fresher and experienced resumes showcase projects.
--      A separate table lets users list multiple projects with
--      individual descriptions, tech stacks, and links —
--      instead of one flat text field.
-- ============================================================
CREATE TABLE projects (
    project_id      INT            AUTO_INCREMENT PRIMARY KEY,
    resume_id       INT            NOT NULL,
    project_name    VARCHAR(200)   NOT NULL,
    description     TEXT           NULL,
    tech_stack      VARCHAR(300)   NULL           COMMENT 'e.g. "Java, MySQL, Spring Boot"',
    project_url     VARCHAR(500)   NULL           COMMENT 'GitHub / live link',
    display_order   TINYINT        DEFAULT 0,

    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 8. certifications
-- ============================================================
-- WHY: A user can earn many certifications. Normalizing them
--      lets you store the issuing organization, date, and a
--      credential URL — far richer than a single text column.
-- ============================================================
CREATE TABLE certifications (
    certification_id   INT            AUTO_INCREMENT PRIMARY KEY,
    resume_id          INT            NOT NULL,
    certification_name VARCHAR(200)   NOT NULL,
    issuing_org        VARCHAR(200)   NULL         COMMENT 'e.g. "Coursera", "AWS"',
    issue_date         DATE           NULL,
    credential_url     VARCHAR(500)   NULL,
    display_order      TINYINT        DEFAULT 0,

    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 9. generated_resumes
-- ============================================================
-- WHY: Every time a user clicks "Generate Resume" the app
--      produces a PDF. This table logs each generation event,
--      stores the file path, and records which format was used.
--      It decouples the "resume data" from the "export history"
--      so you never lose track of previously generated files.
-- ============================================================
CREATE TABLE generated_resumes (
    generation_id   INT            AUTO_INCREMENT PRIMARY KEY,
    resume_id       INT            NOT NULL,
    file_path       VARCHAR(500)   NOT NULL       COMMENT 'Path or URL to the generated PDF',
    file_format     ENUM('PDF','DOCX')  DEFAULT 'PDF',
    generated_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Indexes for common query patterns
-- ============================================================
CREATE INDEX idx_resumes_user     ON resumes(user_id);
CREATE INDEX idx_education_resume ON education(resume_id);
CREATE INDEX idx_experience_resume ON experience(resume_id);
CREATE INDEX idx_projects_resume  ON projects(resume_id);
CREATE INDEX idx_certs_resume     ON certifications(resume_id);
CREATE INDEX idx_generated_resume ON generated_resumes(resume_id);
