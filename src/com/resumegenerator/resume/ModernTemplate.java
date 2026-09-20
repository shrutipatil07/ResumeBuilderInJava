package com.resumegenerator.resume;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.resumegenerator.export.PDFHelper;
import com.resumegenerator.model.Resume;
import com.resumegenerator.model.*;

import java.util.List;

/**
 * ModernTemplate — Contemporary visual PDF layout with colored accent header banner,
 * modern section indicators, and compact skill chips.
 */
public class ModernTemplate implements ResumeTemplate {

    @Override
    public void renderPDF(Resume resume, Document document, PdfWriter writer) throws Exception {
        User user = resume.getUser();

        // 1. Header Banner Table
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(PDFHelper.COLOR_PRIMARY_NAVY);
        headerCell.setPadding(12f);
        headerCell.setBorder(Rectangle.NO_BORDER);

        String name = user != null && user.getName() != null ? user.getName() : "Professional Resume";
        Paragraph nameP = new Paragraph(PDFHelper.sanitizeText(name.toUpperCase()), PDFHelper.FONT_HEADER_NAME_MODERN);
        nameP.setSpacingAfter(4f);
        headerCell.addElement(nameP);

        StringBuilder contactStr = new StringBuilder();
        if (user != null) {
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                contactStr.append("Email: ").append(user.getEmail().trim());
            }
            if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                if (contactStr.length() > 0) contactStr.append("   |   ");
                contactStr.append("Phone: ").append(user.getPhone().trim());
            }
        }
        if (contactStr.length() > 0) {
            Paragraph contactP = new Paragraph(PDFHelper.sanitizeText(contactStr.toString()), PDFHelper.FONT_CONTACT_MODERN);
            headerCell.addElement(contactP);
        }

        headerTable.addCell(headerCell);
        document.add(headerTable);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6))); // Spacer

        // 2. Objective
        if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
            PDFHelper.addSectionHeading(document, "Executive Summary", PDFHelper.FONT_SECTION_MODERN, PDFHelper.COLOR_ACCENT_BLUE);
            Paragraph objP = new Paragraph(PDFHelper.sanitizeText(resume.getObjective().trim()), PDFHelper.FONT_BODY);
            objP.setSpacingAfter(6f);
            document.add(objP);
        }

        // 3. Technical Skills (Badge Chip Table)
        List<Skill> skillList = resume.getSkillList();
        if (skillList != null && !skillList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "Skills & Proficiencies", PDFHelper.FONT_SECTION_MODERN, PDFHelper.COLOR_ACCENT_BLUE);
            PdfPTable skillsTable = new PdfPTable(3);
            skillsTable.setWidthPercentage(100);
            skillsTable.setSpacingAfter(6f);

            for (Skill s : skillList) {
                String lvl = s.getProficiencyLevel() != null ? s.getProficiencyLevel().name() : null;
                PdfPCell badge = PDFHelper.createSkillBadgeCell(s.getSkillName(), lvl);
                skillsTable.addCell(badge);
            }
            // Fill remaining cells in last row if not multiple of 3
            int rem = skillList.size() % 3;
            if (rem > 0) {
                for (int i = 0; i < (3 - rem); i++) {
                    PdfPCell emptyCell = new PdfPCell();
                    emptyCell.setBorder(Rectangle.NO_BORDER);
                    skillsTable.addCell(emptyCell);
                }
            }
            document.add(skillsTable);
        }

        // 4. Work Experience
        List<Experience> expList = resume.getExperienceList();
        if (expList != null && !expList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "Professional Experience", PDFHelper.FONT_SECTION_MODERN, PDFHelper.COLOR_ACCENT_BLUE);
            for (Experience exp : expList) {
                String header = exp.getJobTitle() + " - " + exp.getCompanyName();
                if (exp.getLocation() != null && !exp.getLocation().trim().isEmpty()) {
                    header += " (" + exp.getLocation().trim() + ")";
                }
                Paragraph expP = new Paragraph(PDFHelper.sanitizeText(header), PDFHelper.FONT_TITLE_BOLD);
                document.add(expP);

                String dateStr = PDFHelper.formatDateRange(exp.getStartDate(), exp.getEndDate());
                Paragraph dateP = new Paragraph(PDFHelper.sanitizeText(dateStr), PDFHelper.FONT_SUBTITLE);
                dateP.setSpacingAfter(2f);
                document.add(dateP);

                if (exp.getDescription() != null && !exp.getDescription().trim().isEmpty()) {
                    PDFHelper.addBulletPoint(document, exp.getDescription().trim(), PDFHelper.FONT_BODY);
                }
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
            }
        }

        // 5. Projects
        List<Project> projList = resume.getProjectList();
        if (projList != null && !projList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "Key Projects", PDFHelper.FONT_SECTION_MODERN, PDFHelper.COLOR_ACCENT_BLUE);
            for (Project proj : projList) {
                Paragraph pHeader = new Paragraph(PDFHelper.sanitizeText(proj.getProjectName()), PDFHelper.FONT_TITLE_BOLD);
                if (proj.getProjectUrl() != null && !proj.getProjectUrl().trim().isEmpty()) {
                    pHeader.add(new Chunk("  "));
                    pHeader.add(PDFHelper.createLinkChunk("[URL]", proj.getProjectUrl().trim(), PDFHelper.FONT_LINK));
                }
                document.add(pHeader);

                if (proj.getTechStack() != null && !proj.getTechStack().trim().isEmpty()) {
                    Paragraph techP = new Paragraph(PDFHelper.sanitizeText("Tech Stack: " + proj.getTechStack().trim()), PDFHelper.FONT_SUBTITLE);
                    techP.setSpacingAfter(2f);
                    document.add(techP);
                }

                if (proj.getDescription() != null && !proj.getDescription().trim().isEmpty()) {
                    PDFHelper.addBulletPoint(document, proj.getDescription().trim(), PDFHelper.FONT_BODY);
                }
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
            }
        }

        // 6. Education
        List<Education> eduList = resume.getEducationList();
        if (eduList != null && !eduList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "Education", PDFHelper.FONT_SECTION_MODERN, PDFHelper.COLOR_ACCENT_BLUE);
            for (Education edu : eduList) {
                StringBuilder title = new StringBuilder();
                title.append(edu.getDegree());
                if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().trim().isEmpty()) {
                    title.append(" in ").append(edu.getFieldOfStudy().trim());
                }
                title.append(" - ").append(edu.getInstitution());

                String dateStr = edu.getStartYear() + " - " + (edu.getEndYear() != null ? edu.getEndYear() : "Present");
                Paragraph eduP = new Paragraph(PDFHelper.sanitizeText(title.toString()), PDFHelper.FONT_TITLE_BOLD);
                document.add(eduP);

                StringBuilder sub = new StringBuilder();
                sub.append("Dates: ").append(dateStr);
                if (edu.getGrade() != null && !edu.getGrade().trim().isEmpty()) {
                    sub.append("  |  Grade: ").append(edu.getGrade().trim());
                }
                Paragraph subP = new Paragraph(PDFHelper.sanitizeText(sub.toString()), PDFHelper.FONT_SUBTITLE);
                subP.setSpacingAfter(4f);
                document.add(subP);
            }
        }

        // 7. Certifications
        List<Certification> certList = resume.getCertificationList();
        if (certList != null && !certList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "Certifications & Training", PDFHelper.FONT_SECTION_MODERN, PDFHelper.COLOR_ACCENT_BLUE);
            for (Certification cert : certList) {
                String certStr = cert.getCertificationName() + " - " + cert.getIssuingOrg();
                if (cert.getIssueDate() != null) {
                    certStr += " (" + PDFHelper.formatDate(cert.getIssueDate()) + ")";
                }
                Paragraph certP = new Paragraph(PDFHelper.sanitizeText(certStr), PDFHelper.FONT_BODY);
                if (cert.getCredentialUrl() != null && !cert.getCredentialUrl().trim().isEmpty()) {
                    certP.add(new Chunk("  "));
                    certP.add(PDFHelper.createLinkChunk("[Verify Credential]", cert.getCredentialUrl().trim(), PDFHelper.FONT_LINK));
                }
                certP.setSpacingAfter(3f);
                document.add(certP);
            }
        }
    }
}