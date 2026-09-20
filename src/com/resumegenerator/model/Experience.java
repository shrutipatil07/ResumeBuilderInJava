package com.resumegenerator.model;

import java.time.LocalDate;

public class Experience {
    private int experienceId;
    private int resumeId;
    private String companyName;
    private String jobTitle;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate; // Nullable if currently working
    private String description;
    private int displayOrder;

    public Experience() {
    }

    public Experience(String companyName, String jobTitle, String location, LocalDate startDate, LocalDate endDate, String description, int displayOrder) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public Experience(int experienceId, int resumeId, String companyName, String jobTitle, String location, LocalDate startDate, LocalDate endDate, String description, int displayOrder) {
        this.experienceId = experienceId;
        this.resumeId = resumeId;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public int getExperienceId() { return experienceId; }
    public void setExperienceId(int experienceId) { this.experienceId = experienceId; }

    public int getResumeId() { return resumeId; }
    public void setResumeId(int resumeId) { this.resumeId = resumeId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public String getFormattedExperience() {
        StringBuilder sb = new StringBuilder();
        sb.append(jobTitle).append(" at ").append(companyName);
        if (location != null && !location.trim().isEmpty()) {
            sb.append(" (").append(location.trim()).append(")");
        }
        if (startDate != null) {
            sb.append(" [").append(startDate);
            if (endDate != null) {
                sb.append(" - ").append(endDate);
            } else {
                sb.append(" - Present");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedExperience();
    }
}
