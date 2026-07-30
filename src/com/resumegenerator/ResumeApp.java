package com.resumegenerator;

import com.resumegenerator.ui.RegisterFrame;

import javax.swing.JOptionPane;

public class ResumeApp {

    public static void main(String[] args) {

        System.out.println("1. Main Started");
        JOptionPane.showMessageDialog(null, "Main Started");

        RegisterFrame frame = new RegisterFrame();

        System.out.println("2. RegisterFrame Created");
        JOptionPane.showMessageDialog(null, "RegisterFrame Created");
    }
}