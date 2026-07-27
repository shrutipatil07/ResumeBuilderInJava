package com.resumegenerator.model;

import java.util.ArrayList;

public class User {
    private String name;
    private String email;
    private String phone;
    private String education;
    private ArrayList<String> skills;
    private String experience;
    private String projects;
    private String certifications;
    private String objective;
    private int experienceYears;

    public User(String name, String email, String phone, String education, ArrayList<String> skills,
                String experience, String projects, String certifications, String objective, int experienceYears) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.education = education;
        this.skills = skills;
        this.experience = experience;
        this.projects = projects;
        this.certifications = certifications;
        this.objective = objective;
        this.experienceYears = experienceYears;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getEducation() { return education; }
    public ArrayList<String> getSkills() { return skills; }
    public String getExperienceDetails() { return experience; }
    public String getProjects() { return projects; }
    public String getCertifications() { return certifications; }
    public String getObjective() { return objective; }
    public int getExperienceYears() { return experienceYears; }
}
