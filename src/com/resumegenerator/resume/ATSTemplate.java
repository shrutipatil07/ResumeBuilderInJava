package com.resumegenerator.resume;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.resumegenerator.export.PDFHelper;
import com.resumegenerator.model.Resume;
import com.resumegenerator.model.*;

import java.util.List;

/**
 * ATSTemplate — 100% Applicant Tracking System (ATS) compliant visual PDF template.
 *
 * Design constraints for ATS compliance:
 * - 100% single-column layout
 * - Standard black Helvetica typography
 * - Zero graphics, tables, background colors, or non-standard characters
 * - Clear, standardized section headings (ALL-CAPS)
 * - Plain dash (-) bullet points
 */
public class ATSTemplate implements ResumeTemplate {

    @Override
    public void renderPDF(Resume resume, Document document, PdfWriter writer) throws Exception {
        User user = resume.getUser();

        // 1. Header (Plain text, left-aligned)
        String name = user != null && user.getName() != null ? user.getName() : "Professional Resume";
        Paragraph nameP = new Paragraph(PDFHelper.sanitizeText(name), PDFHelper.FONT_HEADER_NAME_ATS);
        nameP.setSpacingAfter(4f);
        document.add(nameP);

        StringBuilder contactStr = new StringBuilder();
        if (user != null) {
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                contactStr.append("Email: ").append(user.getEmail().trim());
            }
            if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                if (contactStr.length() > 0) contactStr.append(" | ");
                contactStr.append("Phone: ").append(user.getPhone().trim());
            }
        }
        if (contactStr.length() > 0) {
            Paragraph contactP = new Paragraph(PDFHelper.sanitizeText(contactStr.toString()), PDFHelper.FONT_BODY_ATS);
            contactP.setSpacingAfter(10f);
            document.add(contactP);
        }

        // 2. Summary / Objective
        if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
            addATSHeading(document, "SUMMARY");
            Paragraph objP = new Paragraph(PDFHelper.sanitizeText(resume.getObjective().trim()), PDFHelper.FONT_BODY_ATS);
            objP.setSpacingAfter(8f);
            document.add(objP);
        }

        // 3. Technical Skills
        List<Skill> skillList = resume.getSkillList();
        if (skillList != null && !skillList.isEmpty()) {
            addATSHeading(document, "TECHNICAL SKILLS");
            StringBuilder skillsStr = new StringBuilder();
            for (int i = 0; i < skillList.size(); i++) {
                Skill s = skillList.get(i);
                skillsStr.append(s.getSkillName());
                if (s.getProficiencyLevel() != null) {
                    skillsStr.append(" (").append(s.getProficiencyLevel().name()).append(")");
                }
                if (i < skillList.size() - 1) skillsStr.append(", ");
            }
            Paragraph skP = new Paragraph(PDFHelper.sanitizeText(skillsStr.toString()), PDFHelper.FONT_BODY_ATS);
            skP.setSpacingAfter(8f);
            document.add(skP);
        }

        // 4. Work Experience
        List<Experience> expList = resume.getExperienceList();
        if (expList != null && !expList.isEmpty()) {
            addATSHeading(document, "WORK EXPERIENCE");
            for (Experience exp : expList) {
                String header = exp.getJobTitle() + " | " + exp.getCompanyName();
                if (exp.getLocation() != null && !exp.getLocation().trim().isEmpty()) {
                    header += " | " + exp.getLocation().trim();
                }
                Paragraph expP = new Paragraph(PDFHelper.sanitizeText(header), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK));
                document.add(expP);

                String dateStr = PDFHelper.formatDateRange(exp.getStartDate(), exp.getEndDate());
                Paragraph dateP = new Paragraph(PDFHelper.sanitizeText("Dates: " + dateStr), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.BLACK));
                dateP.setSpacingAfter(3f);
                document.add(dateP);

                if (exp.getDescription() != null && !exp.getDescription().trim().isEmpty()) {
                    PDFHelper.addBulletPoint(document, exp.getDescription().trim(), PDFHelper.FONT_BODY_ATS);
                }
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
            }
        }

        // 5. Projects
        List<Project> projList = resume.getProjectList();
        if (projList != null && !projList.isEmpty()) {
            addATSHeading(document, "PROJECTS");
            for (Project proj : projList) {
                String pTitle = proj.getProjectName();
                if (proj.getProjectUrl() != null && !proj.getProjectUrl().trim().isEmpty()) {
                    pTitle += " (" + proj.getProjectUrl().trim() + ")";
                }
                Paragraph pHeader = new Paragraph(PDFHelper.sanitizeText(pTitle), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK));
                document.add(pHeader);

                if (proj.getTechStack() != null && !proj.getTechStack().trim().isEmpty()) {
                    Paragraph techP = new Paragraph(PDFHelper.sanitizeText("Technologies: " + proj.getTechStack().trim()), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.BLACK));
                    techP.setSpacingAfter(2f);
                    document.add(techP);
                }

                if (proj.getDescription() != null && !proj.getDescription().trim().isEmpty()) {
                    PDFHelper.addBulletPoint(document, proj.getDescription().trim(), PDFHelper.FONT_BODY_ATS);
                }
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
            }
        }

        // 6. Education
        List<Education> eduList = resume.getEducationList();
        if (eduList != null && !eduList.isEmpty()) {
            addATSHeading(document, "EDUCATION");
            for (Education edu : eduList) {
                StringBuilder title = new StringBuilder();
                title.append(edu.getDegree());
                if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().trim().isEmpty()) {
                    title.append(" in ").append(edu.getFieldOfStudy().trim());
                }
                title.append(" | ").append(edu.getInstitution());

                String dateStr = edu.getStartYear() + " - " + (edu.getEndYear() != null ? edu.getEndYear() : "Present");
                Paragraph eduP = new Paragraph(PDFHelper.sanitizeText(title.toString()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK));
                document.add(eduP);

                StringBuilder sub = new StringBuilder();
                sub.append("Graduation Year: ").append(dateStr);
                if (edu.getGrade() != null && !edu.getGrade().trim().isEmpty()) {
                    sub.append(" | Grade: ").append(edu.getGrade().trim());
                }
                Paragraph subP = new Paragraph(PDFHelper.sanitizeText(sub.toString()), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.BLACK));
                subP.setSpacingAfter(4f);
                document.add(subP);
            }
        }

        // 7. Certifications
        List<Certification> certList = resume.getCertificationList();
        if (certList != null && !certList.isEmpty()) {
            addATSHeading(document, "CERTIFICATIONS");
            for (Certification cert : certList) {
                String certStr = cert.getCertificationName() + " - " + cert.getIssuingOrg();
                if (cert.getIssueDate() != null) {
                    certStr += " (" + PDFHelper.formatDate(cert.getIssueDate()) + ")";
                }
                if (cert.getCredentialUrl() != null && !cert.getCredentialUrl().trim().isEmpty()) {
                    certStr += " | URL: " + cert.getCredentialUrl().trim();
                }
                Paragraph certP = new Paragraph(PDFHelper.sanitizeText(certStr), PDFHelper.FONT_BODY_ATS);
                certP.setSpacingAfter(3f);
                document.add(certP);
            }
        }
    }

    private void addATSHeading(Document doc, String title) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Paragraph p = new Paragraph(PDFHelper.sanitizeText(title.toUpperCase()), PDFHelper.FONT_SECTION_ATS);
        p.setSpacingAfter(4f);
        doc.add(p);
    }
}
