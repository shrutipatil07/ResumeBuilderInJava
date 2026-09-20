package com.resumegenerator.export;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import com.resumegenerator.resume.Resume;

public class PDFGenerator {

    public static void createPDF(String content, String fileName) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();
            document.add(new Paragraph(content));
            document.close();
            System.out.println("✅ PDF Created: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
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
