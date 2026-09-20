package com.resumegenerator.model;

import com.resumegenerator.resume.TemplateType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Resume {
    private int resumeId;
    private int userId;
    private String title;
    private ResumeType resumeType;
    private String objective;
    private TemplateType templateType = TemplateType.CLASSIC;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Contact/Account entity linked to this resume
    private User user;

    private List<Education> educationList = new ArrayList<>();
    private List<Experience> experienceList = new ArrayList<>();
    private List<Project> projectList = new ArrayList<>();
    private List<Certification> certificationList = new ArrayList<>();
    private List<Skill> skillList = new ArrayList<>();

    public Resume() {
    }

    public Resume(int userId, String title, ResumeType resumeType) {
        this.userId = userId;
        this.title = title;
        this.resumeType = resumeType;
    }

    public int getResumeId() { return resumeId; }
    public void setResumeId(int resumeId) { this.resumeId = resumeId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ResumeType getResumeType() { return resumeType; }
    public void setResumeType(ResumeType resumeType) { this.resumeType = resumeType; }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public TemplateType getTemplateType() { return templateType; }
    public void setTemplateType(TemplateType templateType) { this.templateType = templateType; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Education> getEducationList() { return educationList; }
    public void setEducationList(List<Education> educationList) { this.educationList = educationList; }

    public List<Experience> getExperienceList() { return experienceList; }
    public void setExperienceList(List<Experience> experienceList) { this.experienceList = experienceList; }

    public List<Project> getProjectList() { return projectList; }
    public void setProjectList(List<Project> projectList) { this.projectList = projectList; }

    public List<Certification> getCertificationList() { return certificationList; }
    public void setCertificationList(List<Certification> certificationList) { this.certificationList = certificationList; }

    public List<Skill> getSkillList() { return skillList; }
    public void setSkillList(List<Skill> skillList) { this.skillList = skillList; }

    public void addEducation(Education education) {
        if (education.getDisplayOrder() <= 0) {
            education.setDisplayOrder(educationList.size() + 1);
        }
        this.educationList.add(education);
    }

    public void addExperience(Experience experience) {
        if (experience.getDisplayOrder() <= 0) {
            experience.setDisplayOrder(experienceList.size() + 1);
        }
        this.experienceList.add(experience);
    }

    public void addProject(Project project) {
        if (project.getDisplayOrder() <= 0) {
            project.setDisplayOrder(projectList.size() + 1);
        }
        this.projectList.add(project);
    }

    public void addCertification(Certification certification) {
        if (certification.getDisplayOrder() <= 0) {
            certification.setDisplayOrder(certificationList.size() + 1);
        }
        this.certificationList.add(certification);
    }

    public void addSkill(Skill skill) {
        if (skill.getDisplayOrder() <= 0) {
            skill.setDisplayOrder(skillList.size() + 1);
        }
        this.skillList.add(skill);
    }
}
