package com.resumegenerator.resume;

import com.resumegenerator.model.User;

public abstract class Resume {
    protected User user;
    protected com.resumegenerator.model.Resume resumeAggregate;
    protected ResumeTemplate template;

    public Resume(User user) {
        this.user = user;
        this.resumeAggregate = new com.resumegenerator.model.Resume();
        this.resumeAggregate.setUser(user);
    }

    public Resume(com.resumegenerator.model.Resume resumeAggregate) {
        this.resumeAggregate = resumeAggregate;
        if (resumeAggregate != null) {
            this.user = resumeAggregate.getUser();
        }
    }

    public Resume(User user, ResumeTemplate template) {
        this.user = user;
        this.template = template;
        this.resumeAggregate = new com.resumegenerator.model.Resume();
        this.resumeAggregate.setUser(user);
    }

    public Resume(com.resumegenerator.model.Resume resumeAggregate, ResumeTemplate template) {
        this.resumeAggregate = resumeAggregate;
        if (resumeAggregate != null) {
            this.user = resumeAggregate.getUser();
        }
        this.template = template;
    }

    public ResumeTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ResumeTemplate template) {
        this.template = template;
    }

    public com.resumegenerator.model.Resume getResumeAggregate() {
        return resumeAggregate;
    }

    public String renderWithTemplate() {
        if (template == null) {
            throw new IllegalStateException("No ResumeTemplate set on this Resume.");
        }
        if (resumeAggregate == null) {
            resumeAggregate = new com.resumegenerator.model.Resume();
            resumeAggregate.setUser(user);
        }
        return template.render(resumeAggregate);
    }

    public abstract String getFormattedResume();
}
