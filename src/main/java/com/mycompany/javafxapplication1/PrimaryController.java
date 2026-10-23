package com.mycompany.javafxapplication1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/** Login screen. */
public class PrimaryController {

    @FXML
    private Button registerBtn;

    @FXML
    private TextField userTextField;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        open("register.fxml", "Register a new User", null);
    }

    @FXML
    private void switchToSecondary() {
        String user = userTextField.getText() == null ? "" : userTextField.getText().trim();
        if (new DB().validateUser(user, passPasswordField.getText())) {
            Session.signIn(user);
            AuditTrailLogger.logEvent("Login succeeded: " + user);
            open("secondary.fxml", "Dashboard", user);
        } else {
            // Same message whether the user or the password is wrong, so usernames cannot be probed.
            AuditTrailLogger.logEvent("Login failed: " + user);
            passPasswordField.clear();
            DialogHelper.showErrorDialog("Login", "Invalid user name or password. Please try again.");
        }
    }

    private void open(String fxml, String title, String user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if (user != null) {
                SecondaryController controller = loader.getController();
                controller.initialise(new String[]{user});
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle(title);
            stage.show();
            ((Stage) registerBtn.getScene().getWindow()).close();
        } catch (Exception e) {
            DialogHelper.showErrorDialog("Navigation", "Could not open " + title + ".");
        }
    }
}
