package com.resumegenerator.model;

import java.time.LocalDate;

public class Certification {
    private int certificationId;
    private int resumeId;
    private String certificationName;
    private String issuingOrg;
    private LocalDate issueDate;
    private String credentialUrl;
    private int displayOrder;

    public Certification() {
    }

    public Certification(String certificationName, String issuingOrg, LocalDate issueDate, String credentialUrl, int displayOrder) {
        this.certificationName = certificationName;
        this.issuingOrg = issuingOrg;
        this.issueDate = issueDate;
        this.credentialUrl = credentialUrl;
        this.displayOrder = displayOrder;
    }

    public Certification(int certificationId, int resumeId, String certificationName, String issuingOrg, LocalDate issueDate, String credentialUrl, int displayOrder) {
        this.certificationId = certificationId;
        this.resumeId = resumeId;
        this.certificationName = certificationName;
        this.issuingOrg = issuingOrg;
        this.issueDate = issueDate;
        this.credentialUrl = credentialUrl;
        this.displayOrder = displayOrder;
    }

    public int getCertificationId() { return certificationId; }
    public void setCertificationId(int certificationId) { this.certificationId = certificationId; }

    public int getResumeId() { return resumeId; }
    public void setResumeId(int resumeId) { this.resumeId = resumeId; }

    public String getCertificationName() { return certificationName; }
    public void setCertificationName(String certificationName) { this.certificationName = certificationName; }

    public String getIssuingOrg() { return issuingOrg; }
    public void setIssuingOrg(String issuingOrg) { this.issuingOrg = issuingOrg; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public String getCredentialUrl() { return credentialUrl; }
    public void setCredentialUrl(String credentialUrl) { this.credentialUrl = credentialUrl; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public String getFormattedCertification() {
        StringBuilder sb = new StringBuilder();
        sb.append(certificationName);
        if (issuingOrg != null && !issuingOrg.trim().isEmpty()) {
            sb.append(" - ").append(issuingOrg.trim());
        }
        if (issueDate != null) {
            sb.append(" (").append(issueDate).append(")");
        }
        if (credentialUrl != null && !credentialUrl.trim().isEmpty()) {
            sb.append(" [").append(credentialUrl.trim()).append("]");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedCertification();
    }
}
