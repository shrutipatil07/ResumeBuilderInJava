package com.resumegenerator.model;

// ---------------------------------------------------------------
// LoginUser — the data model (POJO) that represents a single
// row in the authentication / login_users table.
//
// RESPONSIBILITY:
//   Hold the credentials and identity of a registered user:
//     • userId   — auto-generated primary key
//     • username — the display / login name
//     • email    — the user's email address
//     • password — the user's password (will be hashed later)
//
//   This class does NOT contain any business logic or database
//   code. It is a pure data carrier passed between the DAO,
//   service, and UI layers.
//
// ENCAPSULATION:
//   All fields are private. The outside world can only read or
//   modify them through public getters and setters. This ensures:
//     1. Controlled access — we can add validation inside a
//        setter at any time without changing the callers.
//     2. Internal freedom — we can rename or restructure the
//        private fields without breaking external code.
//     3. Data integrity — no one can directly set userId to -1
//        or username to null without going through our methods.
// ---------------------------------------------------------------

public class LoginUser {

    // ---------------------------------------------------------------
    // PRIVATE FIELDS
    // ---------------------------------------------------------------
    // Declaring these as 'private' is the core of encapsulation.
    // No other class can access them directly — they MUST use the
    // public getters/setters below.
    // ---------------------------------------------------------------
    private int userId;
    private String username;
    private String email;
    private String password;

    // ---------------------------------------------------------------
    // DEFAULT (NO-ARG) CONSTRUCTOR
    // ---------------------------------------------------------------
    // WHY: Needed when the DAO creates a LoginUser object and
    //      populates it field-by-field from a ResultSet:
    //        LoginUser user = new LoginUser();
    //        user.setUserId(rs.getInt("id"));
    //        user.setUsername(rs.getString("username"));
    //        ...
    // ---------------------------------------------------------------
    public LoginUser() {
    }

    // ---------------------------------------------------------------
    // PARAMETERIZED CONSTRUCTOR
    // ---------------------------------------------------------------
    // WHY: Convenient when all values are known upfront.
    //      Uses 'this' keyword to distinguish the private fields
    //      from the constructor parameters that share the same name.
    //
    //      this.username = username;
    //           ↑ field      ↑ parameter
    // ---------------------------------------------------------------
    public LoginUser(int userId, String username, String email, String password) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // ===============================================================
    //  GETTERS — public methods that READ private field values
    // ===============================================================
    // WHY GETTERS?
    //   Because the fields are private, other classes (LoginDAO,
    //   AuthenticationService, LoginFrame) cannot do:
    //       user.username   ← COMPILE ERROR (private)
    //   They must use:
    //       user.getUsername()   ← allowed (public method)
    // ===============================================================

    public int getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    // ===============================================================
    //  SETTERS — public methods that WRITE private field values
    // ===============================================================
    // WHY SETTERS?
    //   They provide controlled modification. If tomorrow we need
    //   to enforce "username must be at least 3 characters", we
    //   add the check INSIDE setUsername() — every caller gets the
    //   validation automatically without changing their own code.
    //
    //   'this.field = parameter' assigns the incoming value to the
    //   object's own field, resolving the name clash.
    // ===============================================================

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ---------------------------------------------------------------
    // main — quick test to verify encapsulation works correctly
    // ---------------------------------------------------------------
    // public static void main(String[] args) {
    //     LoginUser user = new LoginUser(1, "Shruti", "shruti@email.com", "1234");
    //     System.out.println(user.getUserId());
    //     System.out.println(user.getUsername());
    //     System.out.println(user.getEmail());
    //     System.out.println(user.getPassword());
    // }
}
