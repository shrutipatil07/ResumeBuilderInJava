package com.resumegenerator.ui;

import com.resumegenerator.model.LoginUser;
import com.resumegenerator.model.Resume;
import com.resumegenerator.service.ResumeService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Dashboard — Upgraded interactive management screen for user's resumes.
 *
 * Features:
 * - Displays "Welcome, <username>!" header
 * - Live keyword search and ResumeType filtering
 * - JTable displaying resume metadata (ID, Title, Type, Updated Date)
 * - Actions: Create Resume, Open / Edit Resume, Delete Resume, Logout
 * - Auto-refreshes data when Builder saves or closes
 */
public class Dashboard extends JFrame {

    private LoginUser currentUser;
    private ResumeService resumeService;

    // Controls
    private JTextField searchField;
    private JComboBox<String> typeFilterCombo;
    private JTable resumesTable;
    private DefaultTableModel tableModel;
    private JButton createResumeButton;
    private JButton openResumeButton;
    private JButton deleteResumeButton;
    private JButton logoutButton;

    private static final SimpleDateFormat DATE_FORMATter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public Dashboard(LoginUser user) {
        this.currentUser = user;
        this.resumeService = new ResumeService();

        setTitle("Dashboard - Resume Builder");
        setSize(800, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Header & Welcome Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + (currentUser != null ? currentUser.getUsername() : "User") + "!", SwingConstants.LEFT);
        welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        headerPanel.add(logoutButton, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Center Panel with Search, Filter & Table
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        // Search & Filter Toolbar
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbarPanel.setBorder(BorderFactory.createTitledBorder("Search & Filter Resumes"));

        toolbarPanel.add(new JLabel("Search:"));
        searchField = new JTextField(15);
        toolbarPanel.add(searchField);

        toolbarPanel.add(new JLabel("Type:"));
        typeFilterCombo = new JComboBox<>(new String[]{"All", "FRESHER", "EXPERIENCED"});
        toolbarPanel.add(typeFilterCombo);

        createResumeButton = new JButton("+ Create Resume");
        createResumeButton.setFont(createResumeButton.getFont().deriveFont(Font.BOLD));
        toolbarPanel.add(createResumeButton);

        centerPanel.add(toolbarPanel, BorderLayout.NORTH);

        // Resumes JTable
        String[] columnNames = {"Resume ID", "Title", "Type", "Last Updated"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table cells read-only
            }
        };

        resumesTable = new JTable(tableModel);
        resumesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resumesTable.setRowHeight(25);
        resumesTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        resumesTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        resumesTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        resumesTable.getColumnModel().getColumn(3).setPreferredWidth(160);

        JScrollPane tableScrollPane = new JScrollPane(resumesTable);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Action Buttons Panel
        JPanel bottomActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        openResumeButton = new JButton("Open / Edit Resume");
        deleteResumeButton = new JButton("Delete Resume");

        bottomActionPanel.add(openResumeButton);
        bottomActionPanel.add(deleteResumeButton);
        mainPanel.add(bottomActionPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Event Listeners
        createResumeButton.addActionListener(e -> openCreateResume());
        openResumeButton.addActionListener(e -> openSelectedResume());
        deleteResumeButton.addActionListener(e -> deleteSelectedResume());

        // Dynamic Search Listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { loadResumesTable(); }
            @Override public void removeUpdate(DocumentEvent e) { loadResumesTable(); }
            @Override public void changedUpdate(DocumentEvent e) { loadResumesTable(); }
        });

        // Filter Dropdown Listener
        typeFilterCombo.addActionListener(e -> loadResumesTable());

        // Double click on row to open
        resumesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    openSelectedResume();
                }
            }
        });

        // Initial Load
        loadResumesTable();

        setVisible(true);
    }

    /**
     * Loads/filters the resume list for the current user and populates the JTable.
     */
    public void loadResumesTable() {
        if (currentUser == null || currentUser.getUserId() <= 0) return;

        tableModel.setRowCount(0); // Clear table
        String keyword = searchField.getText().trim();
        String typeChoice = (String) typeFilterCombo.getSelectedItem();
        String typeFilter = ("All".equals(typeChoice)) ? null : typeChoice;

        try {
            List<Resume> resumes = resumeService.searchResumes(currentUser.getUserId(), keyword, typeFilter);
            for (Resume r : resumes) {
                String updatedStr = "";
                if (r.getUpdatedAt() != null) {
                    updatedStr = DATE_FORMATter.format(r.getUpdatedAt());
                } else if (r.getCreatedAt() != null) {
                    updatedStr = DATE_FORMATter.format(r.getCreatedAt());
                }
                Object[] rowData = {
                        r.getResumeId(),
                        r.getTitle() != null ? r.getTitle() : "Untitled Resume",
                        r.getResumeType() != null ? r.getResumeType().name() : "FRESHER",
                        updatedStr
                };
                tableModel.addRow(rowData);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load resumes: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void openCreateResume() {
        new ResumeBuilder(currentUser, null, () -> loadResumesTable());
    }

    private void openSelectedResume() {
        int selectedRow = resumesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resume from the table to open.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resumeId = (int) tableModel.getValueAt(selectedRow, 0);
        try {
            Resume fullResume = resumeService.getResume(resumeId, currentUser.getUserId());
            if (fullResume == null) {
                JOptionPane.showMessageDialog(this, "Could not load selected resume. It may have been deleted.", "Error", JOptionPane.ERROR_MESSAGE);
                loadResumesTable();
                return;
            }
            new ResumeBuilder(currentUser, fullResume, () -> loadResumesTable());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to fetch resume: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void deleteSelectedResume() {
        int selectedRow = resumesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a resume from the table to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int resumeId = (int) tableModel.getValueAt(selectedRow, 0);
        String title = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete resume '" + title + "' (ID #" + resumeId + ")?\nThis action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = resumeService.deleteResume(resumeId, currentUser.getUserId());
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Resume deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    loadResumesTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete resume.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error while deleting: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public void logout() {
        dispose();
        new LoginFrame();
    }

    public static void main(String[] args) {
        LoginUser testUser = new LoginUser();
        testUser.setUserId(1);
        testUser.setUsername("DemoUser");
        new Dashboard(testUser);
    }
}
