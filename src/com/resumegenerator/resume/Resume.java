package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public abstract class Resume {
    protected User user;
    protected ResumeTemplate template; // NEW — composed dependency, not inherited behavior

    // Original constructor — preserved exactly as-is so FresherResume
    // and ExperiencedResume compile and behave unchanged.
    public Resume(User user) {
        this.user = user;
    }

    // NEW — constructor injection, for when a template is available upfront
    public Resume(User user, ResumeTemplate template) {
        this.user = user;
        this.template = template;
    }

    // NEW — allows attaching/swapping a template after construction
    public ResumeTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ResumeTemplate template) {
        this.template = template;
    }

    public abstract String getFormattedResume();
}
