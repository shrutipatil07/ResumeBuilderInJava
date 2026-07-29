package com.resumegenerator;

import com.resumegenerator.ui.ResumeBuilder;

public class ResumeApp {
    public static void main(String[] args) {
        //new ResumeBuilder();
        Properties config = ConfigLoader.load();
        System.out.println(config.getProperty("db.url"));
    }
}
