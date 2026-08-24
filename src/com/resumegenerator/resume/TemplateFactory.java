package com.resumegenerator.resume;

// ---------------------------------------------------------------
// TemplateFactory — the single place responsible for turning a
// TemplateType into a concrete ResumeTemplate instance.
//
// TemplateType  →  TemplateFactory  →  ResumeTemplate
//
// This is the only class in the application that is allowed to
// know about ClassicTemplate / ModernTemplate / MinimalTemplate
// by name. Everything else (UI, Resume, PDFGenerator) only ever
// deals in TemplateType and ResumeTemplate.
// ---------------------------------------------------------------
public class TemplateFactory {

    public static ResumeTemplate create(TemplateType type) {
        switch (type) {
            case CLASSIC:
                return new ClassicTemplate();
            case MODERN:
                return new ModernTemplate();
            case MINIMAL:
                return new MinimalTemplate();
            default:
                // Unreachable as long as TemplateType has no
                // unhandled values, but required so the method
                // compiles with a guaranteed return on every path.
                throw new IllegalArgumentException("Unsupported TemplateType: " + type);
        }
    }
}