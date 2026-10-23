package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** Registration screen. */
public class RegisterController {

    static final int MIN_PASSWORD_LENGTH = 8;

    @FXML
    private Button registerBtn;

    @FXML
    private Button backLoginBtn;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private PasswordField rePassPasswordField;

    @FXML
    private TextField userTextField;

    @FXML
    private Text fileText;

    @FXML
    private Button selectBtn;

    @FXML
    private void selectBtnHandler(ActionEvent event) throws IOException {
        File selectedFile = new FileChooser().showOpenDialog(selectBtn.getScene().getWindow());
        if (selectedFile != null) {
            fileText.setText(selectedFile.getCanonicalPath());
        }
    }

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        String user = userTextField.getText() == null ? "" : userTextField.getText().trim();
        String pass = passPasswordField.getText();

        if (user.isEmpty()) {
            DialogHelper.showWarningDialog("Register", "Please enter a user name.");
            return;
        }
        if (pass.length() < MIN_PASSWORD_LENGTH) {
            DialogHelper.showWarningDialog("Register", "Passwords must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            return;
        }
        if (!pass.equals(rePassPasswordField.getText())) {
            DialogHelper.showWarningDialog("Register", "The two passwords do not match.");
            return;
        }
        if (!new DB().addDataToDB(user, pass)) {
            DialogHelper.showErrorDialog("Register", "That user name is already taken.");
            return;
        }
        AuditTrailLogger.logEvent("User registered: " + user);
        Session.signIn(user);
        DialogHelper.showInformationDialog("Register", "Account created.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.initialise(new String[]{user});
            show(root, "Dashboard");
        } catch (IOException e) {
            DialogHelper.showErrorDialog("Register", "Could not open the dashboard.");
        }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        try {
            show(FXMLLoader.load(getClass().getResource("primary.fxml")), "Login");
        } catch (IOException e) {
            DialogHelper.showErrorDialog("Navigation", "Could not open the login page.");
        }
    }

    private void show(Parent root, String title) {
        Stage stage = new Stage();
        stage.setScene(new Scene(root, 640, 480));
        stage.setTitle(title);
        stage.show();
        ((Stage) registerBtn.getScene().getWindow()).close();
    }
}
