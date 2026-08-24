package com.resumegenerator.resume;

import com.resumegenerator.model.User;

// ---------------------------------------------------------------
// ModernTemplate — a second concrete rendering strategy for
// ResumeTemplate. Unlike ClassicTemplate's traditional, formal,
// section-header layout, this favors a compact, scannable,
// header-line style closer to a modern one-page resume format.
// ---------------------------------------------------------------
public class ModernTemplate implements ResumeTemplate {

    @Override
    public String render(User user) {
        StringBuilder sb = new StringBuilder();

        // Compact single-line identity header instead of a boxed banner
        sb.append(user.getName())
          .append("  |  ").append(user.getEmail())
          .append("  |  ").append(user.getPhone())
          .append("\n\n");

        // Objective leads immediately, inline, no section underline
        sb.append("» ").append(user.getObjective()).append("\n\n");

        // Skills rendered as a tag-style inline list, not a paragraph
        sb.append("SKILLS  ");
        sb.append(String.join(" · ", user.getSkills()));
        sb.append("\n\n");

        // Experience + years merged into one summary line
        sb.append("EXPERIENCE  (" ).append(user.getExperienceYears()).append(" yrs)\n");
        sb.append(user.getExperienceDetails()).append("\n\n");

        // Education and certifications grouped together in a
        // single "credentials" block — a structural grouping
        // ClassicTemplate does not make
        sb.append("CREDENTIALS\n");
        sb.append("  Education: ").append(user.getEducation()).append("\n");
        sb.append("  Certifications: ").append(user.getCertifications()).append("\n\n");

        // Projects last, tag-prefixed per line instead of a
        // dedicated bordered section
        sb.append("PROJECTS\n");
        sb.append("  ").append(user.getProjects()).append("\n");

        return sb.toString();
    }
}