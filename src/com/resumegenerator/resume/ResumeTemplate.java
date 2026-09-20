package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public interface ResumeTemplate {

    String render(com.resumegenerator.model.Resume resume);

    default String render(User user) {
        com.resumegenerator.model.Resume r = new com.resumegenerator.model.Resume();
        r.setUser(user);
        return render(r);
    }
}