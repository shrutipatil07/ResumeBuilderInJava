package com.resumegenerator.resume;

// ---------------------------------------------------------------
// TemplateType — enumerates the currently supported resume
// template styles. Used to identify/select a template without
// relying on raw strings or direct references to concrete
// ResumeTemplate classes throughout the application.
// ---------------------------------------------------------------
public enum TemplateType {
    CLASSIC,
    MODERN,
    MINIMAL,
    ATS
}