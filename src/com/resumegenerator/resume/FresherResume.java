package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public class FresherResume extends Resume {
    public FresherResume(User user) { super(user); }
    public FresherResume(com.resumegenerator.model.Resume resumeAggregate) { super(resumeAggregate); }

    @Override
    public String getFormattedResume() {
        User u = resumeAggregate != null && resumeAggregate.getUser() != null ? resumeAggregate.getUser() : user;
        String name = u != null && u.getName() != null ? u.getName() : "";
        String phone = u != null && u.getPhone() != null ? u.getPhone() : "";
        String email = u != null && u.getEmail() != null ? u.getEmail() : "";
        String objective = resumeAggregate != null && resumeAggregate.getObjective() != null ? resumeAggregate.getObjective() : "";

        return "📄 Fresher Resume\n\n"
                + "👤 Name: " + name + "\n"
                + "Mobile number : " + phone + "\n"
                + "📧 Email: " + email + "\n"
                + "🎯 Objective: " + objective;
    }
}
