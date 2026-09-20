package com.resumegenerator.resume;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.resumegenerator.model.Resume;
import com.resumegenerator.model.User;

/**
 * ResumeTemplate — Strategy interface for visual PDF rendering and text exports.
 */
public interface ResumeTemplate {

    /**
     * Renders a visual PDF layout directly to the iText Document.
     *
     * @param resume Resume aggregate
     * @param document open iText Document instance
     * @param writer PdfWriter instance
     * @throws Exception on PDF generation error
     */
    void renderPDF(Resume resume, Document document, PdfWriter writer) throws Exception;

    /**
     * Default plain text rendering fallback.
     */
    default String render(Resume resume) {
        return com.resumegenerator.export.PDFHelper.renderPlainText(resume);
    }

    default String render(User user) {
        Resume r = new Resume();
        r.setUser(user);
        return render(r);
    }
}