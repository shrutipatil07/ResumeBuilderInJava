package com.resumegenerator.model;

public class Skill {
    private int skillId;
    private String skillName;
    private ProficiencyLevel proficiencyLevel;
    private int displayOrder;

    public Skill() {
    }

    public Skill(String skillName) {
        this.skillName = skillName;
    }

    public Skill(String skillName, ProficiencyLevel proficiencyLevel, int displayOrder) {
        this.skillName = skillName;
        this.proficiencyLevel = proficiencyLevel;
        this.displayOrder = displayOrder;
    }

    public Skill(int skillId, String skillName, ProficiencyLevel proficiencyLevel, int displayOrder) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.proficiencyLevel = proficiencyLevel;
        this.displayOrder = displayOrder;
    }

    public int getSkillId() { return skillId; }
    public void setSkillId(int skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public ProficiencyLevel getProficiencyLevel() { return proficiencyLevel; }
    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) { this.proficiencyLevel = proficiencyLevel; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    @Override
    public String toString() {
        return skillName + (proficiencyLevel != null ? " (" + proficiencyLevel + ")" : "");
    }
}
