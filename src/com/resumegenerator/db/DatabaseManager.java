package com.resumegenerator.db;

// ---------------------------------------------------------------
// Connection — represents a live session with the MySQL database.
// Every SQL query you execute travels through this object.
// It lives in the java.sql package (part of JDBC — Java's
// standard database API).
// ---------------------------------------------------------------
import java.sql.Connection;

// ---------------------------------------------------------------
// DriverManager — a factory that creates Connection objects.
// You give it a URL, username, and password; it returns a
// Connection by delegating to the registered JDBC driver.
// ---------------------------------------------------------------
import java.sql.DriverManager;

// ---------------------------------------------------------------
// SQLException — the checked exception that every JDBC operation
// can throw (bad credentials, server down, syntax error, etc.).
// We must either catch or declare it.
// ---------------------------------------------------------------
import java.sql.SQLException;

// ---------------------------------------------------------------
// ConfigLoader — our own class that reads db.url, db.username,
// db.password, and db.driver from config.properties so that
// no credentials are hardcoded in this file.
// ---------------------------------------------------------------
import com.resumegenerator.config.ConfigLoader;

/**
 * DatabaseManager is responsible for exactly two things:
 *   1. Opening a connection to MySQL
 *   2. Closing that connection
 *
 * WHY THIS CLASS EXISTS:
 * Without it, every class that needs the database would
 * independently call DriverManager.getConnection(...) with
 * its own URL, username, and password. That means:
 *   - Credential duplication everywhere
 *   - No single place to change the connection logic
 *   - Easy to forget closing the connection
 *
 * DatabaseManager centralizes all of that into one place.
 * Future DAO (Data Access Object) classes will call
 * DatabaseManager.getConnection() and never worry about
 * HOW the connection is made.
 */
public class DatabaseManager {

    // ---------------------------------------------------------------
    // STATIC INITIALIZER BLOCK
    // ---------------------------------------------------------------
    // Runs exactly ONCE when the JVM first loads this class.
    //
    // Class.forName(driverClassName) does two things:
    //   1. Loads the MySQL driver class into memory.
    //   2. The driver's own static block automatically registers
    //      itself with DriverManager, so DriverManager.getConnection()
    //      knows how to speak the MySQL protocol.
    //
    // WHY EXPLICIT LOADING?
    // Modern JDBC 4.0+ drivers auto-register via META-INF/services,
    // so this line is technically optional with MySQL Connector/J 8.x.
    // But we keep it because:
    //   a) It makes the dependency explicit — you see exactly which
    //      driver is expected.
    //   b) If the driver JAR is missing from the classpath, the app
    //      fails HERE with a clear ClassNotFoundException instead of
    //      a confusing "No suitable driver" error later.
    // ---------------------------------------------------------------
    static {
        try {
            // -------------------------------------------------------
            // ConfigLoader.getDriver() returns the String
            // "com.mysql.cj.jdbc.Driver" from config.properties.
            //
            // Class.forName(...) asks the JVM's ClassLoader to find
            // and load that class. If the MySQL Connector/J JAR is
            // not on the classpath, this throws ClassNotFoundException.
            // -------------------------------------------------------
            Class.forName(ConfigLoader.getDriver());
        } catch (ClassNotFoundException e) {
            // -------------------------------------------------------
            // Wrap in RuntimeException so the app crashes immediately
            // with a meaningful message — fail-fast design.
            //
            // If we silently swallowed this, getConnection() would
            // later throw "No suitable driver found" — a much harder
            // error to debug.
            // -------------------------------------------------------
            throw new RuntimeException(
                "MySQL JDBC Driver not found. "
              + "Add mysql-connector-java to your pom.xml dependencies.", e
            );
        }
    }

    // ===============================================================
    //  OPEN CONNECTION
    // ===============================================================
    /**
     * Creates and returns a NEW connection to the MySQL database.
     *
     * Each call opens a fresh TCP socket to the MySQL server,
     * authenticates with the username/password from config.properties,
     * and selects the database specified in the JDBC URL.
     *
     * @return a live Connection object ready for SQL operations
     * @throws SQLException if the server is unreachable, the
     *         credentials are wrong, or the database doesn't exist
     */
    public static Connection getConnection() throws SQLException {
        // -----------------------------------------------------------
        // DriverManager.getConnection(url, user, password)
        //
        //   url  → "jdbc:mysql://localhost:3306/resume_builder"
        //          Tells JDBC: use the MySQL protocol, connect to
        //          localhost on port 3306, and select the
        //          resume_builder database.
        //
        //   user → "root" (from config.properties)
        //
        //   pass → the password (from config.properties, never
        //          hardcoded here)
        //
        // Internally, DriverManager iterates over all registered
        // drivers and asks each one: "Can you handle this URL?"
        // The MySQL driver recognizes "jdbc:mysql://..." and
        // opens the connection.
        //
        // Returns a Connection object, or throws SQLException
        // if anything goes wrong.
        // -----------------------------------------------------------
        return DriverManager.getConnection(
            ConfigLoader.getUrl(),       // JDBC URL
            ConfigLoader.getUsername(),   // DB username
            ConfigLoader.getPassword()   // DB password
        );
    }

    // ===============================================================
    //  CLOSE CONNECTION
    // ===============================================================
    /**
     * Safely closes the given database connection.
     *
     * WHY A SEPARATE METHOD instead of just calling conn.close()?
     *   1. Null-safety — if conn is null (e.g., getConnection()
     *      was never called), calling conn.close() throws
     *      NullPointerException. This method checks first.
     *   2. Already-closed safety — calling close() on an already-
     *      closed connection throws SQLException. We guard that.
     *   3. Single responsibility — callers don't need to write
     *      the same try-catch-null-check boilerplate everywhere.
     *
     * @param connection the Connection to close (may be null)
     */
    public static void closeConnection(Connection connection) {
        // -----------------------------------------------------------
        // Guard clause: if connection is null, there is nothing
        // to close. Return silently — this is not an error.
        // This happens when getConnection() was never called or
        // threw an exception before returning.
        // -----------------------------------------------------------
        if (connection == null) {
            return;
        }

        try {
            // -------------------------------------------------------
            // connection.isClosed()
            //   → returns true if close() was already called on this
            //     connection. Calling close() again on some drivers
            //     is a no-op, but on others it throws. The check
            //     makes our code driver-agnostic.
            // -------------------------------------------------------
            if (!connection.isClosed()) {
                // ---------------------------------------------------
                // connection.close()
                //   → releases the JDBC Connection and the underlying
                //     TCP socket to the MySQL server. After this call,
                //     any attempt to use this connection for queries
                //     will throw "Connection is closed".
                //
                // WHY THIS MATTERS:
                // Each open connection holds a TCP socket + a thread
                // on the MySQL server. MySQL defaults to a max of
                // 151 connections. If you forget to close, you'll
                // eventually hit "Too many connections" and the
                // entire app stops working.
                // ---------------------------------------------------
                connection.close();
            }
        } catch (SQLException e) {
            // -------------------------------------------------------
            // If close() itself fails (rare — usually means the
            // server already dropped the connection), we print
            // the stack trace for debugging but do NOT crash the
            // app. Failing to close is not worth killing the
            // entire application over.
            // -------------------------------------------------------
            e.printStackTrace();
        }
    }
}
