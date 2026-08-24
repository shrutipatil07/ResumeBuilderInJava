package com.resumegenerator.export;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import com.resumegenerator.resume.Resume;

public class PDFGenerator {

    // Original overload — preserved unchanged so any existing caller
    // that passes a raw string still works exactly as before.
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

    // NEW overload — the template-aware path. PDFGenerator asks the
    // Resume for its rendered content (which the Resume gets from
    // its attached ResumeTemplate) instead of deciding formatting itself.
    public static void createPDF(Resume resume, String fileName) {
        String content = resume.renderWithTemplate();
        createPDF(content, fileName); // reuse the same PDF-writing logic
    }
}
