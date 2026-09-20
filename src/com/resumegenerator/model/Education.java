package com.resumegenerator.model;

public class Education {
    private int educationId;
    private int resumeId;
    private String institution;
    private String degree;
    private String fieldOfStudy;
    private int startYear;
    private Integer endYear; // Nullable if currently pursuing
    private String grade;
    private int displayOrder;

    public Education() {
    }

    public Education(String institution, String degree, String fieldOfStudy, int startYear, Integer endYear, String grade, int displayOrder) {
        this.institution = institution;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.startYear = startYear;
        this.endYear = endYear;
        this.grade = grade;
        this.displayOrder = displayOrder;
    }

    public Education(int educationId, int resumeId, String institution, String degree, String fieldOfStudy, int startYear, Integer endYear, String grade, int displayOrder) {
        this.educationId = educationId;
        this.resumeId = resumeId;
        this.institution = institution;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.startYear = startYear;
        this.endYear = endYear;
        this.grade = grade;
        this.displayOrder = displayOrder;
    }

    public int getEducationId() { return educationId; }
    public void setEducationId(int educationId) { this.educationId = educationId; }

    public int getResumeId() { return resumeId; }
    public void setResumeId(int resumeId) { this.resumeId = resumeId; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }

    public int getStartYear() { return startYear; }
    public void setStartYear(int startYear) { this.startYear = startYear; }

    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public String getFormattedEducation() {
        StringBuilder sb = new StringBuilder();
        sb.append(degree);
        if (fieldOfStudy != null && !fieldOfStudy.trim().isEmpty()) {
            sb.append(" in ").append(fieldOfStudy.trim());
        }
        sb.append(" - ").append(institution);
        if (startYear > 0) {
            sb.append(" (").append(startYear);
            if (endYear != null && endYear > 0) {
                sb.append(" - ").append(endYear);
            } else {
                sb.append(" - Present");
            }
            sb.append(")");
        }
        if (grade != null && !grade.trim().isEmpty()) {
            sb.append(" [Grade: ").append(grade.trim()).append("]");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedEducation();
    }
}
