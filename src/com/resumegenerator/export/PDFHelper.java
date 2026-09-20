package com.resumegenerator.export;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.resumegenerator.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDFHelper — Shared utility methods and design tokens for iText visual PDF layout rendering.
 *
 * Responsibilities:
 * - Color palette definitions (Navy, Slate, Charcoal, Muted Gray, Accent Blue)
 * - Typography definitions (Helvetica variations in WinAnsi encoding)
 * - Non-WinAnsi text sanitization to prevent iText font encoding crashes
 * - Visual section header construction (with colored borders, dividers)
 * - Clickable URL and email link creation
 * - Date range formatting
 * - Plain text fallback rendering
 */
public class PDFHelper {

    // Color Palette Tokens
    public static final BaseColor COLOR_PRIMARY_NAVY = new BaseColor(26, 54, 93);    // #1A365D
    public static final BaseColor COLOR_SLATE_DARK    = new BaseColor(45, 55, 72);    // #2D3748
    public static final BaseColor COLOR_SLATE_MUTED   = new BaseColor(113, 128, 150); // #718096
    public static final BaseColor COLOR_LIGHT_BG      = new BaseColor(247, 250, 252); // #F7FAFC
    public static final BaseColor COLOR_ACCENT_BLUE   = new BaseColor(43, 108, 176);  // #2B6CB0
    public static final BaseColor COLOR_BLACK         = BaseColor.BLACK;

    // Font Families (WinAnsi Standard Fonts)
    public static final Font FONT_HEADER_NAME_LARGE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_PRIMARY_NAVY);
    public static final Font FONT_HEADER_NAME_MODERN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.WHITE);
    public static final Font FONT_HEADER_NAME_MINIMAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_SLATE_DARK);
    public static final Font FONT_HEADER_NAME_ATS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_BLACK);

    public static final Font FONT_CONTACT = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_SLATE_DARK);
    public static final Font FONT_CONTACT_MODERN = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(226, 232, 240));

    public static final Font FONT_SECTION_CLASSIC = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY_NAVY);
    public static final Font FONT_SECTION_MODERN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_ACCENT_BLUE);
    public static final Font FONT_SECTION_MINIMAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_SLATE_DARK);
    public static final Font FONT_SECTION_ATS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_BLACK);

    public static final Font FONT_TITLE_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_SLATE_DARK);
    public static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, COLOR_SLATE_MUTED);
    public static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_SLATE_DARK);
    public static final Font FONT_BODY_ATS = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_BLACK);
    public static final Font FONT_LINK = FontFactory.getFont(FontFactory.HELVETICA, 9f, Font.UNDERLINE, COLOR_ACCENT_BLUE);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    /**
     * Defensive text sanitization replacing unsupported Unicode symbols with WinAnsi-safe equivalents.
     */
    public static String sanitizeText(String input) {
        if (input == null) return "";
        return input.replace("•", "-")
                    .replace("»", ">")
                    .replace("·", "|")
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace("“", "\"")
                    .replace("”", "\"")
                    .replace("‘", "'")
                    .replace("’", "'");
    }

    /**
     * Formats LocalDate to "MMM yyyy" string (e.g., "Jun 2022"). Returns "Present" if null.
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "Present";
        return date.format(DATE_FORMATTER);
    }

    /**
     * Formats start and end dates as a range (e.g. "Jun 2022 - Present").
     */
    public static String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) return "";
        String startStr = formatDate(startDate);
        String endStr = (endDate != null) ? formatDate(endDate) : "Present";
        return startStr + " - " + endStr;
    }

    /**
     * Adds a section heading with optional divider line.
     */
    public static void addSectionHeading(Document doc, String title, Font font, BaseColor lineColor) throws DocumentException {
        Font headerFont = (font != null) ? font : FONT_SECTION_CLASSIC;
        if (title != null && !title.trim().isEmpty()) {
            doc.add(Chunk.NEWLINE);
            Paragraph p = new Paragraph(sanitizeText(title.toUpperCase()), headerFont);
            p.setSpacingAfter(4f);
            doc.add(p);
        }

        if (lineColor != null) {
            LineSeparator line = new LineSeparator(1f, 100f, lineColor, Element.ALIGN_CENTER, -2f);
            doc.add(line);
            doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3))); // Spacer
        }
    }

    /**
     * Adds a bullet point item to doc.
     */
    public static void addBulletPoint(Document doc, String text, Font font) throws DocumentException {
        if (text == null || text.trim().isEmpty()) return;
        Paragraph p = new Paragraph("- " + sanitizeText(text.trim()), font);
        p.setIndentationLeft(12f);
        p.setSpacingAfter(3f);
        doc.add(p);
    }

    /**
     * Creates a clickable URL anchor chunk.
     */
    public static Anchor createLinkChunk(String text, String url, Font linkFont) {
        String cleanUrl = url != null ? url.trim() : "";
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://") && !cleanUrl.startsWith("mailto:")) {
            if (cleanUrl.contains("@")) cleanUrl = "mailto:" + cleanUrl;
            else cleanUrl = "https://" + cleanUrl;
        }
        Anchor anchor = new Anchor(sanitizeText(text), linkFont);
        anchor.setReference(cleanUrl);
        return anchor;
    }

    /**
     * Generates a modern skill badge tag inside a table cell.
     */
    public static PdfPCell createSkillBadgeCell(String skillName, String levelStr) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_LIGHT_BG);
        cell.setBorderColor(COLOR_SLATE_MUTED);
        cell.setBorderWidth(0.5f);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        String display = sanitizeText(skillName) + (levelStr != null ? " (" + levelStr + ")" : "");
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY_NAVY);
        cell.addElement(new Paragraph(display, f));
        return cell;
    }

    /**
     * Generates plain text representation of Resume aggregate for legacy tests and string output.
     */
    public static String renderPlainText(Resume resume) {
        if (resume == null) return "";
        StringBuilder sb = new StringBuilder();
        User u = resume.getUser();
        if (u != null && u.getName() != null) sb.append(u.getName()).append("\n");
        if (u != null && u.getEmail() != null) sb.append(u.getEmail()).append(" | ");
        if (u != null && u.getPhone() != null) sb.append(u.getPhone());
        sb.append("\n\n");

        if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
            sb.append("OBJECTIVE\n").append(resume.getObjective()).append("\n\n");
        }

        if (resume.getEducationList() != null && !resume.getEducationList().isEmpty()) {
            sb.append("EDUCATION\n");
            for (Education edu : resume.getEducationList()) {
                sb.append("- ").append(edu.getDegree());
                if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().trim().isEmpty()) {
                    sb.append(" in ").append(edu.getFieldOfStudy().trim());
                }
                sb.append(" - ").append(edu.getInstitution());
                sb.append(" (").append(edu.getStartYear()).append("-").append(edu.getEndYear() != null ? edu.getEndYear() : "Present").append(")\n");
            }
            sb.append("\n");
        }

        if (resume.getSkillList() != null && !resume.getSkillList().isEmpty()) {
            sb.append("SKILLS\n");
            for (int i = 0; i < resume.getSkillList().size(); i++) {
                Skill s = resume.getSkillList().get(i);
                sb.append(s.getSkillName());
                if (s.getProficiencyLevel() != null) {
                    String lvl = s.getProficiencyLevel().name();
                    String formattedLvl = lvl.substring(0, 1).toUpperCase() + lvl.substring(1).toLowerCase();
                    sb.append(" - ").append(formattedLvl);
                }
                if (i < resume.getSkillList().size() - 1) sb.append(", ");
            }
            sb.append("\n\n");
        }

        if (resume.getExperienceList() != null && !resume.getExperienceList().isEmpty()) {
            sb.append("EXPERIENCE\n");
            for (Experience exp : resume.getExperienceList()) {
                sb.append("- ").append(exp.getJobTitle()).append(" at ").append(exp.getCompanyName()).append("\n");
                if (exp.getDescription() != null) sb.append("  ").append(exp.getDescription()).append("\n");
            }
            sb.append("\n");
        }

        if (resume.getProjectList() != null && !resume.getProjectList().isEmpty()) {
            sb.append("PROJECTS\n");
            for (Project p : resume.getProjectList()) {
                sb.append("- ").append(p.getProjectName()).append(": ").append(p.getDescription() != null ? p.getDescription() : "").append("\n");
            }
            sb.append("\n");
        }

        if (resume.getCertificationList() != null && !resume.getCertificationList().isEmpty()) {
            sb.append("CERTIFICATIONS\n");
            for (Certification c : resume.getCertificationList()) {
                sb.append("- ").append(c.getCertificationName()).append(" by ").append(c.getIssuingOrg()).append("\n");
            }
            sb.append("\n");
        }

        return sanitizeText(sb.toString());
    }
}
