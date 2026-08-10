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
        "INSERT INTO resumes (user_id, title, resume_type, objective) VALUES (?, ?, ?, ?)";

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
     * @param user the User object to persist
     * @return the generated user_id (primary key), or -1 if the insert failed
     * @throws SQLException if a database error occurs (triggers rollback)
     */
    public int save(User user) throws SQLException {

        // -----------------------------------------------------------
        // Declare all JDBC resources outside the try block so we
        // can close them in the finally block regardless of outcome.
        //
        // We need PreparedStatements for each INSERT (users, resumes,
        // education, skills, resume_skills, experience, projects,
        // certifications) and ResultSets for generated keys.
        // -----------------------------------------------------------
        Connection conn = null;
        PreparedStatement userStmt = null;
        PreparedStatement resumeStmt = null;
        PreparedStatement educationStmt = null;
        PreparedStatement skillStmt = null;
        PreparedStatement selectSkillStmt = null;
        PreparedStatement resumeSkillStmt = null;
        PreparedStatement experienceStmt = null;
        PreparedStatement projectStmt = null;
        PreparedStatement certificationStmt = null;
        ResultSet userKeys = null;
        ResultSet resumeKeys = null;
        ResultSet skillIdRs = null;

        try {
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // TRANSACTION STEP 1: Disable auto-commit
            // -------------------------------------------------------
            //
            // By default, JDBC runs in AUTO-COMMIT mode: every
            // single SQL statement is immediately committed to the
            // database. This means if we INSERT into users and then
            // the INSERT into resumes fails, the users row is already
            // permanent — we can't undo it.
            //
            // conn.setAutoCommit(false) switches to MANUAL COMMIT:
            //   • No statement is committed until we explicitly call
            //     conn.commit()
            //   • If anything fails, we call conn.rollback() to undo
            //     ALL statements since setAutoCommit(false)
            //
            // This gives us ATOMICITY: both INSERTs succeed together
            // or fail together. The database is never left in a
            // half-saved state.
            //
            // WHAT WOULD GO WRONG WITHOUT A TRANSACTION?
            //   1. INSERT INTO users → succeeds → user_id = 42
            //   2. INSERT INTO resumes → fails (e.g., bad ENUM value)
            //   Result: user_id 42 exists but has no resume.
            //   The foreign key relationship is broken — orphan row.
            //   With a transaction, step 1 is rolled back too.
            // -------------------------------------------------------
            conn.setAutoCommit(false);

            // -------------------------------------------------------
            // TRANSACTION STEP 2: INSERT into users
            // -------------------------------------------------------
            userStmt = conn.prepareStatement(
                INSERT_USER_SQL,
                Statement.RETURN_GENERATED_KEYS
            );

            userStmt.setString(1, user.getName());   // ?1 → full_name
            userStmt.setString(2, user.getEmail());   // ?2 → email
            userStmt.setString(3, user.getPhone());   // ?3 → phone

            int userRowsAffected = userStmt.executeUpdate();

            // -------------------------------------------------------
            // Retrieve the auto-generated user_id.
            // We need it immediately for the resumes INSERT (as the
            // foreign key) and to return to the caller.
            // -------------------------------------------------------
            int generatedUserId = -1;
            if (userRowsAffected > 0) {
                userKeys = userStmt.getGeneratedKeys();
                if (userKeys.next()) {
                    generatedUserId = userKeys.getInt(1);
                }
            }

            if (generatedUserId == -1) {
                // User INSERT didn't produce a key — something is
                // very wrong. Roll back and signal failure.
                conn.rollback();
                return -1;
            }

            // -------------------------------------------------------
            // TRANSACTION STEP 3: INSERT into resumes
            // -------------------------------------------------------
            //
            // Now we use the user_id we just obtained as the foreign
            // key in the resumes table. This is exactly WHY we
            // retrieved the generated key in Step 2.
            //
            // Column mapping:
            //   ?1 → user_id       ← generatedUserId
            //   ?2 → title         ← derived from user's name
            //   ?3 → resume_type   ← 'EXPERIENCED' or 'FRESHER'
            //   ?4 → objective     ← user.getObjective()
            // -------------------------------------------------------
            resumeStmt = conn.prepareStatement(
                INSERT_RESUME_SQL,
                Statement.RETURN_GENERATED_KEYS
            );

            resumeStmt.setInt(1, generatedUserId);

            // -------------------------------------------------------
            // Title: We derive a default title from the user's name.
            // Example: "John Doe's Resume"
            //
            // In the future, the UI could have a dedicated title
            // field, but for now we auto-generate it since the
            // schema requires a non-null title.
            // -------------------------------------------------------
            resumeStmt.setString(2, user.getName() + "'s Resume");

            // -------------------------------------------------------
            // Resume type: The User model stores experienceYears.
            //   • experienceYears > 0  → 'EXPERIENCED'
            //   • experienceYears == 0 → 'FRESHER'
            //
            // This maps directly to the ENUM('FRESHER','EXPERIENCED')
            // column in the resumes table.
            // -------------------------------------------------------
            String resumeType = user.getExperienceYears() > 0
                ? "EXPERIENCED" : "FRESHER";
            resumeStmt.setString(3, resumeType);

            // -------------------------------------------------------
            // Objective: The career objective / summary statement.
            // This can be NULL in the schema, so an empty string
            // from the form is acceptable.
            // -------------------------------------------------------
            String objective = user.getObjective();
            if (objective != null && !objective.trim().isEmpty()) {
                resumeStmt.setString(4, objective);
            } else {
                resumeStmt.setNull(4, java.sql.Types.VARCHAR);
            }

            resumeStmt.executeUpdate();

            // -------------------------------------------------------
            // Retrieve the auto-generated resume_id.
            //
            // We don't return this to the UI (to avoid UI changes),
            // but we print it so you can verify the INSERT worked
            // and see the ID that future child-table DAOs will use.
            // -------------------------------------------------------
            resumeKeys = resumeStmt.getGeneratedKeys();
            int generatedResumeId = -1;
            if (resumeKeys.next()) {
                generatedResumeId = resumeKeys.getInt(1);
                System.out.println(
                    "[UserDAO] Resume saved — resume_id: " + generatedResumeId
                    + " (linked to user_id: " + generatedUserId + ")"
                );
            }

            if (generatedResumeId == -1) {
                // Resume INSERT didn't produce a key — roll back
                // the users INSERT too and signal failure.
                conn.rollback();
                return -1;
            }

            // -------------------------------------------------------
            // TRANSACTION STEP 4: INSERT into education
            // -------------------------------------------------------
            //
            // The UI stores education as a single String (e.g.,
            // "B.Tech Computer Science"). We store it in the 'degree'
            // column and use defaults for the required columns that
            // the UI doesn't collect separately.
            //
            // SKIP-IF-EMPTY: If the user left the education field
            // blank, we don't insert a placeholder row — we simply
            // skip this step. This avoids rows with only defaults
            // and no meaningful data.
            // -------------------------------------------------------
            String education = user.getEducation();
            if (education != null && !education.trim().isEmpty()) {
                educationStmt = conn.prepareStatement(INSERT_EDUCATION_SQL);
                educationStmt.setInt(1, generatedResumeId);        // ?1 → resume_id
                educationStmt.setString(2, "Not specified");        // ?2 → institution (default)
                educationStmt.setString(3, education.trim());      // ?3 → degree
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                educationStmt.setInt(4, currentYear);               // ?4 → start_year (default)
                educationStmt.executeUpdate();
                System.out.println(
                    "[UserDAO] Education saved for resume_id: " + generatedResumeId
                );
            }

            // -------------------------------------------------------
            // TRANSACTION STEP 5: INSERT into skills + resume_skills
            // -------------------------------------------------------
            //
            // The UI stores skills as an ArrayList<String>, e.g.,
            // ["Java", "MySQL", "Spring Boot"]. For each skill:
            //
            //   1. INSERT IGNORE into the skills master table
            //      → creates the skill if it doesn't exist yet
            //      → silently skips if it already exists (no error)
            //
            //   2. SELECT skill_id by name
            //      → retrieves the ID whether we just created it
            //        or it already existed
            //
            //   3. INSERT into resume_skills junction table
            //      → links this resume to this skill
            //      → display_order preserves the user's ordering
            //
            // WHY NOT LAST_INSERT_ID()?
            //   INSERT IGNORE does NOT set LAST_INSERT_ID() when the
            //   row already exists (it was a no-op). So we always
            //   SELECT by name to reliably get the skill_id.
            //
            // SKIP-IF-EMPTY: If the skills list is null or contains
            // only blank entries, we skip entirely.
            // -------------------------------------------------------
            ArrayList<String> skills = user.getSkills();
            if (skills != null && !skills.isEmpty()) {
                skillStmt = conn.prepareStatement(INSERT_SKILL_SQL);
                selectSkillStmt = conn.prepareStatement(SELECT_SKILL_ID_SQL);
                resumeSkillStmt = conn.prepareStatement(INSERT_RESUME_SKILL_SQL);

                int displayOrder = 0;
                for (String skillName : skills) {
                    // Skip blank skill entries (e.g., trailing comma:
                    // "Java, MySQL, " → ["Java", "MySQL", ""])
                    if (skillName == null || skillName.trim().isEmpty()) {
                        continue;
                    }
                    String trimmedSkill = skillName.trim();

                    // Step 5a: INSERT IGNORE — create skill if new
                    skillStmt.setString(1, trimmedSkill);
                    skillStmt.executeUpdate();

                    // Step 5b: SELECT — get the skill_id
                    selectSkillStmt.setString(1, trimmedSkill);
                    skillIdRs = selectSkillStmt.executeQuery();
                    if (skillIdRs.next()) {
                        int skillId = skillIdRs.getInt(1);

                        // Step 5c: INSERT into junction table
                        resumeSkillStmt.setInt(1, generatedResumeId);
                        resumeSkillStmt.setInt(2, skillId);
                        resumeSkillStmt.setInt(3, displayOrder);
                        resumeSkillStmt.executeUpdate();

                        displayOrder++;
                    }
                    // Close the ResultSet before the next iteration
                    // to avoid resource leaks
                    skillIdRs.close();
                    skillIdRs = null;
                }
                System.out.println(
                    "[UserDAO] " + displayOrder + " skill(s) saved for resume_id: "
                    + generatedResumeId
                );
            }

            // -------------------------------------------------------
            // TRANSACTION STEP 6: INSERT into experience
            // -------------------------------------------------------
            //
            // Same pattern as education — the UI gives us a single
            // String, but the schema expects structured columns.
            // We store the text in the 'description' column and use
            // defaults for the NOT NULL columns (company_name,
            // job_title, start_date).
            //
            // SKIP-IF-EMPTY: Only inserts if the experience field
            // is non-blank. For fresher resumes, this field is
            // typically disabled in the UI, so it will be empty.
            // -------------------------------------------------------
            String experience = user.getExperienceDetails();
            if (experience != null && !experience.trim().isEmpty()) {
                experienceStmt = conn.prepareStatement(INSERT_EXPERIENCE_SQL);
                experienceStmt.setInt(1, generatedResumeId);       // ?1 → resume_id
                experienceStmt.setString(2, "Not specified");       // ?2 → company_name (default)
                experienceStmt.setString(3, "Not specified");       // ?3 → job_title (default)
                experienceStmt.setDate(4,                           // ?4 → start_date (default: today)
                    new java.sql.Date(System.currentTimeMillis()));
                experienceStmt.setString(5, experience.trim());    // ?5 → description
                experienceStmt.executeUpdate();
                System.out.println(
                    "[UserDAO] Experience saved for resume_id: " + generatedResumeId
                );
            }

            // -------------------------------------------------------
            // TRANSACTION STEP 7: INSERT into projects
            // -------------------------------------------------------
            //
            // The projects table only requires resume_id and
            // project_name as NOT NULL. The user's text maps
            // directly to project_name — no defaults needed.
            //
            // SKIP-IF-EMPTY: Only inserts if the projects field
            // is non-blank.
            // -------------------------------------------------------
            String projects = user.getProjects();
            if (projects != null && !projects.trim().isEmpty()) {
                projectStmt = conn.prepareStatement(INSERT_PROJECT_SQL);
                projectStmt.setInt(1, generatedResumeId);          // ?1 → resume_id
                projectStmt.setString(2, projects.trim());         // ?2 → project_name
                projectStmt.executeUpdate();
                System.out.println(
                    "[UserDAO] Project saved for resume_id: " + generatedResumeId
                );
            }

            // -------------------------------------------------------
            // TRANSACTION STEP 8: INSERT into certifications
            // -------------------------------------------------------
            //
            // Same as projects — only resume_id and
            // certification_name are NOT NULL.
            //
            // SKIP-IF-EMPTY: Only inserts if the certifications
            // field is non-blank.
            // -------------------------------------------------------
            String certifications = user.getCertifications();
            if (certifications != null && !certifications.trim().isEmpty()) {
                certificationStmt = conn.prepareStatement(INSERT_CERTIFICATION_SQL);
                certificationStmt.setInt(1, generatedResumeId);          // ?1 → resume_id
                certificationStmt.setString(2, certifications.trim());   // ?2 → certification_name
                certificationStmt.executeUpdate();
                System.out.println(
                    "[UserDAO] Certification saved for resume_id: " + generatedResumeId
                );
            }

            // -------------------------------------------------------
            // TRANSACTION STEP 9: COMMIT
            // -------------------------------------------------------
            //
            // conn.commit() makes ALL INSERTs permanent — users,
            // resumes, education, skills, experience, projects, and
            // certifications. Until this call, the rows are visible
            // only to THIS connection (ISOLATION in ACID).
            //
            // If ANY of the 8 steps above threw a SQLException, we
            // never reach this line — the catch block rolls back
            // everything. This guarantees ATOMICITY: all tables are
            // populated together or none are.
            //
            // After commit(), the data is DURABLE — it survives
            // server crashes, power failures, etc. (the D in ACID).
            // -------------------------------------------------------
            conn.commit();
            System.out.println(
                "[UserDAO] Transaction committed — all tables saved for "
                + "user_id: " + generatedUserId + ", resume_id: " + generatedResumeId
            );

            return generatedUserId;

        } catch (SQLException ex) {
            // -------------------------------------------------------
            // TRANSACTION ROLLBACK on failure
            // -------------------------------------------------------
            //
            // If ANY SQL statement fails (users, resumes, education,
            // skills, experience, projects, or certifications INSERT),
            // we land here.
            //
            // conn.rollback() undoes ALL statements executed since
            // setAutoCommit(false). This guarantees ATOMICITY:
            //   • If the certifications INSERT failed, ALL previous
            //     INSERTs (users, resumes, education, skills,
            //     experience, projects) are undone too.
            //   • The database is left in the exact state it was
            //     in before save() was called.
            //
            // We then re-throw the exception so the UI layer can
            // display the error message to the user.
            // -------------------------------------------------------
            System.err.println("[UserDAO] Transaction failed — rolling back all tables.");
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Rollback itself failed — log it but still
                    // throw the original exception so the caller
                    // knows the save failed.
                    rollbackEx.printStackTrace();
                }
            }
            throw ex;

        } finally {
            // -------------------------------------------------------
            // Clean up ALL resources (ALWAYS runs)
            // -------------------------------------------------------
            //
            // Close in REVERSE order of creation:
            //   ResultSets → PreparedStatements → Connection
            //
            // We also restore auto-commit to its default (true)
            // before closing. This is good practice in case the
            // connection is returned to a pool instead of closed —
            // the next user of the connection would otherwise
            // inherit our manual-commit mode unexpectedly.
            // -------------------------------------------------------

            // Close ResultSets
            if (skillIdRs != null) {
                try { skillIdRs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (resumeKeys != null) {
                try { resumeKeys.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (userKeys != null) {
                try { userKeys.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Close PreparedStatements (reverse order of creation)
            if (certificationStmt != null) {
                try { certificationStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (projectStmt != null) {
                try { projectStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (experienceStmt != null) {
                try { experienceStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (resumeSkillStmt != null) {
                try { resumeSkillStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (selectSkillStmt != null) {
                try { selectSkillStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (skillStmt != null) {
                try { skillStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (educationStmt != null) {
                try { educationStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (resumeStmt != null) {
                try { resumeStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (userStmt != null) {
                try { userStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Restore auto-commit before closing (pool safety)
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Close Connection
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
                    phone,                   // phone
                    "",                      // education   (separate table)
                    new ArrayList<>(),       // skills      (separate table)
                    "",                      // experience  (separate table)
                    "",                      // projects    (separate table)
                    "",                      // certifications (separate table)
                    "",                      // objective   (resumes table)
                    0                        // experienceYears
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
                    phone,                   // phone
                    "",                      // education   (separate table)
                    new ArrayList<>(),       // skills      (separate table)
                    "",                      // experience  (separate table)
                    "",                      // projects    (separate table)
                    "",                      // certifications (separate table)
                    "",                      // objective   (resumes table)
                    0                        // experienceYears
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
