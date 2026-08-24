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

    // NEW — delegates rendering to whichever ResumeTemplate is attached.
    // Resume still does no formatting itself; it just brokers the call.
    public String renderWithTemplate() {
        if (template == null) {
            throw new IllegalStateException("No ResumeTemplate set on this Resume.");
        }
        return template.render(user);
    }

    public abstract String getFormattedResume();
}
