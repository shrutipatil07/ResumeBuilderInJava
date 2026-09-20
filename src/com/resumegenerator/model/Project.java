package com.resumegenerator.model;

public class Project {
    private int projectId;
    private int resumeId;
    private String projectName;
    private String description;
    private String techStack;
    private String projectUrl;
    private int displayOrder;

    public Project() {
    }

    public Project(String projectName, String description, String techStack, String projectUrl, int displayOrder) {
        this.projectName = projectName;
        this.description = description;
        this.techStack = techStack;
        this.projectUrl = projectUrl;
        this.displayOrder = displayOrder;
    }

    public Project(int projectId, int resumeId, String projectName, String description, String techStack, String projectUrl, int displayOrder) {
        this.projectId = projectId;
        this.resumeId = resumeId;
        this.projectName = projectName;
        this.description = description;
        this.techStack = techStack;
        this.projectUrl = projectUrl;
        this.displayOrder = displayOrder;
    }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public int getResumeId() { return resumeId; }
    public void setResumeId(int resumeId) { this.resumeId = resumeId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public String getFormattedProject() {
        StringBuilder sb = new StringBuilder();
        sb.append(projectName);
        if (techStack != null && !techStack.trim().isEmpty()) {
            sb.append(" [").append(techStack.trim()).append("]");
        }
        if (projectUrl != null && !projectUrl.trim().isEmpty()) {
            sb.append(" (").append(projectUrl.trim()).append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedProject();
    }
}
