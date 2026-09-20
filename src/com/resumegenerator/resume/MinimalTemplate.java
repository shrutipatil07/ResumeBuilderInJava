package com.resumegenerator.resume;

import com.resumegenerator.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MinimalTemplate implements ResumeTemplate {

    @Override
    public String render(com.resumegenerator.model.Resume resume) {
        StringBuilder sb = new StringBuilder();
        User user = resume.getUser();

        if (user != null) {
            if (user.getName() != null) sb.append(user.getName()).append("\n");
            sb.append(user.getEmail() != null ? user.getEmail() : "").append(" | ").append(user.getPhone() != null ? user.getPhone() : "").append("\n\n");
        }

        if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
            sb.append("Objective: ").append(resume.getObjective()).append("\n");
        }

        List<Education> eduList = resume.getEducationList().stream()
                .sorted(Comparator.comparingInt(Education::getDisplayOrder))
                .collect(Collectors.toList());
        if (!eduList.isEmpty()) {
            sb.append("Education: ");
            String edus = eduList.stream().map(Education::toString).collect(Collectors.joining("; "));
            sb.append(edus).append("\n");
        }

        List<Skill> skillList = resume.getSkillList().stream()
                .sorted(Comparator.comparingInt(Skill::getDisplayOrder))
                .collect(Collectors.toList());
        if (!skillList.isEmpty()) {
            sb.append("Skills: ");
            String sks = skillList.stream().map(Skill::getFormattedSkill).collect(Collectors.joining(", "));
            sb.append(sks).append("\n");
        }

        List<Experience> expList = resume.getExperienceList().stream()
                .sorted(Comparator.comparingInt(Experience::getDisplayOrder))
                .collect(Collectors.toList());
        if (!expList.isEmpty()) {
            sb.append("Experience: ");
            String exps = expList.stream().map(Experience::toString).collect(Collectors.joining("; "));
            sb.append(exps).append("\n");
        }

        List<Project> projList = resume.getProjectList().stream()
                .sorted(Comparator.comparingInt(Project::getDisplayOrder))
                .collect(Collectors.toList());
        if (!projList.isEmpty()) {
            sb.append("Projects: ");
            String projs = projList.stream().map(Project::toString).collect(Collectors.joining("; "));
            sb.append(projs).append("\n");
        }

        List<Certification> certList = resume.getCertificationList().stream()
                .sorted(Comparator.comparingInt(Certification::getDisplayOrder))
                .collect(Collectors.toList());
        if (!certList.isEmpty()) {
            sb.append("Certifications: ");
            String certs = certList.stream().map(Certification::toString).collect(Collectors.joining("; "));
            sb.append(certs).append("\n");
        }

        return sb.toString();
    }
}