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
 * File metadata (name, size, owner, timestamps) stored in the same SQLite
 * database as the user accounts. Ownership is recorded for every file and
 * checked before a file can be opened or deleted.
 */
public class DatabaseManager implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    private final Connection connection;

    public DatabaseManager() throws SQLException {
        this(AppPaths.databaseUrl());
    }

    public DatabaseManager(String jdbcUrl) throws SQLException {
        connection = DriverManager.getConnection(jdbcUrl);
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS MetaData ("
                    + "Name TEXT NOT NULL, "
                    + "Size INTEGER NOT NULL, "
                    + "Owner TEXT NOT NULL, "
                    + "Modification TEXT NOT NULL, "
                    + "Creation TEXT NOT NULL, "
                    + "PRIMARY KEY (Owner, Name))");
        }
    }

    /** Inserts a new file record or updates size and modification time if it already exists. */
    public void saveFile(String fileName, long fileSize, String owner, String timestamp) throws SQLException {
        String sql = "INSERT INTO MetaData (Name, Size, Owner, Modification, Creation) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(Owner, Name) DO UPDATE SET Size = excluded.Size, Modification = excluded.Modification";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fileName);
            ps.setLong(2, fileSize);
            ps.setString(3, owner);
            ps.setString(4, timestamp);
            ps.setString(5, timestamp);
            ps.executeUpdate();
        }
    }

    /** True only if the given user owns the named file. */
    public boolean isOwner(String fileName, String user) {
        String sql = "SELECT 1 FROM MetaData WHERE Name = ? AND Owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fileName);
            ps.setString(2, user);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Ownership check failed", e);
            return false;
        }
    }

    public boolean deleteFile(String fileName, String owner) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM MetaData WHERE Name = ? AND Owner = ?")) {
            ps.setString(1, fileName);
            ps.setString(2, owner);
            return ps.executeUpdate() > 0;
        }
    }

    /** Lists only the files belonging to one user. */
    public ObservableList<MetaData> getFilesFor(String owner) {
        ObservableList<MetaData> data = FXCollections.observableArrayList();
        String sql = "SELECT * FROM MetaData WHERE Owner = ? ORDER BY Name";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new MetaData(rs.getString("Name"), rs.getInt("Size"), rs.getString("Owner"),
                            rs.getString("Modification"), rs.getString("Creation")));
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Could not list files", e);
        }
        return data;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Could not close database", e);
        }
    }
}
