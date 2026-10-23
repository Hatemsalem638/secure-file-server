package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * User accounts stored in SQLite.
 *
 * <p>Every query uses a {@link PreparedStatement}, so user input is never
 * concatenated into SQL. Passwords are stored as PBKDF2-HMAC-SHA256 hashes
 * with a random salt per user (see {@link PasswordHasher}).</p>
 */
public class DB {

    private static final Logger LOG = Logger.getLogger(DB.class.getName());
    private static final String TABLE = "Users";
    private static final int TIMEOUT_SECONDS = 30;

    private final String url;

    public DB() {
        this(AppPaths.databaseUrl());
    }

    /** Allows tests to point at a temporary database. */
    public DB(String jdbcUrl) {
        this.url = jdbcUrl;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /** Creates the users table if it does not exist. */
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL UNIQUE, "
                + "password TEXT NOT NULL, "
                + "salt TEXT NOT NULL)";
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.setQueryTimeout(TIMEOUT_SECONDS);
            s.executeUpdate(sql);
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Could not create users table", ex);
        }
    }

    /**
     * Registers a new user.
     *
     * @return true if the account was created, false if the name is taken or invalid
     */
    public boolean addDataToDB(String user, String password) {
        if (user == null || user.isBlank() || password == null || password.isEmpty()) {
            return false;
        }
        String salt = PasswordHasher.newSalt();
        String sql = "INSERT INTO " + TABLE + " (name, password, salt) VALUES (?, ?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.trim());
            ps.setString(2, PasswordHasher.hash(password, salt));
            ps.setString(3, salt);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            // A UNIQUE constraint violation means the username already exists.
            LOG.log(Level.INFO, "Registration rejected for {0}: {1}", new Object[]{user, ex.getMessage()});
            return false;
        }
    }

    /** Lists usernames only; password hashes are never sent to the UI. */
    public ObservableList<User> getDataFromTable() {
        ObservableList<User> result = FXCollections.observableArrayList();
        String sql = "SELECT name FROM " + TABLE + " ORDER BY name";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new User(rs.getString("name"), ""));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Could not list users", ex);
        }
        return result;
    }

    /** Checks a username and password against the stored salted hash. */
    public boolean validateUser(String user, String pass) {
        String sql = "SELECT password, salt FROM " + TABLE + " WHERE name = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && PasswordHasher.verify(pass, rs.getString("salt"), rs.getString("password"));
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Could not validate user", ex);
            return false;
        }
    }

    public boolean deleteUser(String username) {
        String sql = "DELETE FROM " + TABLE + " WHERE name = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Could not delete user", ex);
            return false;
        }
    }

    /** Changes a password only if the old password is correct; the new hash gets a new salt. */
    public boolean updateUserPassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.isEmpty() || !validateUser(username, oldPassword)) {
            return false;
        }
        String salt = PasswordHasher.newSalt();
        String sql = "UPDATE " + TABLE + " SET password = ?, salt = ? WHERE name = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, PasswordHasher.hash(newPassword, salt));
            ps.setString(2, salt);
            ps.setString(3, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Could not update password", ex);
            return false;
        }
    }

    public String getTableName() {
        return TABLE;
    }

    public void log(String message) {
        LOG.info(message);
    }
}
