package com.resumegenerator.resume;

import com.resumegenerator.model.*;

import java.time.LocalDate;

public class TemplateTest {

    public static void main(String[] args) {
        User user = new User("Shruti Sharma", "shruti@example.com", "9876543210");

        com.resumegenerator.model.Resume resume = new com.resumegenerator.model.Resume();
        resume.setUser(user);
        resume.setObjective("Seeking a software engineering role to apply CS fundamentals.");

        resume.addEducation(new Education("ABC Tech University", "B.Tech", "Computer Science", 2020, 2024, "8.5 CGPA", 1));
        resume.addSkill(new Skill("Java", ProficiencyLevel.ADVANCED, 1));
        resume.addSkill(new Skill("SQL", ProficiencyLevel.INTERMEDIATE, 2));
        resume.addSkill(new Skill("Git", ProficiencyLevel.INTERMEDIATE, 3));
        resume.addExperience(new Experience("Fintech Startup", "Software Engineer Intern", "Bangalore", LocalDate.of(2023, 6, 1), LocalDate.of(2023, 12, 31), "Developed backend API microservices.", 1));
        resume.addProject(new Project("Resume Generator", "Java Swing desktop app with MySQL", "Java, Swing, MySQL", "https://github.com/example/resume-generator", 1));
        resume.addCertification(new Certification("Oracle Certified Java Associate", "Oracle", LocalDate.of(2023, 5, 15), "https://oracle.com/cert/123", 1));

        ResumeTemplate[] templates = {
                new ClassicTemplate(),
                new ModernTemplate(),
                new MinimalTemplate()
        };

        for (ResumeTemplate template : templates) {
            System.out.println("=== " + template.getClass().getSimpleName() + " ===");
            System.out.println(template.render(resume));
            System.out.println();
        }

        ResumeTemplate t = TemplateFactory.create(TemplateType.MODERN);
        System.out.println("Factory created template: " + t.getClass().getSimpleName());
    }
}