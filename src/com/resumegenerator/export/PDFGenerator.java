package com.resumegenerator.export;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.resumegenerator.dao.ResumeDAO;
import com.resumegenerator.model.Resume;
import com.resumegenerator.resume.ClassicTemplate;
import com.resumegenerator.resume.ResumeTemplate;
import com.resumegenerator.resume.TemplateFactory;

import java.io.FileOutputStream;

/**
 * PDFGenerator — Facade for generating visual PDF documents via Strategy templates
 * and logging export events to the database.
 */
public class PDFGenerator {

    private static final ResumeDAO resumeDAO = new ResumeDAO();

    /**
     * Generates a unique, filesystem-safe filename using resume title, ID, and timestamp.
     */
    public static String generateUniqueFileName(Resume resume) {
        String baseTitle = "Resume";
        if (resume != null && resume.getTitle() != null && !resume.getTitle().trim().isEmpty()) {
            baseTitle = resume.getTitle().trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        }
        int id = resume != null ? resume.getResumeId() : 0;
        long timestamp = System.currentTimeMillis();
        return baseTitle + "_" + (id > 0 ? id + "_" : "") + timestamp + ".pdf";
    }

    /**
     * Creates a PDF using visual Strategy pattern rendering.
     *
     * @param resume Resume aggregate
     * @param fileName Destination file path (if null/empty, generates unique filename)
     * @return absolute path of the generated PDF file
     */
    public static String createPDF(Resume resume, String fileName) {
        if (resume == null) {
            throw new IllegalArgumentException("Cannot generate PDF from null Resume.");
        }

        String targetPath = (fileName != null && !fileName.trim().isEmpty())
                ? fileName.trim()
                : generateUniqueFileName(resume);

        if (!targetPath.toLowerCase().endsWith(".pdf")) {
            targetPath += ".pdf";
        }

        // Initialize iText Document with standard 0.5 inch (36pt) margins
        Document document = new Document(com.itextpdf.text.PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetPath));
            document.open();

            // Select strategy template
            ResumeTemplate template;
            if (resume.getTemplateType() != null) {
                template = TemplateFactory.create(resume.getTemplateType());
            } else {
                template = new ClassicTemplate();
            }

            // Execute visual PDF rendering
            template.renderPDF(resume, document, writer);

            document.close();
            System.out.println("✅ Visual PDF Created: " + targetPath);

            // Record in generated_resumes table if saved resume
            if (resume.getResumeId() > 0) {
                try {
                    resumeDAO.recordGeneratedResume(resume.getResumeId(), targetPath, "PDF");
                    System.out.println("   PDF generation logged in database for resume_id = " + resume.getResumeId());
                } catch (Exception dbEx) {
                    System.err.println("⚠️ Could not log PDF generation in DB: " + dbEx.getMessage());
                }
            }

            return targetPath;

        } catch (java.io.IOException e) {
            System.err.println("❌ PDF File Lock Error: " + e.getMessage());
            throw new RuntimeException("Could not save PDF file '" + targetPath + "'.\nIf the file is currently open in another program, please close it and try again.", e);
        } catch (Exception e) {
            System.err.println("❌ PDF Generation Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PDF Generation Failed: " + e.getMessage(), e);
        }
    }

    /**
     * Backward-compatibility fallback for simple string rendering.
     */
    public static void createPDF(String content, String fileName) {
        if (content == null) content = "";
        String safeContent = PDFHelper.sanitizeText(content);

        Document document = new Document(com.itextpdf.text.PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();
            com.itextpdf.text.Font font = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 11, com.itextpdf.text.BaseColor.BLACK);
            document.add(new com.itextpdf.text.Paragraph(safeContent, font));
            document.close();
            System.out.println("✅ Plain PDF Created: " + fileName);
        } catch (Exception e) {
            throw new RuntimeException("PDF Generation Failed: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy bridge for old resume package model.
     */
    public static void createPDF(com.resumegenerator.resume.Resume legacyResume, String fileName) {
        if (legacyResume != null) {
            createPDF(legacyResume.renderWithTemplate(), fileName);
        }
    }
}
