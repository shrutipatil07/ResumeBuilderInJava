package com.resumegenerator.resume;

import com.resumegenerator.model.User;

// ---------------------------------------------------------------
// ResumeTemplate — contract for anything that can render a User's
// data into a formatted resume representation.
//
// Concrete implementations (ClassicTemplate, ModernTemplate,
// MinimalTemplate) will each define their OWN layout/formatting
// rules, but all of them are interchangeable through this contract.
// ---------------------------------------------------------------
public interface ResumeTemplate {

    /**
     * Renders the given user's data into a formatted resume string.
     *
     * @param user the data source — same User object used elsewhere
     *             in the app (Dashboard, ResumeBuilder, PDFGenerator)
     * @return the fully formatted resume content, ready to be
     *         displayed on screen or passed to PDFGenerator
     */
    String render(User user);
}