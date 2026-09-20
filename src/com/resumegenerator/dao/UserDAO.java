package com.resumegenerator.dao;

// ---------------------------------------------------------------
// Connection — the live session with MySQL that we get from
// DatabaseManager.getConnection(). Every SQL operation runs
// through this object.
// ---------------------------------------------------------------
import java.sql.Connection;

// ---------------------------------------------------------------
// PreparedStatement — a pre-compiled SQL template with
// placeholders (?). It is safer and faster than concatenating
// user input directly into a SQL string.
//
// WHY NOT Statement?
// Statement builds SQL by string concatenation:
//   "INSERT INTO users VALUES('" + name + "')"
// If name = "'; DROP TABLE users; --", you get SQL injection.
// PreparedStatement escapes all input automatically.
// ---------------------------------------------------------------
import java.sql.PreparedStatement;

// ---------------------------------------------------------------
// ResultSet — holds the rows returned by a SELECT query.
// Here we use it to retrieve the auto-generated user_id
// after an INSERT.
// ---------------------------------------------------------------
import java.sql.ResultSet;

// ---------------------------------------------------------------
// Statement — we only import this for the constant
// Statement.RETURN_GENERATED_KEYS, which tells the driver
// to return the auto-increment ID after INSERT.
// We do NOT use Statement to execute queries.
// ---------------------------------------------------------------
import java.sql.Statement;

// ---------------------------------------------------------------
// SQLException — checked exception for any JDBC failure
// (bad SQL syntax, constraint violation, connection lost, etc.).
// ---------------------------------------------------------------
import java.sql.SQLException;

// ---------------------------------------------------------------
// ArrayList — needed to construct the User object, which stores
// skills as an ArrayList<String>.
// ---------------------------------------------------------------
import java.util.ArrayList;

// ---------------------------------------------------------------
// Calendar — used to get the current year as a default value for
// the education table's start_year column, since the UI collects
// education as a single text field without a separate year input.
// ---------------------------------------------------------------
import java.util.Calendar;

// ---------------------------------------------------------------
// Our own classes
// ---------------------------------------------------------------
import com.resumegenerator.model.User;
import com.resumegenerator.db.DatabaseManager;

/**
 * UserDAO — Data Access Object for the 'users' table.
 *
 * WHY THIS CLASS EXISTS:
 * It separates database logic from business logic.
 * Without it, SQL strings would be scattered across your
 * UI code (ResumeBuilder), model code (User), and everywhere
 * else. The DAO pattern puts ALL database operations for one
 * table in one class.
 *
 * Currently implements:
 *   - save(User user)     → INSERT
 *   - findById(int id)    → SELECT by primary key
 *   - findAll()           → SELECT all users
 */
public class UserDAO {

    // ===============================================================
    // SQL TEMPLATE — INSERT into users
    // ===============================================================
    // This is the INSERT statement with three ? placeholders.
    //
    // Column mapping:
    //   ?1 → full_name   (from user.getName())
    //   ?2 → email       (from user.getEmail())
    //   ?3 → phone       (from user.getPhone())
    //
    // Columns NOT listed here:
    //   user_id    → AUTO_INCREMENT, MySQL generates it
    //   password_hash → NULL for now (auth not implemented)
    //   created_at → DEFAULT CURRENT_TIMESTAMP, MySQL fills it
    //   updated_at → DEFAULT CURRENT_TIMESTAMP, MySQL fills it
    //
    // WHY DEFINE IT AS A CONSTANT?
    // If the SQL is written inline inside the method, you might
    // accidentally have typos in different methods. A constant
    // ensures the SQL is written once and reused.
    // ===============================================================
    private static final String INSERT_USER_SQL =
        "INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)";

    // ===============================================================
    // SQL TEMPLATE — INSERT into resumes
    // ===============================================================
    //
    // Column mapping:
    //   ?1 → user_id      (auto-generated from the users INSERT)
    //   ?2 → title         (derived from user's name, e.g.
    //                       "John Doe's Resume")
    //   ?3 → resume_type   (ENUM: 'FRESHER' or 'EXPERIENCED',
    //                       determined by user.getExperienceYears())
    //   ?4 → objective     (career objective from the form)
    //
    // Columns NOT listed here:
    //   resume_id  → AUTO_INCREMENT, MySQL generates it
    //   created_at → DEFAULT CURRENT_TIMESTAMP
    //   updated_at → DEFAULT CURRENT_TIMESTAMP ON UPDATE
    //
    // WHY THIS TABLE?
    //   The users table stores WHO the person is (identity/contact).
    //   The resumes table stores WHAT resume they are building
    //   (title, type, objective). One user can have many resumes.
    //
    // ===============================================================
    // HOW THE GENERATED resume_id WILL BE USED LATER
    // ===============================================================
    //
    // The resume_id is the CENTRAL FOREIGN KEY for all resume
    // content tables. When we implement saving to the child tables,
    // every INSERT will reference this resume_id:
    //
    //   education table:
    //     INSERT INTO education (resume_id, institution, degree, ...)
    //     → links each degree to THIS specific resume
    //
    //   skills table + resume_skills junction table:
    //     INSERT INTO skills (skill_name) → get skill_id
    //     INSERT INTO resume_skills (resume_id, skill_id, ...)
    //     → many-to-many: one resume can have many skills,
    //       one skill can appear on many resumes
    //
    //   experience table:
    //     INSERT INTO experience (resume_id, company_name, ...)
    //     → links each job position to THIS resume
    //
    //   projects table:
    //     INSERT INTO projects (resume_id, project_name, ...)
    //     → links each project to THIS resume
    //
    //   certifications table:
    //     INSERT INTO certifications (resume_id, certification_name, ...)
    //     → links each certification to THIS resume
    //
    //   generated_resumes table:
    //     INSERT INTO generated_resumes (resume_id, file_path, ...)
    //     → logs each PDF generation event for THIS resume
    //
    // Without resume_id, none of these child tables can be populated.
    // That is why we generate and capture it NOW, even though we
    // are not yet saving to those tables.
    //
    // FLOW (current):
    //   users INSERT → user_id → resumes INSERT → resume_id (printed)
    //
    // FLOW (future, all in one transaction):
    //   users INSERT     → user_id
    //   resumes INSERT   → resume_id
    //   education INSERT(s)     ← resume_id
    //   skills / resume_skills  ← resume_id
    //   experience INSERT(s)    ← resume_id
    //   projects INSERT(s)      ← resume_id
    //   certifications INSERT(s)← resume_id
    //   COMMIT
    // ===============================================================
    private static final String INSERT_RESUME_SQL =
        "INSERT INTO resumes (user_id, title, resume_type, objective, template_type) VALUES (?, ?, ?, ?, ?)";

    private static final String INSERT_EDUCATION_FULL_SQL =
        "INSERT INTO education (resume_id, institution, degree, field_of_study, start_year, end_year, grade, display_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_RESUME_SKILL_FULL_SQL =
        "INSERT INTO resume_skills (resume_id, skill_id, proficiency_level, display_order) VALUES (?, ?, ?, ?)";

    private static final String INSERT_EXPERIENCE_FULL_SQL =
        "INSERT INTO experience (resume_id, company_name, job_title, location, start_date, end_date, description, display_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_PROJECT_FULL_SQL =
        "INSERT INTO projects (resume_id, project_name, description, tech_stack, project_url, display_order) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_CERTIFICATION_FULL_SQL =
        "INSERT INTO certifications (resume_id, certification_name, issuing_org, issue_date, credential_url, display_order) VALUES (?, ?, ?, ?, ?, ?)";

    // ===============================================================
    // SQL TEMPLATE — INSERT into education
    // ===============================================================
    //
    // Column mapping:
    //   ?1 → resume_id    (from the resumes INSERT above)
    //   ?2 → institution  (default: "Not specified" — UI has no
    //                      separate field for this)
    //   ?3 → degree       (the flat education string from the UI)
    //   ?4 → start_year   (default: current year — UI has no
    //                      separate year field)
    //
    // WHY THESE DEFAULTS?
    //   The education table has NOT NULL constraints on institution,
    //   degree, and start_year. The UI currently collects education
    //   as a single text field. Rather than rejecting the data or
    //   changing the UI, we store the text in the most meaningful
    //   column (degree) and use safe defaults for the rest.
    //   When the UI is redesigned with structured fields, these
    //   defaults will be replaced with real user input.
    // ===============================================================
    private static final String INSERT_EDUCATION_SQL =
        "INSERT INTO education (resume_id, institution, degree, start_year) VALUES (?, ?, ?, ?)";

    // ===============================================================
    // SQL TEMPLATES — INSERT into skills + resume_skills
    // ===============================================================
    //
    // Skills use a MANY-TO-MANY relationship:
    //   skills table       → master list of unique skill names
    //   resume_skills table → junction table linking resumes to skills
    //
    // STEP 1: INSERT IGNORE INTO skills
    //   INSERT IGNORE means: if "Java" already exists (UNIQUE
    //   constraint on skill_name), silently skip the INSERT instead
    //   of throwing a duplicate-key error. This is safe because we
    //   only need the skill to EXIST — we don't care if we or
    //   another user created it.
    //
    // STEP 2: SELECT skill_id
    //   After the INSERT (or skip), we SELECT the skill_id by name.
    //   We need this ID for the junction table INSERT.
    //
    // STEP 3: INSERT INTO resume_skills
    //   Links THIS resume to THIS skill via their IDs.
    //   display_order preserves the order the user typed them in.
    // ===============================================================
    private static final String INSERT_SKILL_SQL =
        "INSERT IGNORE INTO skills (skill_name) VALUES (?)";

    private static final String SELECT_SKILL_ID_SQL =
        "SELECT skill_id FROM skills WHERE skill_name = ?";

    private static final String INSERT_RESUME_SKILL_SQL =
        "INSERT INTO resume_skills (resume_id, skill_id, display_order) VALUES (?, ?, ?)";

    // ===============================================================
    // SQL TEMPLATE — INSERT into experience
    // ===============================================================
    //
    // Column mapping:
    //   ?1 → resume_id     (from the resumes INSERT)
    //   ?2 → company_name  (default: "Not specified")
    //   ?3 → job_title     (default: "Not specified")
    //   ?4 → start_date    (default: today's date)
    //   ?5 → description   (the flat experience string from the UI)
    //
    // Same rationale as education — the UI collects a single text
    // field, but the schema requires structured NOT NULL columns.
    // ===============================================================
    private static final String INSERT_EXPERIENCE_SQL =
        "INSERT INTO experience (resume_id, company_name, job_title, start_date, description) "
        + "VALUES (?, ?, ?, ?, ?)";

    // ===============================================================
    // SQL TEMPLATE — INSERT into projects
    // ===============================================================
    //
    // Column mapping:
    //   ?1 → resume_id     (from the resumes INSERT)
    //   ?2 → project_name  (the flat projects string from the UI)
    //
    // The projects table only requires resume_id and project_name
    // as NOT NULL — all other columns (description, tech_stack,
    // project_url) are nullable. So we only need the user's text.
    // ===============================================================
    private static final String INSERT_PROJECT_SQL =
        "INSERT INTO projects (resume_id, project_name) VALUES (?, ?)";

    // ===============================================================
    // SQL TEMPLATE — INSERT into certifications
    // ===============================================================
    //
    // Column mapping:
    //   ?1 → resume_id          (from the resumes INSERT)
    //   ?2 → certification_name (the flat certifications string
    //                            from the UI)
    //
    // Same as projects — only resume_id and certification_name are
    // NOT NULL. The other columns (issuing_org, issue_date,
    // credential_url) are nullable.
    // ===============================================================
    private static final String INSERT_CERTIFICATION_SQL =
        "INSERT INTO certifications (resume_id, certification_name) VALUES (?, ?)";


    // ===============================================================
    // SQL TEMPLATE — SELECT by primary key
    // ===============================================================
    // SELECT * returns all columns for the row whose user_id
    // matches the ? placeholder.
    //
    // WHY "SELECT *" HERE?
    //   For a findById that returns the full User object, we need
    //   every column. In performance-critical code you'd list
    //   specific columns, but for a single-row PK lookup the
    //   difference is negligible.
    //
    // WHY "WHERE user_id = ?"?
    //   user_id is the PRIMARY KEY, so MySQL uses the clustered
    //   index — this is an O(log n) lookup, effectively instant
    //   even with millions of rows.
    // ===============================================================
    private static final String SELECT_BY_ID_SQL =
        "SELECT * FROM users WHERE user_id = ?";

    // ===============================================================
    // SQL TEMPLATE — SELECT all users
    // ===============================================================
    // No WHERE clause → returns every row in the users table.
    //
    // ORDER BY created_at DESC → newest users appear first.
    // Without ORDER BY, MySQL returns rows in an undefined order
    // (usually insertion order for InnoDB, but NOT guaranteed).
    // Always specify ORDER BY when order matters to the caller.
    //
    // No ? placeholders → no parameters to bind. We still use
    // PreparedStatement (not Statement) for consistency and to
    // benefit from the server-side execution plan cache.
    // ===============================================================
    private static final String SELECT_ALL_SQL =
        "SELECT * FROM users ORDER BY created_at DESC";

    // ===============================================================
    //  save(User user) — inserts a user AND their resume in one
    //                     atomic transaction
    // ===============================================================
    /**
     * Saves a User to the 'users' table AND creates a corresponding
     * row in the 'resumes' table, all within a single database
     * transaction.
     *
     * <p><b>Transaction guarantee:</b> Either BOTH rows are inserted
     * (users + resumes), or NEITHER is. If the resumes INSERT fails
     * after the users INSERT succeeded, the transaction is rolled
     * back and the users row is undone.</p>
     *
     * <p>The generated resume_id is printed to the console. It will
     * be used as the foreign key for education, skills, experience,
     * projects, and certifications when those DAOs are implemented.</p>
     *
    /**
     * Saves a new resume for an EXISTING user account (userId).
     * Creates a new row in 'resumes' and populates child tables
     * without inserting a new user account row.
     *
     * @param userId      the primary key of the logged-in user account
     * @param resumeTitle the title for this resume version (e.g. "Java Developer Resume")
     * @param user        the User object containing resume form details
     * @return the generated resume_id
     * @throws SQLException on database error
     */
    public int saveResumeForUser(int userId, String resumeTitle, User user) throws SQLException {
        // Legacy bridge: construct a Resume aggregate from the flat User and delegate
        com.resumegenerator.model.Resume resume = new com.resumegenerator.model.Resume();
        resume.setUserId(userId);
        resume.setTitle(resumeTitle != null && !resumeTitle.trim().isEmpty()
            ? resumeTitle.trim()
            : (user.getName() != null ? user.getName() + "'s Resume" : "Untitled Resume"));
        resume.setResumeType(com.resumegenerator.model.ResumeType.FRESHER);
        resume.setTemplateType(com.resumegenerator.resume.TemplateType.CLASSIC);
        resume.setUser(user);
        return saveResume(resume);
    }

    public java.util.List<String> getResumesForUser(int userId) throws SQLException {
        java.util.List<String> resumeTitles = new java.util.ArrayList<>();
        String sql = "SELECT resume_id, title FROM resumes WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resumeTitles.add(rs.getInt("resume_id") + ": " + rs.getString("title"));
                }
            }
        }
        return resumeTitles;
    }

    public int saveResume(com.resumegenerator.model.Resume resume) throws SQLException {
        Connection conn = null;
        PreparedStatement resumeStmt = null;
        PreparedStatement eduStmt = null;
        PreparedStatement skillStmt = null;
        PreparedStatement selectSkillStmt = null;
        PreparedStatement resumeSkillStmt = null;
        PreparedStatement expStmt = null;
        PreparedStatement projStmt = null;
        PreparedStatement certStmt = null;
        ResultSet resumeKeys = null;
        ResultSet skillIdRs = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            resumeStmt = conn.prepareStatement(INSERT_RESUME_SQL, Statement.RETURN_GENERATED_KEYS);
            resumeStmt.setInt(1, resume.getUserId());
            resumeStmt.setString(2, resume.getTitle() != null ? resume.getTitle() : "Untitled Resume");
            resumeStmt.setString(3, resume.getResumeType() != null ? resume.getResumeType().name() : "FRESHER");
            if (resume.getObjective() != null && !resume.getObjective().isEmpty()) {
                resumeStmt.setString(4, resume.getObjective());
            } else {
                resumeStmt.setNull(4, java.sql.Types.VARCHAR);
            }
            resumeStmt.setString(5, resume.getTemplateType() != null ? resume.getTemplateType().name() : "CLASSIC");

            resumeStmt.executeUpdate();
            resumeKeys = resumeStmt.getGeneratedKeys();
            int generatedResumeId = -1;
            if (resumeKeys.next()) {
                generatedResumeId = resumeKeys.getInt(1);
            }
            if (generatedResumeId == -1) {
                conn.rollback();
                return -1;
            }
            resume.setResumeId(generatedResumeId);

            if (resume.getEducationList() != null && !resume.getEducationList().isEmpty()) {
                eduStmt = conn.prepareStatement(INSERT_EDUCATION_FULL_SQL);
                for (com.resumegenerator.model.Education edu : resume.getEducationList()) {
                    eduStmt.setInt(1, generatedResumeId);
                    eduStmt.setString(2, edu.getInstitution() != null ? edu.getInstitution() : "Not specified");
                    eduStmt.setString(3, edu.getDegree() != null ? edu.getDegree() : "Not specified");
                    eduStmt.setString(4, edu.getFieldOfStudy());
                    eduStmt.setInt(5, edu.getStartYear() > 0 ? edu.getStartYear() : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
                    if (edu.getEndYear() != null) eduStmt.setInt(6, edu.getEndYear()); else eduStmt.setNull(6, java.sql.Types.INTEGER);
                    eduStmt.setString(7, edu.getGrade());
                    eduStmt.setInt(8, edu.getDisplayOrder());
                    eduStmt.executeUpdate();
                }
            }

            if (resume.getSkillList() != null && !resume.getSkillList().isEmpty()) {
                skillStmt = conn.prepareStatement(INSERT_SKILL_SQL);
                selectSkillStmt = conn.prepareStatement(SELECT_SKILL_ID_SQL);
                resumeSkillStmt = conn.prepareStatement(INSERT_RESUME_SKILL_FULL_SQL);

                for (com.resumegenerator.model.Skill skill : resume.getSkillList()) {
                    if (skill.getSkillName() == null || skill.getSkillName().trim().isEmpty()) continue;
                    String sName = skill.getSkillName().trim();
                    skillStmt.setString(1, sName);
                    skillStmt.executeUpdate();

                    selectSkillStmt.setString(1, sName);
                    skillIdRs = selectSkillStmt.executeQuery();
                    if (skillIdRs.next()) {
                        int skillId = skillIdRs.getInt(1);
                        skill.setSkillId(skillId);
                        resumeSkillStmt.setInt(1, generatedResumeId);
                        resumeSkillStmt.setInt(2, skillId);
                        if (skill.getProficiencyLevel() != null) resumeSkillStmt.setString(3, skill.getProficiencyLevel().name());
                        else resumeSkillStmt.setNull(3, java.sql.Types.VARCHAR);
                        resumeSkillStmt.setInt(4, skill.getDisplayOrder());
                        resumeSkillStmt.executeUpdate();
                    }
                    if (skillIdRs != null) { skillIdRs.close(); skillIdRs = null; }
                }
            }

            if (resume.getExperienceList() != null && !resume.getExperienceList().isEmpty()) {
                expStmt = conn.prepareStatement(INSERT_EXPERIENCE_FULL_SQL);
                for (com.resumegenerator.model.Experience exp : resume.getExperienceList()) {
                    expStmt.setInt(1, generatedResumeId);
                    expStmt.setString(2, exp.getCompanyName() != null ? exp.getCompanyName() : "Not specified");
                    expStmt.setString(3, exp.getJobTitle() != null ? exp.getJobTitle() : "Not specified");
                    expStmt.setString(4, exp.getLocation());
                    expStmt.setDate(5, exp.getStartDate() != null ? java.sql.Date.valueOf(exp.getStartDate()) : new java.sql.Date(System.currentTimeMillis()));
                    if (exp.getEndDate() != null) expStmt.setDate(6, java.sql.Date.valueOf(exp.getEndDate())); else expStmt.setNull(6, java.sql.Types.DATE);
                    expStmt.setString(7, exp.getDescription());
                    expStmt.setInt(8, exp.getDisplayOrder());
                    expStmt.executeUpdate();
                }
            }

            if (resume.getProjectList() != null && !resume.getProjectList().isEmpty()) {
                projStmt = conn.prepareStatement(INSERT_PROJECT_FULL_SQL);
                for (com.resumegenerator.model.Project proj : resume.getProjectList()) {
                    projStmt.setInt(1, generatedResumeId);
                    projStmt.setString(2, proj.getProjectName() != null ? proj.getProjectName() : "Untitled Project");
                    projStmt.setString(3, proj.getDescription());
                    projStmt.setString(4, proj.getTechStack());
                    projStmt.setString(5, proj.getProjectUrl());
                    projStmt.setInt(6, proj.getDisplayOrder());
                    projStmt.executeUpdate();
                }
            }

            if (resume.getCertificationList() != null && !resume.getCertificationList().isEmpty()) {
                certStmt = conn.prepareStatement(INSERT_CERTIFICATION_FULL_SQL);
                for (com.resumegenerator.model.Certification cert : resume.getCertificationList()) {
                    certStmt.setInt(1, generatedResumeId);
                    certStmt.setString(2, cert.getCertificationName() != null ? cert.getCertificationName() : "Untitled Certification");
                    certStmt.setString(3, cert.getIssuingOrg());
                    if (cert.getIssueDate() != null) certStmt.setDate(4, java.sql.Date.valueOf(cert.getIssueDate())); else certStmt.setNull(4, java.sql.Types.DATE);
                    certStmt.setString(5, cert.getCredentialUrl());
                    certStmt.setInt(6, cert.getDisplayOrder());
                    certStmt.executeUpdate();
                }
            }

            conn.commit();
            System.out.println("[UserDAO] Structured Resume aggregate saved for user_id: " + resume.getUserId() + " with resume_id: " + generatedResumeId);
            return generatedResumeId;

        } catch (SQLException ex) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw ex;
        } finally {
            if (skillIdRs != null) { try { skillIdRs.close(); } catch (SQLException ignored) {} }
            if (resumeKeys != null) { try { resumeKeys.close(); } catch (SQLException ignored) {} }
            if (certStmt != null) { try { certStmt.close(); } catch (SQLException ignored) {} }
            if (projStmt != null) { try { projStmt.close(); } catch (SQLException ignored) {} }
            if (expStmt != null) { try { expStmt.close(); } catch (SQLException ignored) {} }
            if (resumeSkillStmt != null) { try { resumeSkillStmt.close(); } catch (SQLException ignored) {} }
            if (selectSkillStmt != null) { try { selectSkillStmt.close(); } catch (SQLException ignored) {} }
            if (skillStmt != null) { try { skillStmt.close(); } catch (SQLException ignored) {} }
            if (eduStmt != null) { try { eduStmt.close(); } catch (SQLException ignored) {} }
            if (resumeStmt != null) { try { resumeStmt.close(); } catch (SQLException ignored) {} }
        }
    }

    public int saveResumeForUser(int userId, String title, com.resumegenerator.model.Resume resume) throws SQLException {
        resume.setUserId(userId);
        if (title != null && !title.isEmpty()) {
            resume.setTitle(title);
        }
        return saveResume(resume);
    }

    public int save(User user) throws SQLException {
        if (user.getUserId() > 0) {
            return user.getUserId();
        }

        Connection conn = null;
        PreparedStatement userStmt = null;
        ResultSet userKeys = null;

        try {
            conn = DatabaseManager.getConnection();
            userStmt = conn.prepareStatement(
                INSERT_USER_SQL,
                Statement.RETURN_GENERATED_KEYS
            );

            userStmt.setString(1, user.getName());
            userStmt.setString(2, user.getEmail());
            userStmt.setString(3, user.getPhone());

            int userRowsAffected = userStmt.executeUpdate();

            int generatedUserId = -1;
            if (userRowsAffected > 0) {
                userKeys = userStmt.getGeneratedKeys();
                if (userKeys.next()) {
                    generatedUserId = userKeys.getInt(1);
                }
            }

            return generatedUserId;

        } finally {
            if (userKeys != null) { try { userKeys.close(); } catch (SQLException ignored) {} }
            if (userStmt != null) { try { userStmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    // ===============================================================
    //  findById(int id) — retrieves a single user by primary key
    // ===============================================================
    /**
     * Looks up a user by their user_id.
     *
     * @param id the primary key (user_id) to search for
     * @return the matching User object, or null if no user exists
     *         with that id
     * @throws SQLException if a database error occurs
     */
    public User findById(int id) throws SQLException {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // Prepare the SELECT statement.
            //
            // No RETURN_GENERATED_KEYS flag this time — we are
            // reading data, not inserting. The single-argument
            // version of prepareStatement() is sufficient.
            // -------------------------------------------------------
            pstmt = conn.prepareStatement(SELECT_BY_ID_SQL);

            // -------------------------------------------------------
            // Bind the user_id to ?1.
            //
            // pstmt.setInt(1, id)
            //   parameterIndex = 1 → the first (and only) ? in
            //   "SELECT * FROM users WHERE user_id = ?"
            //   setInt() binds a Java int, matching the INT column.
            // -------------------------------------------------------
            pstmt.setInt(1, id);

            // -------------------------------------------------------
            // Execute the SELECT.
            //
            // pstmt.executeQuery()
            //   → sends the SQL to MySQL and returns a ResultSet.
            //
            // WHY executeQuery() AND NOT executeUpdate()?
            //   • executeQuery()  → for SELECT (returns rows)
            //   • executeUpdate() → for INSERT/UPDATE/DELETE
            //     (returns an affected-row count)
            //
            // =====================================================
            //  WHAT IS A ResultSet?
            // =====================================================
            //
            // Think of ResultSet as a TABLE returned by MySQL,
            // loaded into Java's memory. It has:
            //   • ROWS   — one for each matching record
            //   • COLUMNS — one for each column in your SELECT
            //   • A CURSOR — an invisible pointer that starts
            //     BEFORE the first row
            //
            // Visual model of the cursor:
            //
            //   Cursor →  (BEFORE FIRST ROW)     ← rs starts here
            //             ┌────────┬──────────┬───────┐
            //   Row 1     │ user_id│ full_name│ email │
            //             ├────────┼──────────┼───────┤
            //   Row 2     │  ...   │   ...    │  ...  │
            //             └────────┴──────────┴───────┘
            //             (AFTER LAST ROW)
            //
            // The cursor starts BEFORE row 1. You must call
            // rs.next() to move it onto a row before you can
            // read any data.
            // =====================================================
            // -------------------------------------------------------
            rs = pstmt.executeQuery();

            // -------------------------------------------------------
            // READING THE ResultSet
            // -------------------------------------------------------
            //
            // =====================================================
            //  WHY while(rs.next()) ?
            // =====================================================
            //
            // rs.next() does TWO things:
            //   1. MOVES the cursor forward by one row
            //   2. RETURNS true if the cursor is now on a valid row,
            //      or false if it has moved past the last row
            //
            // So the while loop means:
            //   "Move to the next row. If a row exists, enter the
            //    loop body. If no more rows, stop."
            //
            // Iteration example (2 rows returned):
            //
            //   Call 1: rs.next() → cursor moves to Row 1 → true
            //           loop body reads Row 1 columns
            //   Call 2: rs.next() → cursor moves to Row 2 → true
            //           loop body reads Row 2 columns
            //   Call 3: rs.next() → cursor moves past last → false
            //           loop exits
            //
            // WHY NOT if(rs.next()) ?
            //   For findById we expect 0 or 1 rows (because user_id
            //   is a PRIMARY KEY). So if(rs.next()) would work here.
            //   BUT we use while(rs.next()) because:
            //     a) It's the standard JDBC idiom — every Java
            //        developer recognizes it instantly.
            //     b) It's forward-compatible — if the query ever
            //        returns multiple rows (e.g., you change the
            //        WHERE clause), the code still works.
            //     c) It handles the "0 rows" case automatically —
            //        the loop body simply never executes, and we
            //        fall through to return null.
            //
            // IMPORTANT: You CANNOT skip rs.next(). If you try to
            // call rs.getString() without calling rs.next() first,
            // JDBC throws: "Before start of result set" because the
            // cursor is still BEFORE row 1.
            // =====================================================
            while (rs.next()) {

                // ---------------------------------------------------
                // rs.getString("full_name")
                //
                //   Reads the value of the "full_name" column from
                //   the CURRENT row (the row the cursor is on).
                //
                //   You can also use rs.getString(2) to access by
                //   column index (1-based), but column NAMES are
                //   preferred because:
                //     • They're self-documenting ("full_name" vs 2)
                //     • They don't break if you add a column to the
                //       table or reorder columns in the SELECT
                //
                // rs.getInt("user_id")
                //   Same idea, but returns an int instead of String.
                //   JDBC automatically converts the MySQL INT to
                //   a Java int.
                // ---------------------------------------------------
                String name  = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");

                // ---------------------------------------------------
                // Construct and return the User object.
                //
                // The User constructor requires all fields, but the
                // users table only stores name, email, phone. The
                // remaining fields (education, skills, experience,
                // etc.) live in separate normalized tables and will
                // be loaded by their own DAOs in the future.
                //
                // For now we pass empty/default values for those
                // fields since this DAO only handles the users table.
                // ---------------------------------------------------
                return new User(
                    name,                    // full_name
                    email,                   // email
                    phone                    // phone
                );
            }

            // -------------------------------------------------------
            // If rs.next() returned false on the very first call,
            // the while loop body never executed. This means no
            // user with that id exists in the database.
            // Returning null signals "not found" to the caller.
            // -------------------------------------------------------
            return null;

        } finally {
            // Close in reverse order: ResultSet → PreparedStatement → Connection
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            DatabaseManager.closeConnection(conn);
        }
    }

    // ===============================================================
    //  findAll() — retrieves every user from the database
    // ===============================================================
    //
    // ===============================================================
    //  WHY DOES THIS METHOD RETURN ArrayList<User>
    //  INSTEAD OF ResultSet?
    // ===============================================================
    //
    //  You might wonder: "The ResultSet already contains all the
    //  data. Why not just return it and let the caller read it?"
    //
    //  Here are 4 reasons why that's a bad idea:
    //
    //  1. RESOURCE SAFETY
    //     A ResultSet is tied to its PreparedStatement, which is
    //     tied to its Connection. If we return the ResultSet, we
    //     CANNOT close the Connection in our finally block —
    //     because closing the Connection automatically closes the
    //     ResultSet too. The caller would receive a dead ResultSet.
    //
    //     To keep it alive, we'd have to leave the Connection open
    //     and trust the caller to close it. If they forget, we
    //     leak connections → MySQL hits "Too many connections" →
    //     the entire application dies.
    //
    //  2. ENCAPSULATION (Hiding database details)
    //     ResultSet is a JDBC class — it belongs to the database
    //     layer. If your UI code (ResumeBuilder) receives a
    //     ResultSet, it now depends on java.sql.* imports, column
    //     names, and SQL types. Change a column name in MySQL and
    //     you break the UI. By returning User objects, only the
    //     DAO needs to know about column names.
    //
    //  3. TESTABILITY
    //     You can easily create an ArrayList<User> in a unit test:
    //        List<User> fakeUsers = new ArrayList<>();
    //        fakeUsers.add(new User("Alice", ...));
    //     You CANNOT easily create a fake ResultSet — it requires
    //     a live database connection or a complex mock.
    //
    //  4. SEPARATION OF CONCERNS
    //     The DAO's job is: "Talk to the database and give me
    //     Java objects." The UI's job is: "Display Java objects."
    //     Neither should know about the other's internals.
    //
    //     Database → DAO → User objects → UI
    //     (SQL)      (converts)           (displays)
    //
    //  RULE OF THUMB:
    //    ResultSet should NEVER leave the DAO class. Convert it
    //    to model objects (User, Resume, etc.) inside the DAO,
    //    close all resources, and return clean Java objects.
    // ===============================================================
    /**
     * Retrieves all users from the database.
     *
     * @return an ArrayList of User objects (empty list if no users exist)
     * @throws SQLException if a database error occurs
     */
    public ArrayList<User> findAll() throws SQLException {

        // -----------------------------------------------------------
        // We create the list OUTSIDE the try block so it's accessible
        // in the return statement. Even if the ResultSet is empty,
        // we return an empty list — never null.
        //
        // WHY NEVER RETURN NULL?
        //   If findAll() returns null, every caller must write:
        //     if (users != null) { for (User u : users) { ... } }
        //   If it returns an empty list, callers just write:
        //     for (User u : users) { ... }
        //   The loop body simply never executes. Cleaner, safer.
        // -----------------------------------------------------------
        ArrayList<User> users = new ArrayList<>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // Prepare and execute — no parameters to bind this time,
            // so we go straight from prepareStatement to executeQuery.
            // -------------------------------------------------------
            pstmt = conn.prepareStatement(SELECT_ALL_SQL);
            rs = pstmt.executeQuery();

            // -------------------------------------------------------
            // ITERATE through every row in the ResultSet.
            //
            // This is where while(rs.next()) truly shines — unlike
            // findById (which returns at most 1 row), findAll can
            // return thousands of rows. The while loop processes
            // each one:
            //
            //   Iteration 1: rs.next() → cursor on Row 1 → true
            //     read columns → build User → add to list
            //   Iteration 2: rs.next() → cursor on Row 2 → true
            //     read columns → build User → add to list
            //   ...repeat for every row...
            //   Last call:   rs.next() → past end → false → exit
            //
            // If the table is empty, rs.next() returns false on the
            // very first call and the loop body never executes.
            // We return the empty ArrayList — no special handling.
            // -------------------------------------------------------
            while (rs.next()) {

                // ---------------------------------------------------
                // Extract columns from the CURRENT row.
                //
                // We use column NAMES (not indexes) for readability
                // and resilience to schema changes.
                // ---------------------------------------------------
                String name  = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");

                // ---------------------------------------------------
                // Build a User object from this row and add it
                // to the list.
                //
                // Same as findById — we pass defaults for fields
                // that live in other normalized tables.
                // ---------------------------------------------------
                User user = new User(
                    name,                    // full_name
                    email,                   // email
                    phone                    // phone
                );

                users.add(user);
            }

        } finally {
            // Close in reverse order: ResultSet → PreparedStatement → Connection
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            DatabaseManager.closeConnection(conn);
        }

        // -----------------------------------------------------------
        // Return the fully-built list AFTER all resources are closed.
        //
        // This is the whole point: the Connection, PreparedStatement,
        // and ResultSet are all closed. The data now lives safely in
        // plain Java objects (ArrayList<User>) that the caller can
        // use forever — no database dependency.
        // -----------------------------------------------------------
        return users;
    }
}
