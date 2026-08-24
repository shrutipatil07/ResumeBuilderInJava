package com.resumegenerator.resume;

import com.resumegenerator.model.User;

// ---------------------------------------------------------------
// MinimalTemplate — third concrete rendering strategy for
// ResumeTemplate. Deliberately stripped down: no banners, no
// section underlines, no grouping — just labeled lines of data,
// one after another, in the plainest form the content allows.
// ---------------------------------------------------------------
public class MinimalTemplate implements ResumeTemplate {

    @Override
    public String render(User user) {
        StringBuilder sb = new StringBuilder();

        sb.append(user.getName()).append("\n");
        sb.append(user.getEmail()).append(" | ").append(user.getPhone()).append("\n\n");

        sb.append("Objective: ").append(user.getObjective()).append("\n");
        sb.append("Education: ").append(user.getEducation()).append("\n");
        sb.append("Skills: ").append(String.join(", ", user.getSkills())).append("\n");
        sb.append("Experience: ").append(user.getExperienceYears()).append(" yrs - ")
          .append(user.getExperienceDetails()).append("\n");
        sb.append("Projects: ").append(user.getProjects()).append("\n");
        sb.append("Certifications: ").append(user.getCertifications()).append("\n");

        return sb.toString();
    }
}