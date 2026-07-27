package com.resumegenerator.export;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;

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
}
