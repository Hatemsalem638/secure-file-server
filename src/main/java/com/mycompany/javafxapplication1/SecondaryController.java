package com.mycompany.javafxapplication1;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/** Dashboard shown after login. */
public class SecondaryController {

    @FXML
    private TextField userTextField;

    @FXML
    private TableView<User> dataTableView;

    @FXML
    private TextField customTextField;

    @FXML
    private Label outputLabel;

    private final DB db = new DB();

    public void initialise(String[] credentials) {
        userTextField.setText(credentials[0]);
        userTextField.setEditable(false);
        // Only usernames are shown; password hashes never leave the database layer.
        TableColumn<User, String> user = new TableColumn<>("User");
        user.setCellValueFactory(new PropertyValueFactory<>("user"));
        dataTableView.getColumns().setAll(user);
        refreshTable();
    }

    @FXML
    private void refreshBtnHandler(ActionEvent event) {
        refreshTable();
    }

    private void refreshTable() {
        dataTableView.setItems(db.getDataFromTable());
    }

    /** Users may delete only their own account, after confirming. */
    @FXML
    private void deleteUser(ActionEvent event) {
        String me = Session.currentUser();
        User selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.getUser().equals(me)) {
            DialogHelper.showErrorDialog("Delete User", "You can only delete your own account.");
            AuditTrailLogger.logEvent("ACCESS DENIED: " + me + " tried to delete " + selected.getUser());
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete the account \"" + me + "\"?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES && db.deleteUser(me)) {
            AuditTrailLogger.logEvent("Account deleted: " + me);
            switchToPrimary(event);
        }
    }

    public void handleCommand(String command) {
        if (outputLabel != null) {
            outputLabel.setText("Command received: " + command);
        }
    }

    @FXML
    private void switchToPrimary(ActionEvent event) {
        AuditTrailLogger.logEvent("Logout: " + (Session.isSignedIn() ? Session.currentUser() : "?"));
        Session.signOut();
        open(event, "primary.fxml", "Login", 640, 480);
    }

    @FXML
    private void goToFileManagementPage(ActionEvent event) {
        open(event, "fileManagement.fxml", "File Management", -1, -1);
    }

    @FXML
    private void redirectToTerminal(ActionEvent event) {
        open(event, "Terminal.fxml", "Terminal", 600, 400);
    }

    @FXML
    private void updateUserPassword(ActionEvent event) {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("UpdateUser.fxml"))));
            stage.setTitle("Change Password");
            stage.show();
        } catch (IOException e) {
            DialogHelper.showErrorDialog("Navigation", "Could not open the password page.");
        }
    }

    private void open(ActionEvent event, String fxml, String title, double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if (loader.getController() instanceof Terminal) {
                ((Terminal) loader.getController()).setSecondaryController(this);
            }
            Stage stage = new Stage();
            stage.setScene(w > 0 ? new Scene(root, w, h) : new Scene(root));
            stage.setTitle(title);
            stage.show();
            ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
        } catch (IOException e) {
            DialogHelper.showErrorDialog("Navigation", "Could not open " + title + ".");
        }
    }
}
