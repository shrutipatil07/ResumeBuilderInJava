package com.resumegenerator.resume;

import com.resumegenerator.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClassicTemplate implements ResumeTemplate {

    @Override
    public String render(com.resumegenerator.model.Resume resume) {
        StringBuilder sb = new StringBuilder();
        User user = resume.getUser();

        sb.append("=========================================\n");
        sb.append(user != null && user.getName() != null ? user.getName() : "N/A").append("\n");
        sb.append("=========================================\n");
        if (user != null) {
            if (user.getEmail() != null) sb.append("Email: ").append(user.getEmail()).append("\n");
            if (user.getPhone() != null) sb.append("Phone: ").append(user.getPhone()).append("\n");
        }
        sb.append("\n");

        if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
            sb.append("OBJECTIVE\n");
            sb.append("---------\n");
            sb.append(resume.getObjective()).append("\n\n");
        }

        List<Education> eduList = resume.getEducationList().stream()
                .sorted(Comparator.comparingInt(Education::getDisplayOrder))
                .collect(Collectors.toList());
        if (!eduList.isEmpty()) {
            sb.append("EDUCATION\n");
            sb.append("---------\n");
            for (Education edu : eduList) {
                sb.append("• ").append(edu.getDegree());
                if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().isEmpty()) {
                    sb.append(" in ").append(edu.getFieldOfStudy());
                }
                sb.append(" - ").append(edu.getInstitution());
                String endStr = (edu.getEndYear() != null) ? String.valueOf(edu.getEndYear()) : "Present";
                sb.append(" (").append(edu.getStartYear()).append(" - ").append(endStr).append(")");
                if (edu.getGrade() != null && !edu.getGrade().isEmpty()) {
                    sb.append(" [Grade: ").append(edu.getGrade()).append("]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        List<Skill> skillList = resume.getSkillList().stream()
                .sorted(Comparator.comparingInt(Skill::getDisplayOrder))
                .collect(Collectors.toList());
        if (!skillList.isEmpty()) {
            sb.append("SKILLS\n");
            sb.append("------\n");
            String skillsFormatted = skillList.stream()
                    .map(Skill::toString)
                    .collect(Collectors.joining(", "));
            sb.append(skillsFormatted).append("\n\n");
        }

        List<Experience> expList = resume.getExperienceList().stream()
                .sorted(Comparator.comparingInt(Experience::getDisplayOrder))
                .collect(Collectors.toList());
        if (!expList.isEmpty()) {
            sb.append("EXPERIENCE\n");
            sb.append("----------\n");
            for (Experience exp : expList) {
                sb.append("• ").append(exp.getJobTitle()).append(" at ").append(exp.getCompanyName());
                if (exp.getLocation() != null && !exp.getLocation().isEmpty()) {
                    sb.append(" (").append(exp.getLocation()).append(")");
                }
                String endStr = (exp.getEndDate() != null) ? exp.getEndDate().toString() : "Present";
                sb.append(" [").append(exp.getStartDate()).append(" - ").append(endStr).append("]\n");
                if (exp.getDescription() != null && !exp.getDescription().isEmpty()) {
                    sb.append("   ").append(exp.getDescription()).append("\n");
                }
            }
            sb.append("\n");
        }

        List<Project> projList = resume.getProjectList().stream()
                .sorted(Comparator.comparingInt(Project::getDisplayOrder))
                .collect(Collectors.toList());
        if (!projList.isEmpty()) {
            sb.append("PROJECTS\n");
            sb.append("--------\n");
            for (Project proj : projList) {
                sb.append("• ").append(proj.getProjectName());
                if (proj.getTechStack() != null && !proj.getTechStack().isEmpty()) {
                    sb.append(" (").append(proj.getTechStack()).append(")");
                }
                sb.append("\n");
                if (proj.getDescription() != null && !proj.getDescription().isEmpty()) {
                    sb.append("   ").append(proj.getDescription()).append("\n");
                }
                if (proj.getProjectUrl() != null && !proj.getProjectUrl().isEmpty()) {
                    sb.append("   URL: ").append(proj.getProjectUrl()).append("\n");
                }
            }
            sb.append("\n");
        }

        List<Certification> certList = resume.getCertificationList().stream()
                .sorted(Comparator.comparingInt(Certification::getDisplayOrder))
                .collect(Collectors.toList());
        if (!certList.isEmpty()) {
            sb.append("CERTIFICATIONS\n");
            sb.append("--------------\n");
            for (Certification cert : certList) {
                sb.append("• ").append(cert.getCertificationName());
                if (cert.getIssuingOrg() != null && !cert.getIssuingOrg().isEmpty()) {
                    sb.append(" - ").append(cert.getIssuingOrg());
                }
                if (cert.getIssueDate() != null) {
                    sb.append(" (").append(cert.getIssueDate()).append(")");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}