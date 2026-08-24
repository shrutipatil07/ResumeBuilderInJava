package com.resumegenerator.resume;

import com.resumegenerator.model.User;

// ---------------------------------------------------------------
// ClassicTemplate — one concrete rendering strategy for
// ResumeTemplate. Formats a User's data as a traditional,
// section-by-section plain-text resume layout.
// ---------------------------------------------------------------
public class ClassicTemplate implements ResumeTemplate {

    @Override
    public String render(User user) {
        StringBuilder sb = new StringBuilder();

        sb.append("=========================================\n");
        sb.append(user.getName()).append("\n");
        sb.append("=========================================\n");
        sb.append("Email: ").append(user.getEmail()).append("\n");
        sb.append("Phone: ").append(user.getPhone()).append("\n\n");

        sb.append("OBJECTIVE\n");
        sb.append("---------\n");
        sb.append(user.getObjective()).append("\n\n");

        sb.append("EDUCATION\n");
        sb.append("---------\n");
        sb.append(user.getEducation()).append("\n\n");

        sb.append("SKILLS\n");
        sb.append("------\n");
        sb.append(String.join(", ", user.getSkills())).append("\n\n");

        sb.append("EXPERIENCE (").append(user.getExperienceYears()).append(" years)\n");
        sb.append("----------\n");
        sb.append(user.getExperienceDetails()).append("\n\n");

        sb.append("PROJECTS\n");
        sb.append("--------\n");
        sb.append(user.getProjects()).append("\n\n");

        sb.append("CERTIFICATIONS\n");
        sb.append("--------------\n");
        sb.append(user.getCertifications()).append("\n");

        return sb.toString();
    }
}