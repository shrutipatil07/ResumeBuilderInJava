package com.resumegenerator.service;

import com.resumegenerator.dao.ResumeDAO;
import com.resumegenerator.model.Resume;
import com.resumegenerator.model.ResumeType;

import java.sql.SQLException;
import java.util.List;

/**
 * ResumeService — Business logic service for resume management and CRUD orchestration.
 *
 * Responsibilities:
 * - Validates business rules on Resume aggregates before persistence.
 * - Enforces account ownership boundaries.
 * - Delegates transaction management to ResumeDAO.
 */
public class ResumeService {

    private final ResumeDAO resumeDAO;

    public ResumeService() {
        this.resumeDAO = new ResumeDAO();
    }

    public ResumeService(ResumeDAO resumeDAO) {
        this.resumeDAO = resumeDAO;
    }

    /**
     * Validates and saves a Resume aggregate. Automatically determines whether to create or update.
     *
     * @param resume Resume aggregate with valid userId
     * @return saved resume_id
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException on database error
     */
    public int saveResume(Resume resume) throws SQLException {
        validateResume(resume);

        if (resume.getResumeId() > 0) {
            boolean updated = resumeDAO.update(resume);
            if (!updated) {
                throw new IllegalStateException("Failed to update resume #" + resume.getResumeId() + ". Ensure it exists and belongs to user #" + resume.getUserId());
            }
            return resume.getResumeId();
        } else {
            return resumeDAO.create(resume);
        }
    }

    /**
     * Retrieves a resume by ID ensuring ownership check for userId.
     *
     * @param resumeId primary key of resume
     * @param userId logged-in user account ID
     * @return Resume aggregate, or null if not found or ownership mismatch
     * @throws SQLException on database error
     */
    public Resume getResume(int resumeId, int userId) throws SQLException {
        if (resumeId <= 0 || userId <= 0) {
            return null;
        }
        return resumeDAO.findById(resumeId, userId);
    }

    /**
     * Fetches all resumes belonging to a user.
     *
     * @param userId logged-in user account ID
     * @return list of Resume aggregates
     * @throws SQLException on database error
     */
    public List<Resume> getUserResumes(int userId) throws SQLException {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }
        return resumeDAO.findAllByUserId(userId);
    }

    /**
     * Searches resumes for a user.
     *
     * @param userId user account ID
     * @param query search text keyword
     * @param typeFilter optional type filter
     * @return list of matching Resume aggregates
     * @throws SQLException on database error
     */
    public List<Resume> searchResumes(int userId, String query, String typeFilter) throws SQLException {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }
        return resumeDAO.searchByUserId(userId, query, typeFilter);
    }

    /**
     * Deletes a resume for a user enforcing ownership.
     *
     * @param resumeId primary key of resume
     * @param userId user account ID
     * @return true if deleted successfully
     * @throws SQLException on database error
     */
    public boolean deleteResume(int resumeId, int userId) throws SQLException {
        if (resumeId <= 0 || userId <= 0) {
            return false;
        }
        return resumeDAO.delete(resumeId, userId);
    }

    /**
     * Validates a Resume aggregate before saving.
     */
    public void validateResume(Resume resume) {
        if (resume == null) {
            throw new IllegalArgumentException("Resume object cannot be null.");
        }
        if (resume.getUserId() <= 0) {
            throw new IllegalArgumentException("Resume must be associated with a valid user ID.");
        }
        if (resume.getTitle() == null || resume.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Resume title cannot be blank.");
        }
        if (resume.getResumeType() == ResumeType.EXPERIENCED) {
            if (resume.getExperienceList() == null || resume.getExperienceList().isEmpty()) {
                throw new IllegalArgumentException("Experienced resumes require at least one Work Experience entry.");
            }
        }
    }
}
