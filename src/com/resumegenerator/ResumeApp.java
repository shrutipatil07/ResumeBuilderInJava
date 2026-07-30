package com.resumegenerator;

import com.resumegenerator.ui.ResumeBuilder;
import com.resumegenerator.model.LoginUser;

public class ResumeApp {
    public static void main(String[] args) {
        //new ResumeBuilder();
        // Properties config = ConfigLoader.load();
        // System.out.println(config.getProperty("db.url"));
        //System.out.println(com.resumegenerator.config.ConfigLoader.getUrl());
        LoginUser user =new LoginUser(1,"Shruti","shruti@email.com","1234");
    }
}
