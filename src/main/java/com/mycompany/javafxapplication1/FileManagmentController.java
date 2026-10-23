package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * File management screen. All storage goes through {@link FileStore}, which
 * encrypts, chunks and records ownership; this class only handles the UI.
 */
public class FileManagmentController {

    @FXML
    private TextField fileNameTextField;

    @FXML
    private TextArea fileContentTextArea;

    @FXML
    private void saveFile() {
        String name = fileNameTextField.getText() == null ? "" : fileNameTextField.getText().trim();
        if (name.isEmpty()) {
            DialogHelper.showWarningDialog("Save File", "Please enter a file name.");
            return;
        }
        char[] passphrase = DialogHelper.askPassphrase("Save File", "Passphrase used to encrypt \"" + name + "\"");
        if (passphrase == null) {
            return;
        }
        try (DatabaseManager db = new DatabaseManager()) {
            byte[] content = fileContentTextArea.getText().getBytes(StandardCharsets.UTF_8);
            new FileStore(db).save(Session.currentUser(), name, content, passphrase);
            AuditTrailLogger.logEvent(Session.currentUser() + " saved " + name);
            DialogHelper.showInformationDialog("Save File", "\"" + name + "\" was encrypted, split into "
                    + FileChunkingController.getNumChunks() + " chunks and saved.");
        } catch (IllegalArgumentException e) {
            DialogHelper.showErrorDialog("Save File", e.getMessage());
        } catch (Exception e) {
            DialogHelper.showErrorDialog("Save File", "Could not save the file: " + e.getMessage());
        } finally {
            Arrays.fill(passphrase, '\0');
        }
    }

    @FXML
    private void loadFile() {
        try (DatabaseManager db = new DatabaseManager()) {
            FileStore store = new FileStore(db);
            String name = DialogHelper.chooseFile("Load File", store.listFiles(Session.currentUser()));
            if (name == null) {
                return;
            }
            char[] passphrase = DialogHelper.askPassphrase("Load File", "Passphrase for \"" + name + "\"");
            if (passphrase == null) {
                return;
            }
            try {
                byte[] content = store.load(Session.currentUser(), name, passphrase);
                fileNameTextField.setText(name);
                fileContentTextArea.setText(new String(content, StandardCharsets.UTF_8));
            } finally {
                Arrays.fill(passphrase, '\0');
            }
        } catch (CryptoService.DecryptionException | FileStore.AccessDeniedException e) {
            DialogHelper.showErrorDialog("Load File", e.getMessage());
        } catch (SQLException | IOException e) {
            DialogHelper.showErrorDialog("Load File", "Could not load the file: " + e.getMessage());
        }
    }

    @FXML
    private void deleteFileHandler(ActionEvent event) {
        try (DatabaseManager db = new DatabaseManager()) {
            FileStore store = new FileStore(db);
            String name = DialogHelper.chooseFile("Delete File", store.listFiles(Session.currentUser()));
            if (name == null) {
                return;
            }
            store.delete(Session.currentUser(), name);
            AuditTrailLogger.logEvent(Session.currentUser() + " deleted " + name);
            fileNameTextField.clear();
            fileContentTextArea.clear();
            DialogHelper.showInformationDialog("Delete File", "\"" + name + "\" was moved to the recycle bin ("
                    + RecycleBinManager.RETENTION_DAYS + " days before permanent deletion).");
        } catch (FileStore.AccessDeniedException e) {
            DialogHelper.showErrorDialog("Delete File", e.getMessage());
        } catch (SQLException | IOException e) {
            DialogHelper.showErrorDialog("Delete File", "Could not delete the file: " + e.getMessage());
        }
    }

    @FXML
    private void goBackHandler(ActionEvent event) {
        openScene(event, "secondary.fxml", "Dashboard", true);
    }

    @FXML
    private void goToEncyptionPage(ActionEvent event) {
        openScene(event, "Encryption.fxml", "Encrypt / Decrypt a local file", false);
    }

    private void openScene(ActionEvent event, String fxml, String title, boolean dashboard) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if (dashboard) {
                SecondaryController controller = loader.getController();
                controller.initialise(new String[]{Session.currentUser()});
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
            ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
        } catch (IOException e) {
            DialogHelper.showErrorDialog("Navigation", "Could not open " + title + ".");
        }
    }
}
