package com.resumegenerator.resume;

import com.resumegenerator.model.User;
import java.util.ArrayList;
import java.util.Arrays;

// ---------------------------------------------------------------
// TemplateTest — TEMPORARY verification class.
// Not part of production code. Delete after confirming all three
// templates render correctly from the same data.
//
// Purpose: prove ClassicTemplate, ModernTemplate, and
// MinimalTemplate all satisfy ResumeTemplate and produce
// genuinely different output from identical input.
// ---------------------------------------------------------------
public class TemplateTest {

    public static void main(String[] args) {
        // Single shared User instance — same data goes into every template
        ArrayList<String> skills = new ArrayList<>(Arrays.asList("Java", "SQL", "Git"));
        User user = new User(
                "Shruti Sharma",
                "shruti@example.com",
                "9876543210",
                "B.Tech Computer Science",
                skills,
                "1 year internship at a fintech startup",
                "Resume Generator (Java Swing)",
                "Oracle Certified Java Programmer",
                "Seeking a software engineering role to apply CS fundamentals.",
                1
        );

        // Declared as ResumeTemplate — the variable's static type is the
        // interface, not the concrete class. This is what proves
        // polymorphism: each object is swapped in through the SAME
        // reference type.
        ResumeTemplate[] templates = {
                new ClassicTemplate(),
                new ModernTemplate(),
                new MinimalTemplate()
        };

        for (ResumeTemplate template : templates) {
            System.out.println("=== " + template.getClass().getSimpleName() + " ===");
            System.out.println(template.render(user));
            System.out.println();
        }

        ResumeTemplate t = TemplateFactory.create(TemplateType.MODERN);
System.out.println(t.getClass().getSimpleName()); // should print: ModernTemplate
System.out.println(t instanceof ModernTemplate);    // should print: true
    }
}