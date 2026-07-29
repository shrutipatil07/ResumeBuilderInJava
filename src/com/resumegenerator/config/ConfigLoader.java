package com.resumegenerator.config;

// Properties — a hash-table subclass that reads key=value pairs from .properties files
import java.util.Properties;

// InputStream — represents a raw byte stream; used to read the config file from the classpath
import java.io.InputStream;

// IOException — checked exception thrown when the file cannot be read
import java.io.IOException;

/**
 * ConfigLoader reads database credentials from config.properties at runtime.
 *
 * WHY THIS CLASS EXISTS:
 * Without it, you would hardcode "root" and "password" directly in your Java code.
 * That means every time the password changes, you recompile. Worse, if you push
 * the code to GitHub, your credentials are exposed. This class solves both problems
 * by loading credentials from an external file.
 */
public class ConfigLoader {

    // ---------------------------------------------------------------
    // 'properties' holds every key=value pair read from the file.
    // It is an instance of java.util.Properties, which internally
    // uses a Hashtable<String, String> — so lookups are O(1).
    // ---------------------------------------------------------------
    private static final Properties properties = new Properties();

    // ---------------------------------------------------------------
    // STATIC INITIALIZER BLOCK
    // ---------------------------------------------------------------
    // This block runs exactly ONCE — the first time any code
    // references the ConfigLoader class. It loads the config file
    // before any method can be called, so the properties are always
    // ready to use.
    // ---------------------------------------------------------------
    static {
        // ----------------------------------------------------------
        // 'try-with-resources' ensures the InputStream is
        // automatically closed after the block finishes,
        // even if an exception is thrown. This prevents
        // file-handle leaks.
        // ----------------------------------------------------------
        try (
            // -------------------------------------------------------
            // ConfigLoader.class.getClassLoader()
            //   → gets the ClassLoader that loaded this class.
            //
            // .getResourceAsStream("config.properties")
            //   → searches the classpath (src/ folder at dev time,
            //     or the JAR's root at runtime) for a file named
            //     "config.properties" and opens it as a byte stream.
            //
            // Returns null if the file is not found — we handle
            // that case below.
            // -------------------------------------------------------
            InputStream input = ConfigLoader.class
                                    .getClassLoader()
                                    .getResourceAsStream("config.properties")
        ) {
            // -------------------------------------------------------
            // If the file was not found on the classpath, input is
            // null. We throw immediately with a clear message so the
            // developer knows exactly what went wrong.
            // -------------------------------------------------------
            if (input == null) {
                throw new RuntimeException(
                    "config.properties not found on the classpath. "
                  + "Place it in the src/ folder."
                );
            }

            // -------------------------------------------------------
            // properties.load(input)
            //   → reads every line of the InputStream, parses each
            //     "key=value" pair, and stores it in the Properties
            //     hash table. Lines starting with '#' are treated
            //     as comments and skipped automatically.
            // -------------------------------------------------------
            properties.load(input);

        } catch (IOException e) {
            // -------------------------------------------------------
            // If the file exists but cannot be read (e.g., permission
            // denied, disk error), we wrap the checked IOException
            // in an unchecked RuntimeException. This crashes the app
            // early with a meaningful stack trace — fail-fast design.
            // -------------------------------------------------------
            throw new RuntimeException(
                "Failed to load config.properties", e
            );
        }
    }

    // ===============================================================
    // PUBLIC GETTER METHODS
    // ===============================================================
    // Each method below calls properties.getProperty("key"), which
    // returns the value associated with that key, or null if the
    // key does not exist.
    // ===============================================================

    /**
     * Returns the JDBC driver class name.
     * Example value: "com.mysql.cj.jdbc.Driver"
     *
     * WHY: Class.forName(driver) registers the driver with
     *      DriverManager before you open a connection.
     */
    public static String getDriver() {
        return properties.getProperty("db.driver");
    }

    /**
     * Returns the JDBC connection URL.
     * Example value: "jdbc:mysql://localhost:3306/resume_builder"
     *
     * WHY: DriverManager.getConnection(url, user, pass) needs
     *      this to know which server, port, and database to use.
     */
    public static String getUrl() {
        return properties.getProperty("db.url");
    }

    /**
     * Returns the database username.
     * Example value: "root"
     */
    public static String getUsername() {
        return properties.getProperty("db.username");
    }

    /**
     * Returns the database password.
     * Example value: "your_password_here"
     */
    public static String getPassword() {
        return properties.getProperty("db.password");
    }

    /**
     * Returns the connection pool size as an integer.
     * Defaults to 5 if the key is missing or not a valid number.
     *
     * WHY: A pool reuses connections instead of opening a new one
     *      for every query — much faster under load.
     */
    public static int getPoolSize() {
        // -------------------------------------------------------
        // properties.getProperty("db.pool.size", "5")
        //   → returns the value for key "db.pool.size".
        //     If the key is missing, returns the default "5".
        //
        // Integer.parseInt(...)
        //   → converts the String to an int.
        // -------------------------------------------------------
        return Integer.parseInt(
            properties.getProperty("db.pool.size", "5")
        );
    }
}
