package com.resumegenerator.resume;

/**
 * TemplateFactory — Strategy Factory mapping TemplateType enums to concrete ResumeTemplate instances.
 */
public class TemplateFactory {

    public static ResumeTemplate create(TemplateType type) {
        if (type == null) {
            return new ClassicTemplate();
        }
        switch (type) {
            case CLASSIC:
                return new ClassicTemplate();
            case MODERN:
                return new ModernTemplate();
            case MINIMAL:
                return new MinimalTemplate();
            case ATS:
                return new ATSTemplate();
            default:
                throw new IllegalArgumentException("Unsupported TemplateType: " + type);
        }
    }
}