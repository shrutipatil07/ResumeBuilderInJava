package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public abstract class Resume {
    protected User user;
    public Resume(User user) { this.user = user; }
    public abstract String getFormattedResume();
}
