package com.mycompany.javafxapplication1;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/** Appends security-relevant events (logins, failed logins, access denials) to an audit log. */
public final class AuditTrailLogger {

    private AuditTrailLogger() {
    }

    public static synchronized void logEvent(String event) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(AppPaths.auditLog().toFile(), true))) {
            writer.println(LocalDateTime.now() + " - " + event);
        } catch (IOException e) {
            System.err.println("Error writing to audit trail: " + e.getMessage());
        }
    }
}
