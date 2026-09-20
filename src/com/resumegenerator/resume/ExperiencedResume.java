package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public class ExperiencedResume extends Resume {
    public ExperiencedResume(User user) { super(user); }
    public ExperiencedResume(com.resumegenerator.model.Resume resumeAggregate) { super(resumeAggregate); }

    @Override
    public String getFormattedResume() {
        User u = resumeAggregate != null && resumeAggregate.getUser() != null ? resumeAggregate.getUser() : user;
        String name = u != null && u.getName() != null ? u.getName() : "";
        String email = u != null && u.getEmail() != null ? u.getEmail() : "";
        String phone = u != null && u.getPhone() != null ? u.getPhone() : "";

        return "📄 Experienced Resume\n\n"
                + "👤 Name: " + name + "\n"
                + "📧 Email: " + email + "\n"
                + "Mobile number : " + phone;
    }
}
