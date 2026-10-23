package com.mycompany.javafxapplication1;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/** Lets the signed-in user change their own password. */
public class UpdateUserController {

    @FXML
    private Button updateuserButton;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField oldPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        if (Session.isSignedIn()) {
            // Users can only change their own password.
            usernameField.setText(Session.currentUser());
            usernameField.setEditable(false);
        }
    }

    @FXML
    private void handleEnterKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            updateUserPassword();
        }
    }

    @FXML
    private void updateUserPassword() {
        String newPassword = newPasswordField.getText();
        if (newPassword.length() < RegisterController.MIN_PASSWORD_LENGTH) {
            DialogHelper.showWarningDialog("Update Password",
                    "New passwords must be at least " + RegisterController.MIN_PASSWORD_LENGTH + " characters.");
            return;
        }
        String user = usernameField.getText();
        if (new DB().updateUserPassword(user, oldPasswordField.getText(), newPassword)) {
            AuditTrailLogger.logEvent("Password changed: " + user);
            DialogHelper.showInformationDialog("Update Password", "Password updated successfully.");
            ((Stage) updateuserButton.getScene().getWindow()).close();
        } else {
            AuditTrailLogger.logEvent("Password change failed: " + user);
            DialogHelper.showErrorDialog("Update Password", "Your current password is incorrect.");
        }
    }
}
