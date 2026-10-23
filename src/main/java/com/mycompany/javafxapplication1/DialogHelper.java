package com.mycompany.javafxapplication1;

import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public final class DialogHelper {

    private DialogHelper() {
    }

    public static void showInformationDialog(String title, String content) {
        showAlert(title, content, AlertType.INFORMATION);
    }

    public static void showWarningDialog(String title, String content) {
        showAlert(title, content, AlertType.WARNING);
    }

    public static void showErrorDialog(String title, String content) {
        showAlert(title, content, AlertType.ERROR);
    }

    /** Asks for a passphrase in a masked field. Returns null if cancelled or empty. */
    public static char[] askPassphrase(String title, String header) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        PasswordField field = new PasswordField();
        field.setPromptText("Passphrase");
        dialog.getDialogPane().setContent(new VBox(8, field));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == ButtonType.OK ? field.getText() : null);
        Optional<String> result = dialog.showAndWait();
        return result.filter(s -> !s.isEmpty()).map(String::toCharArray).orElse(null);
    }

    /** Lets the user pick one of their files. Returns null if cancelled. */
    public static String chooseFile(String title, List<String> files) {
        if (files.isEmpty()) {
            showInformationDialog(title, "You have no stored files yet.");
            return null;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(files.get(0), files);
        dialog.setTitle(title);
        dialog.setHeaderText("Choose one of your files");
        return dialog.showAndWait().orElse(null);
    }

    private static void showAlert(String title, String content, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
