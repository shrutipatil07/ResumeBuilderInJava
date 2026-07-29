package com.resumegenerator.dao;

// ---------------------------------------------------------------
// Connection — the live session with MySQL that we get from
// DatabaseManager.getConnection(). Every SQL operation runs
// through this object.
// ---------------------------------------------------------------
import java.sql.Connection;

// ---------------------------------------------------------------
// PreparedStatement — a pre-compiled SQL template with
// placeholders (?). It is safer and faster than concatenating
// user input directly into a SQL string.
//
// WHY NOT Statement?
// Statement builds SQL by string concatenation:
//   "INSERT INTO users VALUES('" + name + "')"
// If name = "'; DROP TABLE users; --", you get SQL injection.
// PreparedStatement escapes all input automatically.
// ---------------------------------------------------------------
import java.sql.PreparedStatement;

// ---------------------------------------------------------------
// ResultSet — holds the rows returned by a SELECT query.
// Here we use it to retrieve the auto-generated user_id
// after an INSERT.
// ---------------------------------------------------------------
import java.sql.ResultSet;

// ---------------------------------------------------------------
// Statement — we only import this for the constant
// Statement.RETURN_GENERATED_KEYS, which tells the driver
// to return the auto-increment ID after INSERT.
// We do NOT use Statement to execute queries.
// ---------------------------------------------------------------
import java.sql.Statement;

// ---------------------------------------------------------------
// SQLException — checked exception for any JDBC failure
// (bad SQL syntax, constraint violation, connection lost, etc.).
// ---------------------------------------------------------------
import java.sql.SQLException;

// ---------------------------------------------------------------
// ArrayList — needed to construct the User object, which stores
// skills as an ArrayList<String>.
// ---------------------------------------------------------------
import java.util.ArrayList;

// ---------------------------------------------------------------
// Our own classes
// ---------------------------------------------------------------
import com.resumegenerator.model.User;
import com.resumegenerator.db.DatabaseManager;

/**
 * UserDAO — Data Access Object for the 'users' table.
 *
 * WHY THIS CLASS EXISTS:
 * It separates database logic from business logic.
 * Without it, SQL strings would be scattered across your
 * UI code (ResumeBuilder), model code (User), and everywhere
 * else. The DAO pattern puts ALL database operations for one
 * table in one class.
 *
 * Currently implements:
 *   - save(User user)     → INSERT
 *   - findById(int id)    → SELECT by primary key
 *   - findAll()           → SELECT all users
 */
public class UserDAO {

    // ===============================================================
    // SQL TEMPLATE
    // ===============================================================
    // This is the INSERT statement with three ? placeholders.
    //
    // Column mapping:
    //   ?1 → full_name   (from user.getName())
    //   ?2 → email       (from user.getEmail())
    //   ?3 → phone       (from user.getPhone())
    //
    // Columns NOT listed here:
    //   user_id    → AUTO_INCREMENT, MySQL generates it
    //   password_hash → NULL for now (auth not implemented)
    //   created_at → DEFAULT CURRENT_TIMESTAMP, MySQL fills it
    //   updated_at → DEFAULT CURRENT_TIMESTAMP, MySQL fills it
    //
    // WHY DEFINE IT AS A CONSTANT?
    // If the SQL is written inline inside the method, you might
    // accidentally have typos in different methods. A constant
    // ensures the SQL is written once and reused.
    // ===============================================================
    private static final String INSERT_USER_SQL =
        "INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)";

    // ===============================================================
    // SQL TEMPLATE — SELECT by primary key
    // ===============================================================
    // SELECT * returns all columns for the row whose user_id
    // matches the ? placeholder.
    //
    // WHY "SELECT *" HERE?
    //   For a findById that returns the full User object, we need
    //   every column. In performance-critical code you'd list
    //   specific columns, but for a single-row PK lookup the
    //   difference is negligible.
    //
    // WHY "WHERE user_id = ?"?
    //   user_id is the PRIMARY KEY, so MySQL uses the clustered
    //   index — this is an O(log n) lookup, effectively instant
    //   even with millions of rows.
    // ===============================================================
    private static final String SELECT_BY_ID_SQL =
        "SELECT * FROM users WHERE user_id = ?";

    // ===============================================================
    // SQL TEMPLATE — SELECT all users
    // ===============================================================
    // No WHERE clause → returns every row in the users table.
    //
    // ORDER BY created_at DESC → newest users appear first.
    // Without ORDER BY, MySQL returns rows in an undefined order
    // (usually insertion order for InnoDB, but NOT guaranteed).
    // Always specify ORDER BY when order matters to the caller.
    //
    // No ? placeholders → no parameters to bind. We still use
    // PreparedStatement (not Statement) for consistency and to
    // benefit from the server-side execution plan cache.
    // ===============================================================
    private static final String SELECT_ALL_SQL =
        "SELECT * FROM users ORDER BY created_at DESC";

    // ===============================================================
    //  save(User user) — inserts a new user into the database
    // ===============================================================
    /**
     * Saves a User to the 'users' table and returns the
     * auto-generated user_id.
     *
     * @param user the User object to persist
     * @return the generated user_id (primary key), or -1 if the insert failed
     * @throws SQLException if a database error occurs
     */
    public int save(User user) throws SQLException {

        // -----------------------------------------------------------
        // STEP 1: Open a connection
        // -----------------------------------------------------------
        // DatabaseManager.getConnection() returns a fresh Connection
        // to the resume_builder database using the credentials from
        // config.properties.
        //
        // We declare conn, pstmt, and rs outside the try block so
        // we can close them in the finally block regardless of
        // whether the try succeeds or throws.
        // -----------------------------------------------------------
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // STEP 2: Create a PreparedStatement
            // -------------------------------------------------------
            //
            // conn.prepareStatement(sql, flag) does two things:
            //
            //   a) Sends the SQL template to MySQL for PARSING and
            //      COMPILATION. MySQL creates an execution plan
            //      (which tables to scan, which indexes to use)
            //      but does NOT execute it yet. The ? placeholders
            //      are just slots waiting to be filled.
            //
            //   b) Statement.RETURN_GENERATED_KEYS tells the JDBC
            //      driver: "After the INSERT, I want to read back
            //      the auto-generated user_id." Without this flag,
            //      getGeneratedKeys() would return an empty ResultSet.
            //
            // WHY prepareStatement() INSTEAD OF createStatement()?
            //   - SQL injection protection: values are bound
            //     separately, never concatenated into the SQL string
            //   - Performance: if you call save() 100 times, MySQL
            //     reuses the same execution plan instead of parsing
            //     the SQL 100 times
            //   - Type safety: setString() ensures proper escaping
            //     of quotes, backslashes, and Unicode characters
            // -------------------------------------------------------
            pstmt = conn.prepareStatement(
                INSERT_USER_SQL,
                Statement.RETURN_GENERATED_KEYS
            );

            // -------------------------------------------------------
            // STEP 3: Bind values to the ? placeholders
            // -------------------------------------------------------
            //
            // pstmt.setString(parameterIndex, value)
            //
            //   parameterIndex — the position of the ? in the SQL,
            //     starting from 1 (not 0). This is a JDBC convention.
            //
            //   value — the Java String to bind. PreparedStatement
            //     will automatically:
            //       • Wrap it in single quotes
            //       • Escape special characters (e.g., O'Brien → O\'Brien)
            //       • Handle NULL values if you pass null
            //
            // Our SQL:  INSERT INTO users (full_name, email, phone)
            //                      VALUES (?1,        ?2,    ?3)
            // -------------------------------------------------------

            // ?1 → full_name column ← user.getName()
            pstmt.setString(1, user.getName());

            // ?2 → email column ← user.getEmail()
            pstmt.setString(2, user.getEmail());

            // ?3 → phone column ← user.getPhone()
            pstmt.setString(3, user.getPhone());

            // -------------------------------------------------------
            // STEP 4: Execute the INSERT
            // -------------------------------------------------------
            //
            // pstmt.executeUpdate()
            //
            //   Sends the compiled SQL + bound values to MySQL.
            //   MySQL inserts the row and returns an int:
            //     • The number of rows affected (1 for a successful
            //       single-row INSERT, 0 if nothing was inserted).
            //
            //   WHY executeUpdate() AND NOT executeQuery()?
            //     • executeQuery()  → for SELECT (returns a ResultSet)
            //     • executeUpdate() → for INSERT, UPDATE, DELETE
            //       (returns an int — the affected row count)
            //
            //   We store the result in 'rowsAffected' to verify
            //   that exactly 1 row was inserted.
            // -------------------------------------------------------
            int rowsAffected = pstmt.executeUpdate();

            // -------------------------------------------------------
            // STEP 5: Retrieve the auto-generated user_id
            // -------------------------------------------------------
            //
            // pstmt.getGeneratedKeys()
            //
            //   Returns a ResultSet containing the auto-increment
            //   value(s) generated by the INSERT. For a single-row
            //   INSERT, this ResultSet has exactly one row with one
            //   column: the generated user_id.
            //
            //   This only works because we passed
            //   Statement.RETURN_GENERATED_KEYS in Step 2.
            //
            // rs.next()
            //
            //   Moves the cursor to the first (and only) row.
            //   Returns true if a row exists, false if empty.
            //
            // rs.getInt(1)
            //
            //   Reads the first column of the current row as an int.
            //   Column index is 1-based (JDBC convention).
            //   This is the user_id that MySQL auto-generated.
            //
            // WHY DO WE NEED THE GENERATED ID?
            //   When we later insert into the 'resumes' table, we
            //   need user_id as the foreign key. Without retrieving
            //   it here, we'd have to run a separate SELECT query
            //   to find it — wasteful and race-condition-prone.
            // -------------------------------------------------------
            if (rowsAffected > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            // If no rows were affected (shouldn't happen for a valid
            // INSERT), return -1 to signal failure to the caller.
            return -1;

        } finally {
            // -------------------------------------------------------
            // STEP 6: Clean up resources (ALWAYS runs)
            // -------------------------------------------------------
            //
            // The finally block executes whether the try block
            // succeeds or throws an exception. We close resources
            // in REVERSE order of creation:
            //   ResultSet → PreparedStatement → Connection
            //
            // WHY REVERSE ORDER?
            //   A ResultSet depends on its PreparedStatement, which
            //   depends on its Connection. Closing in reverse order
            //   ensures each resource is released before its parent.
            //
            // WHY INDIVIDUAL TRY-CATCH FOR EACH?
            //   If rs.close() throws, we still want to close pstmt
            //   and conn. Without individual catches, a single
            //   failure would skip the remaining close() calls
            //   and leak resources.
            // -------------------------------------------------------

            // Close ResultSet
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Close PreparedStatement
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Close Connection (delegates to DatabaseManager's safe close)
            DatabaseManager.closeConnection(conn);
        }
    }

    // ===============================================================
    //  findById(int id) — retrieves a single user by primary key
    // ===============================================================
    /**
     * Looks up a user by their user_id.
     *
     * @param id the primary key (user_id) to search for
     * @return the matching User object, or null if no user exists
     *         with that id
     * @throws SQLException if a database error occurs
     */
    public User findById(int id) throws SQLException {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // Prepare the SELECT statement.
            //
            // No RETURN_GENERATED_KEYS flag this time — we are
            // reading data, not inserting. The single-argument
            // version of prepareStatement() is sufficient.
            // -------------------------------------------------------
            pstmt = conn.prepareStatement(SELECT_BY_ID_SQL);

            // -------------------------------------------------------
            // Bind the user_id to ?1.
            //
            // pstmt.setInt(1, id)
            //   parameterIndex = 1 → the first (and only) ? in
            //   "SELECT * FROM users WHERE user_id = ?"
            //   setInt() binds a Java int, matching the INT column.
            // -------------------------------------------------------
            pstmt.setInt(1, id);

            // -------------------------------------------------------
            // Execute the SELECT.
            //
            // pstmt.executeQuery()
            //   → sends the SQL to MySQL and returns a ResultSet.
            //
            // WHY executeQuery() AND NOT executeUpdate()?
            //   • executeQuery()  → for SELECT (returns rows)
            //   • executeUpdate() → for INSERT/UPDATE/DELETE
            //     (returns an affected-row count)
            //
            // =====================================================
            //  WHAT IS A ResultSet?
            // =====================================================
            //
            // Think of ResultSet as a TABLE returned by MySQL,
            // loaded into Java's memory. It has:
            //   • ROWS   — one for each matching record
            //   • COLUMNS — one for each column in your SELECT
            //   • A CURSOR — an invisible pointer that starts
            //     BEFORE the first row
            //
            // Visual model of the cursor:
            //
            //   Cursor →  (BEFORE FIRST ROW)     ← rs starts here
            //             ┌────────┬──────────┬───────┐
            //   Row 1     │ user_id│ full_name│ email │
            //             ├────────┼──────────┼───────┤
            //   Row 2     │  ...   │   ...    │  ...  │
            //             └────────┴──────────┴───────┘
            //             (AFTER LAST ROW)
            //
            // The cursor starts BEFORE row 1. You must call
            // rs.next() to move it onto a row before you can
            // read any data.
            // =====================================================
            // -------------------------------------------------------
            rs = pstmt.executeQuery();

            // -------------------------------------------------------
            // READING THE ResultSet
            // -------------------------------------------------------
            //
            // =====================================================
            //  WHY while(rs.next()) ?
            // =====================================================
            //
            // rs.next() does TWO things:
            //   1. MOVES the cursor forward by one row
            //   2. RETURNS true if the cursor is now on a valid row,
            //      or false if it has moved past the last row
            //
            // So the while loop means:
            //   "Move to the next row. If a row exists, enter the
            //    loop body. If no more rows, stop."
            //
            // Iteration example (2 rows returned):
            //
            //   Call 1: rs.next() → cursor moves to Row 1 → true
            //           loop body reads Row 1 columns
            //   Call 2: rs.next() → cursor moves to Row 2 → true
            //           loop body reads Row 2 columns
            //   Call 3: rs.next() → cursor moves past last → false
            //           loop exits
            //
            // WHY NOT if(rs.next()) ?
            //   For findById we expect 0 or 1 rows (because user_id
            //   is a PRIMARY KEY). So if(rs.next()) would work here.
            //   BUT we use while(rs.next()) because:
            //     a) It's the standard JDBC idiom — every Java
            //        developer recognizes it instantly.
            //     b) It's forward-compatible — if the query ever
            //        returns multiple rows (e.g., you change the
            //        WHERE clause), the code still works.
            //     c) It handles the "0 rows" case automatically —
            //        the loop body simply never executes, and we
            //        fall through to return null.
            //
            // IMPORTANT: You CANNOT skip rs.next(). If you try to
            // call rs.getString() without calling rs.next() first,
            // JDBC throws: "Before start of result set" because the
            // cursor is still BEFORE row 1.
            // =====================================================
            while (rs.next()) {

                // ---------------------------------------------------
                // rs.getString("full_name")
                //
                //   Reads the value of the "full_name" column from
                //   the CURRENT row (the row the cursor is on).
                //
                //   You can also use rs.getString(2) to access by
                //   column index (1-based), but column NAMES are
                //   preferred because:
                //     • They're self-documenting ("full_name" vs 2)
                //     • They don't break if you add a column to the
                //       table or reorder columns in the SELECT
                //
                // rs.getInt("user_id")
                //   Same idea, but returns an int instead of String.
                //   JDBC automatically converts the MySQL INT to
                //   a Java int.
                // ---------------------------------------------------
                String name  = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");

                // ---------------------------------------------------
                // Construct and return the User object.
                //
                // The User constructor requires all fields, but the
                // users table only stores name, email, phone. The
                // remaining fields (education, skills, experience,
                // etc.) live in separate normalized tables and will
                // be loaded by their own DAOs in the future.
                //
                // For now we pass empty/default values for those
                // fields since this DAO only handles the users table.
                // ---------------------------------------------------
                return new User(
                    name,                    // full_name
                    email,                   // email
                    phone,                   // phone
                    "",                      // education   (separate table)
                    new ArrayList<>(),       // skills      (separate table)
                    "",                      // experience  (separate table)
                    "",                      // projects    (separate table)
                    "",                      // certifications (separate table)
                    "",                      // objective   (resumes table)
                    0                        // experienceYears
                );
            }

            // -------------------------------------------------------
            // If rs.next() returned false on the very first call,
            // the while loop body never executed. This means no
            // user with that id exists in the database.
            // Returning null signals "not found" to the caller.
            // -------------------------------------------------------
            return null;

        } finally {
            // Close in reverse order: ResultSet → PreparedStatement → Connection
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            DatabaseManager.closeConnection(conn);
        }
    }

    // ===============================================================
    //  findAll() — retrieves every user from the database
    // ===============================================================
    //
    // ===============================================================
    //  WHY DOES THIS METHOD RETURN ArrayList<User>
    //  INSTEAD OF ResultSet?
    // ===============================================================
    //
    //  You might wonder: "The ResultSet already contains all the
    //  data. Why not just return it and let the caller read it?"
    //
    //  Here are 4 reasons why that's a bad idea:
    //
    //  1. RESOURCE SAFETY
    //     A ResultSet is tied to its PreparedStatement, which is
    //     tied to its Connection. If we return the ResultSet, we
    //     CANNOT close the Connection in our finally block —
    //     because closing the Connection automatically closes the
    //     ResultSet too. The caller would receive a dead ResultSet.
    //
    //     To keep it alive, we'd have to leave the Connection open
    //     and trust the caller to close it. If they forget, we
    //     leak connections → MySQL hits "Too many connections" →
    //     the entire application dies.
    //
    //  2. ENCAPSULATION (Hiding database details)
    //     ResultSet is a JDBC class — it belongs to the database
    //     layer. If your UI code (ResumeBuilder) receives a
    //     ResultSet, it now depends on java.sql.* imports, column
    //     names, and SQL types. Change a column name in MySQL and
    //     you break the UI. By returning User objects, only the
    //     DAO needs to know about column names.
    //
    //  3. TESTABILITY
    //     You can easily create an ArrayList<User> in a unit test:
    //        List<User> fakeUsers = new ArrayList<>();
    //        fakeUsers.add(new User("Alice", ...));
    //     You CANNOT easily create a fake ResultSet — it requires
    //     a live database connection or a complex mock.
    //
    //  4. SEPARATION OF CONCERNS
    //     The DAO's job is: "Talk to the database and give me
    //     Java objects." The UI's job is: "Display Java objects."
    //     Neither should know about the other's internals.
    //
    //     Database → DAO → User objects → UI
    //     (SQL)      (converts)           (displays)
    //
    //  RULE OF THUMB:
    //    ResultSet should NEVER leave the DAO class. Convert it
    //    to model objects (User, Resume, etc.) inside the DAO,
    //    close all resources, and return clean Java objects.
    // ===============================================================
    /**
     * Retrieves all users from the database.
     *
     * @return an ArrayList of User objects (empty list if no users exist)
     * @throws SQLException if a database error occurs
     */
    public ArrayList<User> findAll() throws SQLException {

        // -----------------------------------------------------------
        // We create the list OUTSIDE the try block so it's accessible
        // in the return statement. Even if the ResultSet is empty,
        // we return an empty list — never null.
        //
        // WHY NEVER RETURN NULL?
        //   If findAll() returns null, every caller must write:
        //     if (users != null) { for (User u : users) { ... } }
        //   If it returns an empty list, callers just write:
        //     for (User u : users) { ... }
        //   The loop body simply never executes. Cleaner, safer.
        // -----------------------------------------------------------
        ArrayList<User> users = new ArrayList<>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // Prepare and execute — no parameters to bind this time,
            // so we go straight from prepareStatement to executeQuery.
            // -------------------------------------------------------
            pstmt = conn.prepareStatement(SELECT_ALL_SQL);
            rs = pstmt.executeQuery();

            // -------------------------------------------------------
            // ITERATE through every row in the ResultSet.
            //
            // This is where while(rs.next()) truly shines — unlike
            // findById (which returns at most 1 row), findAll can
            // return thousands of rows. The while loop processes
            // each one:
            //
            //   Iteration 1: rs.next() → cursor on Row 1 → true
            //     read columns → build User → add to list
            //   Iteration 2: rs.next() → cursor on Row 2 → true
            //     read columns → build User → add to list
            //   ...repeat for every row...
            //   Last call:   rs.next() → past end → false → exit
            //
            // If the table is empty, rs.next() returns false on the
            // very first call and the loop body never executes.
            // We return the empty ArrayList — no special handling.
            // -------------------------------------------------------
            while (rs.next()) {

                // ---------------------------------------------------
                // Extract columns from the CURRENT row.
                //
                // We use column NAMES (not indexes) for readability
                // and resilience to schema changes.
                // ---------------------------------------------------
                String name  = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");

                // ---------------------------------------------------
                // Build a User object from this row and add it
                // to the list.
                //
                // Same as findById — we pass defaults for fields
                // that live in other normalized tables.
                // ---------------------------------------------------
                User user = new User(
                    name,                    // full_name
                    email,                   // email
                    phone,                   // phone
                    "",                      // education   (separate table)
                    new ArrayList<>(),       // skills      (separate table)
                    "",                      // experience  (separate table)
                    "",                      // projects    (separate table)
                    "",                      // certifications (separate table)
                    "",                      // objective   (resumes table)
                    0                        // experienceYears
                );

                users.add(user);
            }

        } finally {
            // Close in reverse order: ResultSet → PreparedStatement → Connection
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            DatabaseManager.closeConnection(conn);
        }

        // -----------------------------------------------------------
        // Return the fully-built list AFTER all resources are closed.
        //
        // This is the whole point: the Connection, PreparedStatement,
        // and ResultSet are all closed. The data now lives safely in
        // plain Java objects (ArrayList<User>) that the caller can
        // use forever — no database dependency.
        // -----------------------------------------------------------
        return users;
    }
}
