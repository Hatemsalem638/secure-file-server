package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Encrypts or decrypts any local file with AES-256-GCM (see {@link CryptoService}).
 * Encryption writes {@code name.enc} next to the original; decryption writes the
 * restored file without the {@code .enc} suffix. Originals are never overwritten.
 */
public class EncryptionController {

    private static final String SUFFIX = ".enc";

    @FXML
    private Label label;

    @FXML
    private TextField filename;

    @FXML
    private Button enc;

    @FXML
    private Button denc;

    @FXML
    private Button cancel;

    private File file;

    @FXML
    private void handleBrowse(ActionEvent event) {
        file = new FileChooser().showOpenDialog(new Stage());
        boolean selected = file != null;
        label.setText(selected ? "Selected file is: " + file.getName() : "No file selected");
        filename.setText(selected ? file.getPath() : "");
        enc.setDisable(!selected);
        denc.setDisable(!selected);
    }

    @FXML
    private void handleEncrypt(ActionEvent event) {
        char[] passphrase = DialogHelper.askPassphrase("Encrypt", "Choose a passphrase for " + file.getName());
        if (passphrase == null) {
            return;
        }
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            Path out = file.toPath().resolveSibling(file.getName() + SUFFIX);
            if (Files.exists(out)) {
                DialogHelper.showWarningDialog("Encrypt", out.getFileName() + " already exists.");
                return;
            }
            Files.write(out, CryptoService.encrypt(data, passphrase));
            DialogHelper.showInformationDialog("Encrypt", "Encrypted copy saved as " + out.getFileName());
        } catch (Exception e) {
            DialogHelper.showErrorDialog("Encrypt", "Encryption failed: " + e.getMessage());
        } finally {
            Arrays.fill(passphrase, '\0');
        }
    }

    @FXML
    private void handleDecrypt(ActionEvent event) {
        char[] passphrase = DialogHelper.askPassphrase("Decrypt", "Passphrase for " + file.getName());
        if (passphrase == null) {
            return;
        }
        try {
            byte[] plain = CryptoService.decrypt(Files.readAllBytes(file.toPath()), passphrase);
            String name = file.getName().endsWith(SUFFIX)
                    ? file.getName().substring(0, file.getName().length() - SUFFIX.length())
                    : file.getName() + ".decrypted";
            Path out = file.toPath().resolveSibling(name);
            if (Files.exists(out)) {
                out = file.toPath().resolveSibling("decrypted-" + name);
            }
            Files.write(out, plain);
            DialogHelper.showInformationDialog("Decrypt", "Decrypted file saved as " + out.getFileName());
        } catch (CryptoService.DecryptionException e) {
            DialogHelper.showErrorDialog("Decrypt", e.getMessage());
        } catch (IOException e) {
            DialogHelper.showErrorDialog("Decrypt", "Could not read or write the file: " + e.getMessage());
        } finally {
            Arrays.fill(passphrase, '\0');
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        ((Stage) cancel.getScene().getWindow()).close();
    }
}
