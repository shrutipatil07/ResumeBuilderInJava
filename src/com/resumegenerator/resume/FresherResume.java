package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public class FresherResume extends Resume {
    public FresherResume(User user) { super(user); }

    @Override
    public String getFormattedResume() {
        return "📄 Fresher Resume\n\n"
                + "👤 Name: " + user.getName() + "\n"
                + "Mobile number : " + user.getPhone() + "\n"
                + "📧 Email: " + user.getEmail() + "\n"
                + "🎓 Education: " + user.getEducation() + "\n"
                + "💡 Skills: " + String.join(", ", user.getSkills()) + "\n"
                + "🎯 Objective: " + user.getObjective();
    }
}
