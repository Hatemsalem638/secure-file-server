package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central place for every location the application writes to, so nothing is
 * hard-coded to one machine. Defaults to ~/SecureFileServer and can be
 * overridden with the {@code sfs.home} system property (used by the tests).
 */
public final class AppPaths {

    private AppPaths() {
    }

    public static Path home() {
        String override = System.getProperty("sfs.home");
        Path base = override != null ? Paths.get(override) : Paths.get(System.getProperty("user.home"), "SecureFileServer");
        return ensure(base);
    }

    public static String databaseUrl() {
        return "jdbc:sqlite:" + home().resolve("sfs.db");
    }

    /** Each user's chunks live in their own directory. */
    public static Path storageFor(String owner) {
        return ensure(home().resolve("storage").resolve(safeName(owner)));
    }

    public static Path recycleBin() {
        return ensure(home().resolve("recycle-bin"));
    }

    public static Path auditLog() {
        return home().resolve("audit.log");
    }

    /** Rejects path separators and traversal so a name can never escape its directory. */
    public static String safeName(String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")
                || name.equals(".") || name.equals("..") || name.contains("\0")) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        return name;
    }

    private static Path ensure(Path dir) {
        try {
            return Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create directory " + dir, e);
        }
    }
}
