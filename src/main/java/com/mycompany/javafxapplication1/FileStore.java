package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The secure storage pipeline, kept free of UI code so it can be unit tested.
 *
 * <p>Save: plaintext → AES-256-GCM encrypt → split into chunks → write chunks
 * to the owner's storage directory → record metadata with the owner.<br>
 * Load: check ownership → read chunks → join → decrypt (GCM verifies integrity).<br>
 * Delete: check ownership → move chunks to the recycle bin → remove metadata.</p>
 */
public class FileStore {

    /** Thrown when a user tries to access a file they do not own. */
    public static class AccessDeniedException extends Exception {
        public AccessDeniedException(String message) {
            super(message);
        }
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DatabaseManager db;

    public FileStore(DatabaseManager db) {
        this.db = db;
    }

    public void save(String owner, String fileName, byte[] plaintext, char[] passphrase)
            throws IOException, SQLException, GeneralSecurityException {
        AppPaths.safeName(fileName);
        byte[] encrypted = CryptoService.encrypt(plaintext, passphrase);
        List<byte[]> chunks = FileChunkingController.split(encrypted);
        Path dir = AppPaths.storageFor(owner);
        for (int i = 0; i < chunks.size(); i++) {
            Files.write(chunkPath(dir, fileName, i + 1), chunks.get(i));
        }
        db.saveFile(fileName, plaintext.length, owner, LocalDateTime.now().format(TS));
    }

    public byte[] load(String user, String fileName, char[] passphrase)
            throws AccessDeniedException, IOException, CryptoService.DecryptionException {
        requireOwner(user, fileName);
        Path dir = AppPaths.storageFor(user);
        List<byte[]> chunks = new ArrayList<>();
        for (int i = 1; i <= FileChunkingController.getNumChunks(); i++) {
            Path p = chunkPath(dir, fileName, i);
            if (!Files.exists(p)) {
                throw new IOException("Chunk " + i + " of " + fileName + " is missing.");
            }
            chunks.add(Files.readAllBytes(p));
        }
        return CryptoService.decrypt(FileChunkingController.join(chunks), passphrase);
    }

    public void delete(String user, String fileName) throws AccessDeniedException, IOException, SQLException {
        requireOwner(user, fileName);
        Path dir = AppPaths.storageFor(user);
        for (int i = 1; i <= FileChunkingController.getNumChunks(); i++) {
            Path p = chunkPath(dir, fileName, i);
            if (Files.exists(p)) {
                RecycleBinManager.moveToRecycleBin(p, user);
            }
        }
        db.deleteFile(fileName, user);
    }

    public List<String> listFiles(String owner) {
        List<String> names = new ArrayList<>();
        db.getFilesFor(owner).forEach(m -> names.add(m.getName()));
        return names;
    }

    private void requireOwner(String user, String fileName) throws AccessDeniedException {
        AppPaths.safeName(fileName);
        if (!db.isOwner(fileName, user)) {
            AuditTrailLogger.logEvent("ACCESS DENIED: " + user + " tried to access " + fileName);
            throw new AccessDeniedException("You do not have access to " + fileName + ".");
        }
    }

    private static Path chunkPath(Path dir, String fileName, int n) {
        return dir.resolve(fileName + ".part" + n);
    }

    /** Copies a file within the recycle bin helper without overwriting user data elsewhere. */
    static void move(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
}
