package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public class ExperiencedResume extends Resume {
    public ExperiencedResume(User user) { super(user); }

    @Override
    public String getFormattedResume() {
        return "📄 Experienced Resume\n\n"
                + "👤 Name: " + user.getName() + "\n"
                + "📧 Email: " + user.getEmail() + "\n"
                + "Mobile number : " + user.getPhone() + "\n"
                + "💼 Experience: " + user.getExperienceDetails() + "\n"
                + "💡 Skills: " + String.join(", ", user.getSkills()) + "\n"
                + "📌 Projects: " + user.getProjects() + "\n"
                + "📜 Certifications: " + user.getCertifications();
    }
}
