package com.resumegenerator.dao;

import com.resumegenerator.db.DatabaseManager;
import com.resumegenerator.model.*;
import com.resumegenerator.resume.TemplateType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ResumeDAO — Data Access Object owning all persistence operations for the Resume aggregate.
 *
 * Design features:
 * - Enforces account ownership on read, update, and delete (user_id + resume_id checks).
 * - Executes create and update operations inside single atomic database transactions.
 * - Hydrates complete Resume domain aggregates (header + education, skills, experience, projects, certifications).
 */
public class ResumeDAO {

    private final AccountDAO accountDAO = new AccountDAO();

    // Header statements
    private static final String INSERT_RESUME_SQL =
        "INSERT INTO resumes (user_id, title, resume_type, objective, template_type) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_RESUME_HEADER_SQL =
        "UPDATE resumes SET title = ?, resume_type = ?, objective = ?, template_type = ?, updated_at = CURRENT_TIMESTAMP WHERE resume_id = ? AND user_id = ?";

    private static final String SELECT_RESUME_BY_ID_AND_USER_ID_SQL =
        "SELECT resume_id, user_id, title, resume_type, objective, template_type, created_at, updated_at FROM resumes WHERE resume_id = ? AND user_id = ?";

    private static final String SELECT_RESUMES_BY_USER_ID_SQL =
        "SELECT resume_id, user_id, title, resume_type, objective, template_type, created_at, updated_at FROM resumes WHERE user_id = ? ORDER BY updated_at DESC, created_at DESC";

    private static final String SEARCH_RESUMES_SQL =
        "SELECT resume_id, user_id, title, resume_type, objective, template_type, created_at, updated_at FROM resumes WHERE user_id = ? AND (title LIKE ? OR objective LIKE ?) ORDER BY updated_at DESC";

    private static final String SEARCH_RESUMES_WITH_TYPE_SQL =
        "SELECT resume_id, user_id, title, resume_type, objective, template_type, created_at, updated_at FROM resumes WHERE user_id = ? AND resume_type = ? AND (title LIKE ? OR objective LIKE ?) ORDER BY updated_at DESC";

    private static final String DELETE_RESUME_SQL =
        "DELETE FROM resumes WHERE resume_id = ? AND user_id = ?";

    // Child table insert statements
    private static final String INSERT_EDUCATION_SQL =
        "INSERT INTO education (resume_id, institution, degree, field_of_study, start_year, end_year, grade, display_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_SKILL_SQL =
        "INSERT IGNORE INTO skills (skill_name) VALUES (?)";

    private static final String SELECT_SKILL_ID_SQL =
        "SELECT skill_id FROM skills WHERE skill_name = ?";

    private static final String INSERT_RESUME_SKILL_SQL =
        "INSERT INTO resume_skills (resume_id, skill_id, proficiency_level, display_order) VALUES (?, ?, ?, ?)";

    private static final String INSERT_EXPERIENCE_SQL =
        "INSERT INTO experience (resume_id, company_name, job_title, location, start_date, end_date, description, display_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_PROJECT_SQL =
        "INSERT INTO projects (resume_id, project_name, description, tech_stack, project_url, display_order) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_CERTIFICATION_SQL =
        "INSERT INTO certifications (resume_id, certification_name, issuing_org, issue_date, credential_url, display_order) VALUES (?, ?, ?, ?, ?, ?)";

    // Child table delete statements
    private static final String DELETE_EDUCATION_SQL = "DELETE FROM education WHERE resume_id = ?";
    private static final String DELETE_RESUME_SKILLS_SQL = "DELETE FROM resume_skills WHERE resume_id = ?";
    private static final String DELETE_EXPERIENCE_SQL = "DELETE FROM experience WHERE resume_id = ?";
    private static final String DELETE_PROJECTS_SQL = "DELETE FROM projects WHERE resume_id = ?";
    private static final String DELETE_CERTIFICATIONS_SQL = "DELETE FROM certifications WHERE resume_id = ?";

    // Child table select statements
    private static final String SELECT_EDUCATION_SQL =
        "SELECT institution, degree, field_of_study, start_year, end_year, grade, display_order FROM education WHERE resume_id = ? ORDER BY display_order ASC";

    private static final String SELECT_SKILLS_SQL =
        "SELECT s.skill_id, s.skill_name, rs.proficiency_level, rs.display_order FROM skills s JOIN resume_skills rs ON s.skill_id = rs.skill_id WHERE rs.resume_id = ? ORDER BY rs.display_order ASC";

    private static final String SELECT_EXPERIENCE_SQL =
        "SELECT company_name, job_title, location, start_date, end_date, description, display_order FROM experience WHERE resume_id = ? ORDER BY display_order ASC";

    private static final String SELECT_PROJECTS_SQL =
        "SELECT project_name, description, tech_stack, project_url, display_order FROM projects WHERE resume_id = ? ORDER BY display_order ASC";

    private static final String SELECT_CERTIFICATIONS_SQL =
        "SELECT certification_name, issuing_org, issue_date, credential_url, display_order FROM certifications WHERE resume_id = ? ORDER BY display_order ASC";


    /**
     * Creates a new Resume aggregate in the database in a single atomic transaction.
     *
     * @param resume Resume aggregate populated with child components and valid userId
     * @return generated resume_id (> 0) on success
     * @throws SQLException on database error
     */
    public int create(Resume resume) throws SQLException {
        if (resume == null || resume.getUserId() <= 0) {
            throw new IllegalArgumentException("Cannot create resume without valid userId.");
        }

        Connection conn = null;
        PreparedStatement resumeStmt = null;
        ResultSet keys = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            resumeStmt = conn.prepareStatement(INSERT_RESUME_SQL, Statement.RETURN_GENERATED_KEYS);
            resumeStmt.setInt(1, resume.getUserId());
            resumeStmt.setString(2, resume.getTitle() != null ? resume.getTitle() : "Untitled Resume");
            resumeStmt.setString(3, resume.getResumeType() != null ? resume.getResumeType().name() : ResumeType.FRESHER.name());
            if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
                resumeStmt.setString(4, resume.getObjective().trim());
            } else {
                resumeStmt.setNull(4, Types.VARCHAR);
            }
            resumeStmt.setString(5, resume.getTemplateType() != null ? resume.getTemplateType().name() : TemplateType.CLASSIC.name());

            resumeStmt.executeUpdate();
            keys = resumeStmt.getGeneratedKeys();

            int generatedResumeId = -1;
            if (keys.next()) {
                generatedResumeId = keys.getInt(1);
            }

            if (generatedResumeId <= 0) {
                conn.rollback();
                throw new SQLException("Failed to retrieve generated resume_id.");
            }

            resume.setResumeId(generatedResumeId);

            // Insert child lists using current transaction
            insertChildRecords(conn, generatedResumeId, resume);

            conn.commit();
            return generatedResumeId;

        } catch (SQLException ex) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw ex;
        } finally {
            if (keys != null) { try { keys.close(); } catch (SQLException ignored) {} }
            if (resumeStmt != null) { try { resumeStmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    /**
     * Finds a complete Resume aggregate by resumeId and userId (enforcing ownership).
     *
     * @param resumeId primary key of resume
     * @param userId owner's account ID
     * @return complete Resume aggregate, or null if not found or ownership mismatch
     * @throws SQLException on database error
     */
    public Resume findById(int resumeId, int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(SELECT_RESUME_BY_ID_AND_USER_ID_SQL);
            pstmt.setInt(1, resumeId);
            pstmt.setInt(2, userId);
            rs = pstmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            Resume resume = new Resume();
            resume.setResumeId(rs.getInt("resume_id"));
            resume.setUserId(rs.getInt("user_id"));
            resume.setTitle(rs.getString("title"));
            String rType = rs.getString("resume_type");
            if (rType != null) {
                try { resume.setResumeType(ResumeType.valueOf(rType)); } catch (Exception ignored) {}
            }
            resume.setObjective(rs.getString("objective"));
            String tType = rs.getString("template_type");
            if (tType != null) {
                try { resume.setTemplateType(TemplateType.valueOf(tType)); } catch (Exception ignored) {}
            }
            resume.setCreatedAt(rs.getTimestamp("created_at"));
            resume.setUpdatedAt(rs.getTimestamp("updated_at"));

            // Attach owner contact info
            User owner = accountDAO.findById(userId);
            resume.setUser(owner);

            // Load child lists
            loadChildRecords(conn, resume);

            return resume;

        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
            if (pstmt != null) { try { pstmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    /**
     * Retrieves all resumes owned by userId.
     *
     * @param userId user account ID
     * @return List of Resume objects (hydrated with child records)
     * @throws SQLException on database error
     */
    public List<Resume> findAllByUserId(int userId) throws SQLException {
        List<Resume> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(SELECT_RESUMES_BY_USER_ID_SQL);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();

            User owner = accountDAO.findById(userId);

            while (rs.next()) {
                Resume resume = new Resume();
                resume.setResumeId(rs.getInt("resume_id"));
                resume.setUserId(rs.getInt("user_id"));
                resume.setTitle(rs.getString("title"));
                String rType = rs.getString("resume_type");
                if (rType != null) {
                    try { resume.setResumeType(ResumeType.valueOf(rType)); } catch (Exception ignored) {}
                }
                resume.setObjective(rs.getString("objective"));
                String tType = rs.getString("template_type");
                if (tType != null) {
                    try { resume.setTemplateType(TemplateType.valueOf(tType)); } catch (Exception ignored) {}
                }
                resume.setCreatedAt(rs.getTimestamp("created_at"));
                resume.setUpdatedAt(rs.getTimestamp("updated_at"));
                resume.setUser(owner);

                loadChildRecords(conn, resume);
                list.add(resume);
            }

            return list;

        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
            if (pstmt != null) { try { pstmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    /**
     * Searches resumes for a user by query string (matching title or objective) and optional type filter.
     *
     * @param userId user account ID
     * @param query search keyword
     * @param typeFilter optional ResumeType string filter (e.g. "FRESHER", "EXPERIENCED", or null/empty for all)
     * @return matching list of Resume aggregates
     * @throws SQLException on database error
     */
    public List<Resume> searchByUserId(int userId, String query, String typeFilter) throws SQLException {
        List<Resume> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String searchPattern = "%" + (query != null ? query.trim() : "") + "%";
        boolean filterByType = typeFilter != null && !typeFilter.trim().isEmpty();

        try {
            conn = DatabaseManager.getConnection();
            if (filterByType) {
                pstmt = conn.prepareStatement(SEARCH_RESUMES_WITH_TYPE_SQL);
                pstmt.setInt(1, userId);
                pstmt.setString(2, typeFilter.trim());
                pstmt.setString(3, searchPattern);
                pstmt.setString(4, searchPattern);
            } else {
                pstmt = conn.prepareStatement(SEARCH_RESUMES_SQL);
                pstmt.setInt(1, userId);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
            }

            rs = pstmt.executeQuery();
            User owner = accountDAO.findById(userId);

            while (rs.next()) {
                Resume resume = new Resume();
                resume.setResumeId(rs.getInt("resume_id"));
                resume.setUserId(rs.getInt("user_id"));
                resume.setTitle(rs.getString("title"));
                String rType = rs.getString("resume_type");
                if (rType != null) {
                    try { resume.setResumeType(ResumeType.valueOf(rType)); } catch (Exception ignored) {}
                }
                resume.setObjective(rs.getString("objective"));
                String tType = rs.getString("template_type");
                if (tType != null) {
                    try { resume.setTemplateType(TemplateType.valueOf(tType)); } catch (Exception ignored) {}
                }
                resume.setCreatedAt(rs.getTimestamp("created_at"));
                resume.setUpdatedAt(rs.getTimestamp("updated_at"));
                resume.setUser(owner);

                loadChildRecords(conn, resume);
                list.add(resume);
            }

            return list;

        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
            if (pstmt != null) { try { pstmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    /**
     * Updates an existing Resume aggregate in a single atomic transaction.
     * Enforces user ownership (`WHERE resume_id = ? AND user_id = ?`).
     * Replaces child entries cleanly.
     *
     * @param resume Resume aggregate with valid resumeId and userId
     * @return true if updated successfully
     * @throws SQLException on database error
     */
    public boolean update(Resume resume) throws SQLException {
        if (resume == null || resume.getResumeId() <= 0 || resume.getUserId() <= 0) {
            return false;
        }

        Connection conn = null;
        PreparedStatement headerStmt = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // Update header
            headerStmt = conn.prepareStatement(UPDATE_RESUME_HEADER_SQL);
            headerStmt.setString(1, resume.getTitle() != null ? resume.getTitle() : "Untitled Resume");
            headerStmt.setString(2, resume.getResumeType() != null ? resume.getResumeType().name() : ResumeType.FRESHER.name());
            if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
                headerStmt.setString(3, resume.getObjective().trim());
            } else {
                headerStmt.setNull(3, Types.VARCHAR);
            }
            headerStmt.setString(4, resume.getTemplateType() != null ? resume.getTemplateType().name() : TemplateType.CLASSIC.name());
            headerStmt.setInt(5, resume.getResumeId());
            headerStmt.setInt(6, resume.getUserId());

            int rows = headerStmt.executeUpdate();
            if (rows == 0) {
                // Resume ID does not exist or userId ownership check failed
                conn.rollback();
                return false;
            }

            // Remove existing child records
            deleteChildRecords(conn, resume.getResumeId());

            // Insert updated child lists
            insertChildRecords(conn, resume.getResumeId(), resume);

            conn.commit();
            return true;

        } catch (SQLException ex) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw ex;
        } finally {
            if (headerStmt != null) { try { headerStmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    /**
     * Deletes a resume aggregate by resumeId and userId (enforcing ownership).
     *
     * @param resumeId primary key of resume
     * @param userId owner user account ID
     * @return true if deleted successfully, false if resumeId not found or not owned by userId
     * @throws SQLException on database error
     */
    public boolean delete(int resumeId, int userId) throws SQLException {
        if (resumeId <= 0 || userId <= 0) {
            return false;
        }

        Connection conn = null;
        PreparedStatement deleteStmt = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // Delete child records first for explicit database safety
            deleteChildRecords(conn, resumeId);

            deleteStmt = conn.prepareStatement(DELETE_RESUME_SQL);
            deleteStmt.setInt(1, resumeId);
            deleteStmt.setInt(2, userId);

            int affected = deleteStmt.executeUpdate();
            if (affected > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }

        } catch (SQLException ex) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw ex;
        } finally {
            if (deleteStmt != null) { try { deleteStmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    // Helper method to insert all child records in active transaction
    private void insertChildRecords(Connection conn, int resumeId, Resume resume) throws SQLException {
        // Education
        if (resume.getEducationList() != null && !resume.getEducationList().isEmpty()) {
            try (PreparedStatement eduStmt = conn.prepareStatement(INSERT_EDUCATION_SQL)) {
                for (Education edu : resume.getEducationList()) {
                    eduStmt.setInt(1, resumeId);
                    eduStmt.setString(2, edu.getInstitution());
                    eduStmt.setString(3, edu.getDegree());
                    if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().trim().isEmpty()) {
                        eduStmt.setString(4, edu.getFieldOfStudy().trim());
                    } else {
                        eduStmt.setNull(4, Types.VARCHAR);
                    }
                    eduStmt.setInt(5, edu.getStartYear());
                    if (edu.getEndYear() != null) {
                        eduStmt.setInt(6, edu.getEndYear());
                    } else {
                        eduStmt.setNull(6, Types.INTEGER);
                    }
                    if (edu.getGrade() != null && !edu.getGrade().trim().isEmpty()) {
                        eduStmt.setString(7, edu.getGrade().trim());
                    } else {
                        eduStmt.setNull(7, Types.VARCHAR);
                    }
                    eduStmt.setInt(8, edu.getDisplayOrder());
                    eduStmt.executeUpdate();
                }
            }
        }

        // Skills
        if (resume.getSkillList() != null && !resume.getSkillList().isEmpty()) {
            try (PreparedStatement skillStmt = conn.prepareStatement(INSERT_SKILL_SQL);
                 PreparedStatement selectSkillStmt = conn.prepareStatement(SELECT_SKILL_ID_SQL);
                 PreparedStatement resumeSkillStmt = conn.prepareStatement(INSERT_RESUME_SKILL_SQL)) {

                for (Skill skill : resume.getSkillList()) {
                    if (skill.getSkillName() == null || skill.getSkillName().trim().isEmpty()) continue;
                    String sName = skill.getSkillName().trim();

                    skillStmt.setString(1, sName);
                    skillStmt.executeUpdate();

                    selectSkillStmt.setString(1, sName);
                    try (ResultSet skillIdRs = selectSkillStmt.executeQuery()) {
                        if (skillIdRs.next()) {
                            int skillId = skillIdRs.getInt(1);
                            skill.setSkillId(skillId);
                            resumeSkillStmt.setInt(1, resumeId);
                            resumeSkillStmt.setInt(2, skillId);
                            if (skill.getProficiencyLevel() != null) {
                                resumeSkillStmt.setString(3, skill.getProficiencyLevel().name());
                            } else {
                                resumeSkillStmt.setNull(3, Types.VARCHAR);
                            }
                            resumeSkillStmt.setInt(4, skill.getDisplayOrder());
                            resumeSkillStmt.executeUpdate();
                        }
                    }
                }
            }
        }

        // Experience
        if (resume.getExperienceList() != null && !resume.getExperienceList().isEmpty()) {
            try (PreparedStatement expStmt = conn.prepareStatement(INSERT_EXPERIENCE_SQL)) {
                for (Experience exp : resume.getExperienceList()) {
                    expStmt.setInt(1, resumeId);
                    expStmt.setString(2, exp.getCompanyName());
                    expStmt.setString(3, exp.getJobTitle());
                    if (exp.getLocation() != null && !exp.getLocation().trim().isEmpty()) {
                        expStmt.setString(4, exp.getLocation().trim());
                    } else {
                        expStmt.setNull(4, Types.VARCHAR);
                    }
                    expStmt.setDate(5, Date.valueOf(exp.getStartDate()));
                    if (exp.getEndDate() != null) {
                        expStmt.setDate(6, Date.valueOf(exp.getEndDate()));
                    } else {
                        expStmt.setNull(6, Types.DATE);
                    }
                    if (exp.getDescription() != null && !exp.getDescription().trim().isEmpty()) {
                        expStmt.setString(7, exp.getDescription().trim());
                    } else {
                        expStmt.setNull(7, Types.VARCHAR);
                    }
                    expStmt.setInt(8, exp.getDisplayOrder());
                    expStmt.executeUpdate();
                }
            }
        }

        // Projects
        if (resume.getProjectList() != null && !resume.getProjectList().isEmpty()) {
            try (PreparedStatement projStmt = conn.prepareStatement(INSERT_PROJECT_SQL)) {
                for (Project proj : resume.getProjectList()) {
                    projStmt.setInt(1, resumeId);
                    projStmt.setString(2, proj.getProjectName());
                    if (proj.getDescription() != null && !proj.getDescription().trim().isEmpty()) {
                        projStmt.setString(3, proj.getDescription().trim());
                    } else {
                        projStmt.setNull(3, Types.VARCHAR);
                    }
                    if (proj.getTechStack() != null && !proj.getTechStack().trim().isEmpty()) {
                        projStmt.setString(4, proj.getTechStack().trim());
                    } else {
                        projStmt.setNull(4, Types.VARCHAR);
                    }
                    if (proj.getProjectUrl() != null && !proj.getProjectUrl().trim().isEmpty()) {
                        projStmt.setString(5, proj.getProjectUrl().trim());
                    } else {
                        projStmt.setNull(5, Types.VARCHAR);
                    }
                    projStmt.setInt(6, proj.getDisplayOrder());
                    projStmt.executeUpdate();
                }
            }
        }

        // Certifications
        if (resume.getCertificationList() != null && !resume.getCertificationList().isEmpty()) {
            try (PreparedStatement certStmt = conn.prepareStatement(INSERT_CERTIFICATION_SQL)) {
                for (Certification cert : resume.getCertificationList()) {
                    certStmt.setInt(1, resumeId);
                    certStmt.setString(2, cert.getCertificationName());
                    certStmt.setString(3, cert.getIssuingOrg());
                    if (cert.getIssueDate() != null) {
                        certStmt.setDate(4, Date.valueOf(cert.getIssueDate()));
                    } else {
                        certStmt.setNull(4, Types.DATE);
                    }
                    if (cert.getCredentialUrl() != null && !cert.getCredentialUrl().trim().isEmpty()) {
                        certStmt.setString(5, cert.getCredentialUrl().trim());
                    } else {
                        certStmt.setNull(5, Types.VARCHAR);
                    }
                    certStmt.setInt(6, cert.getDisplayOrder());
                    certStmt.executeUpdate();
                }
            }
        }
    }

    // Helper method to delete all child records for a resume
    private void deleteChildRecords(Connection conn, int resumeId) throws SQLException {
        try (PreparedStatement d1 = conn.prepareStatement(DELETE_EDUCATION_SQL);
             PreparedStatement d2 = conn.prepareStatement(DELETE_RESUME_SKILLS_SQL);
             PreparedStatement d3 = conn.prepareStatement(DELETE_EXPERIENCE_SQL);
             PreparedStatement d4 = conn.prepareStatement(DELETE_PROJECTS_SQL);
             PreparedStatement d5 = conn.prepareStatement(DELETE_CERTIFICATIONS_SQL)) {
            d1.setInt(1, resumeId); d1.executeUpdate();
            d2.setInt(1, resumeId); d2.executeUpdate();
            d3.setInt(1, resumeId); d3.executeUpdate();
            d4.setInt(1, resumeId); d4.executeUpdate();
            d5.setInt(1, resumeId); d5.executeUpdate();
        }
    }

    // Helper method to load child records into resume aggregate
    private void loadChildRecords(Connection conn, Resume resume) throws SQLException {
        int resumeId = resume.getResumeId();

        // Education
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_EDUCATION_SQL)) {
            pstmt.setInt(1, resumeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Education edu = new Education();
                    edu.setInstitution(rs.getString("institution"));
                    edu.setDegree(rs.getString("degree"));
                    edu.setFieldOfStudy(rs.getString("field_of_study"));
                    edu.setStartYear(rs.getInt("start_year"));
                    int endYr = rs.getInt("end_year");
                    if (!rs.wasNull()) {
                        edu.setEndYear(endYr);
                    }
                    edu.setGrade(rs.getString("grade"));
                    edu.setDisplayOrder(rs.getInt("display_order"));
                    resume.addEducation(edu);
                }
            }
        }

        // Skills
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_SKILLS_SQL)) {
            pstmt.setInt(1, resumeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Skill skill = new Skill();
                    skill.setSkillId(rs.getInt("skill_id"));
                    skill.setSkillName(rs.getString("skill_name"));
                    String pLevel = rs.getString("proficiency_level");
                    if (pLevel != null) {
                        try { skill.setProficiencyLevel(ProficiencyLevel.valueOf(pLevel)); } catch (Exception ignored) {}
                    }
                    skill.setDisplayOrder(rs.getInt("display_order"));
                    resume.addSkill(skill);
                }
            }
        }

        // Experience
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_EXPERIENCE_SQL)) {
            pstmt.setInt(1, resumeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Experience exp = new Experience();
                    exp.setCompanyName(rs.getString("company_name"));
                    exp.setJobTitle(rs.getString("job_title"));
                    exp.setLocation(rs.getString("location"));
                    Date stDate = rs.getDate("start_date");
                    if (stDate != null) exp.setStartDate(stDate.toLocalDate());
                    Date enDate = rs.getDate("end_date");
                    if (enDate != null) exp.setEndDate(enDate.toLocalDate());
                    exp.setDescription(rs.getString("description"));
                    exp.setDisplayOrder(rs.getInt("display_order"));
                    resume.addExperience(exp);
                }
            }
        }

        // Projects
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_PROJECTS_SQL)) {
            pstmt.setInt(1, resumeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Project proj = new Project();
                    proj.setProjectName(rs.getString("project_name"));
                    proj.setDescription(rs.getString("description"));
                    proj.setTechStack(rs.getString("tech_stack"));
                    proj.setProjectUrl(rs.getString("project_url"));
                    proj.setDisplayOrder(rs.getInt("display_order"));
                    resume.addProject(proj);
                }
            }
        }

        // Certifications
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_CERTIFICATIONS_SQL)) {
            pstmt.setInt(1, resumeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Certification cert = new Certification();
                    cert.setCertificationName(rs.getString("certification_name"));
                    cert.setIssuingOrg(rs.getString("issuing_org"));
                    Date iDate = rs.getDate("issue_date");
                    if (iDate != null) cert.setIssueDate(iDate.toLocalDate());
                    cert.setCredentialUrl(rs.getString("credential_url"));
                    cert.setDisplayOrder(rs.getInt("display_order"));
                    resume.addCertification(cert);
                }
            }
        }
    }
}
