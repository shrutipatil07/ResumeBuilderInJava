package com.resumegenerator.export;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import com.resumegenerator.resume.Resume;

public class PDFGenerator {

    public static void createPDF(String content, String fileName) {
        if (content == null) content = "";

        // Defensive sanitization of non-WinAnsi font characters for iText
        content = content.replace("•", "-")
                         .replace("»", ">")
                         .replace("·", "|");

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();
            Font font = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            document.add(new Paragraph(content, font));
            document.close();
            System.out.println("✅ PDF Created: " + fileName);
        } catch (java.io.IOException e) {
            System.err.println("❌ PDF File Lock Error: " + e.getMessage());
            throw new RuntimeException("Could not save PDF file '" + fileName + "'.\nIf the file is currently open in another program, please close it and try again.", e);
        } catch (Exception e) {
            System.err.println("❌ PDF Generation Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PDF Generation Failed: " + e.getMessage(), e);
        }
    }

    public static void createPDF(Resume resume, String fileName) {
        String content = resume.renderWithTemplate();
        createPDF(content, fileName);
    }

    public static void createPDF(com.resumegenerator.model.Resume resumeAggregate, String fileName) {
        String content;
        if (resumeAggregate.getTemplateType() != null) {
            content = com.resumegenerator.resume.TemplateFactory.create(resumeAggregate.getTemplateType()).render(resumeAggregate);
        } else {
            content = new com.resumegenerator.resume.ClassicTemplate().render(resumeAggregate);
        }
        createPDF(content, fileName);
    }
}
