package com.resumegenerator.resume;

import com.resumegenerator.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModernTemplate implements ResumeTemplate {

    @Override
    public String render(com.resumegenerator.model.Resume resume) {
        StringBuilder sb = new StringBuilder();
        User user = resume.getUser();

        sb.append(user != null && user.getName() != null ? user.getName() : "N/A");
        if (user != null) {
            if (user.getEmail() != null) sb.append("  |  ").append(user.getEmail());
            if (user.getPhone() != null) sb.append("  |  ").append(user.getPhone());
        }
        sb.append("\n\n");

        if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
            sb.append("» ").append(resume.getObjective()).append("\n\n");
        }

        List<Skill> skillList = resume.getSkillList().stream()
                .sorted(Comparator.comparingInt(Skill::getDisplayOrder))
                .collect(Collectors.toList());
        if (!skillList.isEmpty()) {
            sb.append("SKILLS  ");
            sb.append(skillList.stream().map(Skill::getSkillName).collect(Collectors.joining(" · ")));
            sb.append("\n\n");
        }

        List<Experience> expList = resume.getExperienceList().stream()
                .sorted(Comparator.comparingInt(Experience::getDisplayOrder))
                .collect(Collectors.toList());
        if (!expList.isEmpty()) {
            sb.append("EXPERIENCE\n");
            for (Experience exp : expList) {
                sb.append("  ").append(exp.getJobTitle()).append(" @ ").append(exp.getCompanyName());
                String endStr = (exp.getEndDate() != null) ? exp.getEndDate().toString() : "Present";
                sb.append(" (").append(exp.getStartDate()).append(" - ").append(endStr).append(")\n");
                if (exp.getDescription() != null && !exp.getDescription().isEmpty()) {
                    sb.append("    ").append(exp.getDescription()).append("\n");
                }
            }
            sb.append("\n");
        }

        List<Education> eduList = resume.getEducationList().stream()
                .sorted(Comparator.comparingInt(Education::getDisplayOrder))
                .collect(Collectors.toList());
        List<Certification> certList = resume.getCertificationList().stream()
                .sorted(Comparator.comparingInt(Certification::getDisplayOrder))
                .collect(Collectors.toList());

        if (!eduList.isEmpty() || !certList.isEmpty()) {
            sb.append("CREDENTIALS\n");
            for (Education edu : eduList) {
                sb.append("  Education: ").append(edu.getDegree()).append(" - ").append(edu.getInstitution()).append("\n");
            }
            for (Certification cert : certList) {
                sb.append("  Certification: ").append(cert.getCertificationName()).append(" (").append(cert.getIssuingOrg()).append(")\n");
            }
            sb.append("\n");
        }

        List<Project> projList = resume.getProjectList().stream()
                .sorted(Comparator.comparingInt(Project::getDisplayOrder))
                .collect(Collectors.toList());
        if (!projList.isEmpty()) {
            sb.append("PROJECTS\n");
            for (Project proj : projList) {
                sb.append("  » ").append(proj.getProjectName());
                if (proj.getTechStack() != null) sb.append(" [").append(proj.getTechStack()).append("]");
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}