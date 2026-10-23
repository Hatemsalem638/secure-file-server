package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deleted chunks are moved to a recycle bin and permanently removed after
 * {@link #RETENTION_DAYS} days by a single daemon scheduler.
 */
public final class RecycleBinManager {

    private static final Logger LOG = Logger.getLogger(RecycleBinManager.class.getName());
    static final int RETENTION_DAYS = 31;
    private static ScheduledExecutorService scheduler;

    private RecycleBinManager() {
    }

    public static void moveToRecycleBin(Path file, String owner) throws IOException {
        Path target = AppPaths.recycleBin().resolve(owner + "__" + Instant.now().toEpochMilli() + "__" + file.getFileName());
        FileStore.move(file, target);
        // Use the move time as the start of the retention period.
        Files.setLastModifiedTime(target, java.nio.file.attribute.FileTime.from(Instant.now()));
        LOG.log(Level.INFO, "Moved to recycle bin: {0}", target);
    }

    public static synchronized void setupPermanentDeletionScheduler() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "recycle-bin-cleaner");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(RecycleBinManager::purgeExpired, 0, 1, TimeUnit.DAYS);
    }

    static void purgeExpired() {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(AppPaths.recycleBin())) {
            for (Path f : files) {
                Instant moved = Files.getLastModifiedTime(f).toInstant();
                if (Duration.between(moved, Instant.now()).toDays() > RETENTION_DAYS) {
                    Files.deleteIfExists(f);
                    LOG.log(Level.INFO, "Permanently deleted: {0}", f);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Recycle bin cleanup failed", e);
        }
    }
}
