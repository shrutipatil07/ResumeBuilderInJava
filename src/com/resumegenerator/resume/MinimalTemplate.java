package com.resumegenerator.resume;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.resumegenerator.export.PDFHelper;
import com.resumegenerator.model.Resume;
import com.resumegenerator.model.*;

import java.util.List;

/**
 * MinimalTemplate — Clean, monochrome, minimalist PDF layout focusing on typography and whitespace.
 */
public class MinimalTemplate implements ResumeTemplate {

    @Override
    public void renderPDF(Resume resume, Document document, PdfWriter writer) throws Exception {
        User user = resume.getUser();

        // 1. Header (Left-aligned, clean typography)
        String name = user != null && user.getName() != null ? user.getName() : "Professional Resume";
        Paragraph nameP = new Paragraph(PDFHelper.sanitizeText(name), PDFHelper.FONT_HEADER_NAME_MINIMAL);
        nameP.setSpacingAfter(2f);
        document.add(nameP);

        StringBuilder contactStr = new StringBuilder();
        if (user != null) {
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                contactStr.append(user.getEmail().trim());
            }
            if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                if (contactStr.length() > 0) contactStr.append("  |  ");
                contactStr.append(user.getPhone().trim());
            }
        }
        if (contactStr.length() > 0) {
            Paragraph contactP = new Paragraph(PDFHelper.sanitizeText(contactStr.toString()), PDFHelper.FONT_CONTACT);
            contactP.setSpacingAfter(12f);
            document.add(contactP);
        }

        // 2. Objective
        if (resume.getObjective() != null && !resume.getObjective().trim().isEmpty()) {
            PDFHelper.addSectionHeading(document, "OBJECTIVE", PDFHelper.FONT_SECTION_MINIMAL, null);
            Paragraph objP = new Paragraph(PDFHelper.sanitizeText(resume.getObjective().trim()), PDFHelper.FONT_BODY);
            objP.setSpacingAfter(6f);
            document.add(objP);
        }

        // 3. Education
        List<Education> eduList = resume.getEducationList();
        if (eduList != null && !eduList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "EDUCATION", PDFHelper.FONT_SECTION_MINIMAL, null);
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
                sub.append(dateStr);
                if (edu.getGrade() != null && !edu.getGrade().trim().isEmpty()) {
                    sub.append("  |  ").append(edu.getGrade().trim());
                }
                Paragraph subP = new Paragraph(PDFHelper.sanitizeText(sub.toString()), PDFHelper.FONT_SUBTITLE);
                subP.setSpacingAfter(4f);
                document.add(subP);
            }
        }

        // 4. Skills
        List<Skill> skillList = resume.getSkillList();
        if (skillList != null && !skillList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "SKILLS", PDFHelper.FONT_SECTION_MINIMAL, null);
            StringBuilder skillsStr = new StringBuilder();
            for (int i = 0; i < skillList.size(); i++) {
                Skill s = skillList.get(i);
                skillsStr.append(s.getSkillName());
                if (s.getProficiencyLevel() != null) {
                    skillsStr.append(" (").append(s.getProficiencyLevel().name()).append(")");
                }
                if (i < skillList.size() - 1) skillsStr.append(", ");
            }
            Paragraph skP = new Paragraph(PDFHelper.sanitizeText(skillsStr.toString()), PDFHelper.FONT_BODY);
            skP.setSpacingAfter(6f);
            document.add(skP);
        }

        // 5. Work Experience
        List<Experience> expList = resume.getExperienceList();
        if (expList != null && !expList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "EXPERIENCE", PDFHelper.FONT_SECTION_MINIMAL, null);
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

        // 6. Projects
        List<Project> projList = resume.getProjectList();
        if (projList != null && !projList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "PROJECTS", PDFHelper.FONT_SECTION_MINIMAL, null);
            for (Project proj : projList) {
                Paragraph pHeader = new Paragraph(PDFHelper.sanitizeText(proj.getProjectName()), PDFHelper.FONT_TITLE_BOLD);
                if (proj.getProjectUrl() != null && !proj.getProjectUrl().trim().isEmpty()) {
                    pHeader.add(new Chunk("  "));
                    pHeader.add(PDFHelper.createLinkChunk("[URL]", proj.getProjectUrl().trim(), PDFHelper.FONT_LINK));
                }
                document.add(pHeader);

                if (proj.getTechStack() != null && !proj.getTechStack().trim().isEmpty()) {
                    Paragraph techP = new Paragraph(PDFHelper.sanitizeText("Tech: " + proj.getTechStack().trim()), PDFHelper.FONT_SUBTITLE);
                    techP.setSpacingAfter(2f);
                    document.add(techP);
                }

                if (proj.getDescription() != null && !proj.getDescription().trim().isEmpty()) {
                    PDFHelper.addBulletPoint(document, proj.getDescription().trim(), PDFHelper.FONT_BODY);
                }
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
            }
        }

        // 7. Certifications
        List<Certification> certList = resume.getCertificationList();
        if (certList != null && !certList.isEmpty()) {
            PDFHelper.addSectionHeading(document, "CERTIFICATIONS", PDFHelper.FONT_SECTION_MINIMAL, null);
            for (Certification cert : certList) {
                String certStr = cert.getCertificationName() + " - " + cert.getIssuingOrg();
                if (cert.getIssueDate() != null) {
                    certStr += " (" + PDFHelper.formatDate(cert.getIssueDate()) + ")";
                }
                Paragraph certP = new Paragraph(PDFHelper.sanitizeText(certStr), PDFHelper.FONT_BODY);
                if (cert.getCredentialUrl() != null && !cert.getCredentialUrl().trim().isEmpty()) {
                    certP.add(new Chunk("  "));
                    certP.add(PDFHelper.createLinkChunk("[Credential]", cert.getCredentialUrl().trim(), PDFHelper.FONT_LINK));
                }
                certP.setSpacingAfter(3f);
                document.add(certP);
            }
        }
    }
}