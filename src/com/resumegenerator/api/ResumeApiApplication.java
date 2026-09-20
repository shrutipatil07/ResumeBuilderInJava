package com.resumegenerator.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// ---------------------------------------------------------------
// ResumeApiApplication — the Spring Boot entry point for the
// REST API layer.
//
// This is a SEPARATE main class from ResumeApp (which launches
// the Swing UI). Both can coexist in the same project:
//
//   Swing UI:  mvn exec:java -Dexec.mainClass=com.resumegenerator.ResumeApp
//   REST API:  mvn spring-boot:run
//
// @SpringBootApplication combines three annotations:
//   @Configuration       — this class can define @Bean methods
//   @EnableAutoConfiguration — Spring auto-configures based on
//                              classpath (e.g., Tomcat, Jackson)
//   @ComponentScan      — scans THIS package and sub-packages
//                          for @Controller, @Service, etc.
//
// scanBasePackages: We tell Spring to scan "com.resumegenerator"
//   (the root package) so it can discover controllers in the
//   "api" sub-package. Without this, Spring would only scan
//   "com.resumegenerator.api" by default.
// ---------------------------------------------------------------
@SpringBootApplication(scanBasePackages = "com.resumegenerator")
public class ResumeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeApiApplication.class, args);
    }
}
