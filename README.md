# Secure File Server

A JavaFX desktop application for storing files securely. Users register and sign in, and every file they save is **encrypted with AES-256-GCM**, **split into chunks** on disk and **recorded against its owner**, so only that user can open or delete it.

Built in Java 11 with JavaFX, Maven and SQLite.

## Features

| Area | What it does |
|---|---|
| **Accounts** | Register, sign in, change password, delete your own account. Passwords are hashed with PBKDF2-HMAC-SHA256 (210,000 iterations) and a random salt per user, and compared in constant time. |
| **Encrypted storage** | Saving a file encrypts it with AES-256-GCM using a key derived from a passphrase (PBKDF2, random salt, random IV per file). GCM also detects a wrong passphrase or any tampering. |
| **Chunking** | The encrypted file is split into 4 chunks stored in the owner's directory, and joined back when the file is loaded. |
| **Access control** | File metadata (name, size, owner, created/modified time) is stored in SQLite. Load and delete check ownership first; other users cannot see, open or delete your files, and denied attempts are logged. |
| **Recycle bin** | Deleted chunks move to a recycle bin and are permanently removed after 31 days. |
| **Audit log** | Logins, failed logins, password changes, file operations and denied access are written to an audit log. |
| **Restricted terminal** | A built-in terminal that only runs allow-listed commands (`ls`, `pwd`, `whoami`, `mkdir`, `cp`, `mv`, `tree`, `ps`, `cat`) without a shell, inside the user's own storage directory, with a timeout. |
| **File encryption tool** | Encrypt or decrypt any local file to a separate `.enc` copy. |

## How a file is saved

```
Text / file content
   → AES-256-GCM encrypt (key from passphrase via PBKDF2, random salt + IV)
   → split into 4 chunks
   → chunks written to ~/SecureFileServer/storage/<owner>/
   → metadata + owner recorded in SQLite
```

Loading reverses this, after checking that the signed-in user owns the file.

## Security decisions

- **No string-built SQL.** Every query uses a `PreparedStatement`, so input such as `x'); DROP TABLE Users; --` is stored as plain text (covered by a test).
- **Hashes never reach the UI.** The user list shows names only.
- **Generic login errors.** The same message is shown for a wrong user name or a wrong password, so accounts cannot be probed.
- **No path traversal.** File and user names containing `/`, `\` or `..` are rejected.
- **Nothing machine-specific.** All data lives under `~/SecureFileServer/` (override with `-Dsfs.home=...`).

## Run it

Requirements: JDK 11 or later and Maven.

```bash
mvn clean javafx:run
```

## Tests

```bash
mvn test
```

22 JUnit 5 tests cover encryption round trips, wrong passphrases and tampering, password hashing, SQL-injection attempts, chunking, ownership checks, the recycle bin, path traversal and the terminal allow-list.

## Project structure

```
src/main/java/com/mycompany/javafxapplication1/
  CryptoService.java          AES-256-GCM encryption and key derivation
  PasswordHasher.java         Salted PBKDF2 password hashing
  FileStore.java              Encrypt → chunk → store pipeline with ownership checks
  FileChunkingController.java Split and join byte chunks
  DB.java                     User accounts (SQLite)
  DatabaseManager.java        File metadata and ownership (SQLite)
  RecycleBinManager.java      31-day recycle bin
  AuditTrailLogger.java       Audit log
  Terminal.java               Restricted terminal
  *Controller.java            JavaFX screens
src/test/java/...             JUnit 5 tests
```

## Background

Originally built for the COMP20081 Systems Software module at Nottingham Trent University, then hardened: the encryption, password storage, SQL handling, access control and terminal were rewritten, and tests were added.
