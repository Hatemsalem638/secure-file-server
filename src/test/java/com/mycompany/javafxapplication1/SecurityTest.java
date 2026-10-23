package com.mycompany.javafxapplication1;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for accounts, chunking, ownership checks and the restricted terminal. */
class SecurityTest {

    @TempDir
    Path tmp;

    private DB users;

    @BeforeEach
    void setUp() {
        System.setProperty("sfs.home", tmp.toString());
        users = new DB(AppPaths.databaseUrl());
        users.createTable();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("sfs.home");
    }

    // ---- accounts ----

    @Test
    void registeredUserCanLogInOnlyWithCorrectPassword() {
        assertTrue(users.addDataToDB("alice", "s3cure-pass"));
        assertTrue(users.validateUser("alice", "s3cure-pass"));
        assertFalse(users.validateUser("alice", "wrong-pass"));
        assertFalse(users.validateUser("nobody", "s3cure-pass"));
    }

    @Test
    void duplicateUsernamesAreRejected() {
        assertTrue(users.addDataToDB("alice", "password-1"));
        assertFalse(users.addDataToDB("alice", "password-2"));
    }

    @Test
    void sqlInjectionInUsernameIsStoredAsPlainText() {
        String evil = "x'); DROP TABLE Users; --";
        assertTrue(users.addDataToDB(evil, "password-1"));
        assertTrue(users.validateUser(evil, "password-1"), "table must still exist and hold the literal name");
        assertTrue(users.addDataToDB("bob", "password-2"));
    }

    @Test
    void injectionInLoginDoesNotBypassPassword() {
        users.addDataToDB("alice", "s3cure-pass");
        assertFalse(users.validateUser("alice' OR '1'='1", "anything"));
    }

    @Test
    void sameePasswordProducesDifferentHashesPerUser() {
        String a = PasswordHasher.hash("same-password", PasswordHasher.newSalt());
        String b = PasswordHasher.hash("same-password", PasswordHasher.newSalt());
        assertNotEquals(a, b);
    }

    @Test
    void passwordChangeRequiresOldPassword() {
        users.addDataToDB("alice", "old-password");
        assertFalse(users.updateUserPassword("alice", "not-the-old-one", "new-password"));
        assertTrue(users.updateUserPassword("alice", "old-password", "new-password"));
        assertTrue(users.validateUser("alice", "new-password"));
        assertFalse(users.validateUser("alice", "old-password"));
    }

    @Test
    void userListNeverExposesPasswordHashes() {
        users.addDataToDB("alice", "s3cure-pass");
        users.getDataFromTable().forEach(u -> assertEquals("", u.getPass()));
    }

    // ---- chunking ----

    @Test
    void chunksJoinBackToOriginal() {
        byte[] data = new byte[1003];
        new java.util.Random(1).nextBytes(data);
        List<byte[]> parts = FileChunkingController.split(data);
        assertEquals(FileChunkingController.getNumChunks(), parts.size());
        assertArrayEquals(data, FileChunkingController.join(parts));
    }

    @Test
    void tinyContentDoesNotCrashChunking() {
        // The original version produced a negative array size here.
        byte[] data = {1, 2};
        assertArrayEquals(data, FileChunkingController.join(FileChunkingController.split(data)));
    }

    // ---- secure storage + ownership ----

    @Test
    void ownerCanSaveAndLoadEncryptedChunks() throws Exception {
        try (DatabaseManager db = new DatabaseManager(AppPaths.databaseUrl())) {
            FileStore store = new FileStore(db);
            byte[] content = "meeting notes".getBytes(StandardCharsets.UTF_8);
            store.save("alice", "notes.txt", content, "pass-phrase".toCharArray());

            Path part1 = AppPaths.storageFor("alice").resolve("notes.txt.part1");
            assertTrue(Files.exists(part1));
            String onDisk = new String(Files.readAllBytes(part1), StandardCharsets.ISO_8859_1);
            assertFalse(onDisk.contains("meeting"), "chunks on disk must be encrypted");

            assertArrayEquals(content, store.load("alice", "notes.txt", "pass-phrase".toCharArray()));
            assertEquals(List.of("notes.txt"), store.listFiles("alice"));
        }
    }

    @Test
    void otherUsersCannotLoadOrDeleteAFile() throws Exception {
        try (DatabaseManager db = new DatabaseManager(AppPaths.databaseUrl())) {
            FileStore store = new FileStore(db);
            store.save("alice", "private.txt", "x".getBytes(StandardCharsets.UTF_8), "pass-phrase".toCharArray());

            assertThrows(FileStore.AccessDeniedException.class,
                    () -> store.load("mallory", "private.txt", "pass-phrase".toCharArray()));
            assertThrows(FileStore.AccessDeniedException.class, () -> store.delete("mallory", "private.txt"));
            assertTrue(store.listFiles("mallory").isEmpty());
        }
    }

    @Test
    void deleteMovesChunksToRecycleBin() throws Exception {
        try (DatabaseManager db = new DatabaseManager(AppPaths.databaseUrl())) {
            FileStore store = new FileStore(db);
            store.save("alice", "old.txt", "bye".getBytes(StandardCharsets.UTF_8), "pass-phrase".toCharArray());
            store.delete("alice", "old.txt");
            assertFalse(Files.exists(AppPaths.storageFor("alice").resolve("old.txt.part1")));
            try (var s = Files.list(AppPaths.recycleBin())) {
                assertEquals(FileChunkingController.getNumChunks(), s.count());
            }
            assertTrue(store.listFiles("alice").isEmpty());
        }
    }

    @Test
    void pathTraversalInFileNamesIsRejected() throws Exception {
        try (DatabaseManager db = new DatabaseManager(AppPaths.databaseUrl())) {
            FileStore store = new FileStore(db);
            assertThrows(IllegalArgumentException.class, () -> store.save("alice", "../escape.txt",
                    new byte[]{1}, "pass-phrase".toCharArray()));
        }
    }

    // ---- terminal ----

    @Test
    void terminalAllowsOnlyListedCommands() {
        assertEquals(List.of("ls", "-l"), Terminal.parse("ls -l"));
        assertThrows(IllegalArgumentException.class, () -> Terminal.parse("rm -rf storage"));
        assertThrows(IllegalArgumentException.class, () -> Terminal.parse("cat ../../sfs.db"));
        assertThrows(IllegalArgumentException.class, () -> Terminal.parse("cat /etc/passwd"));
    }
}
