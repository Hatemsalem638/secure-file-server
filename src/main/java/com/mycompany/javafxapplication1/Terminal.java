package com.mycompany.javafxapplication1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * A restricted terminal. Only allow-listed commands run, they are started
 * directly (no shell, so no pipes, redirection or command chaining), they run
 * inside the signed-in user's storage directory, and they time out.
 */
public class Terminal {

    static final Set<String> ALLOWED = Set.of("ls", "pwd", "whoami", "mkdir", "cp", "mv", "tree", "ps", "cat");
    private static final long TIMEOUT_SECONDS = 5;

    @FXML
    private TextArea outputTextArea;

    @FXML
    private TextField inputTextField;

    @FXML
    private Button exitterminal;

    private SecondaryController secondaryController;

    public void setSecondaryController(SecondaryController secondaryController) {
        this.secondaryController = secondaryController;
    }

    /** Splits a command line into arguments and rejects anything not allowed. */
    static List<String> parse(String commandLine) {
        List<String> args = new ArrayList<>(Arrays.asList(commandLine.trim().split("\\s+")));
        if (args.isEmpty() || args.get(0).isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }
        if (!ALLOWED.contains(args.get(0))) {
            throw new IllegalArgumentException("Command not allowed: " + args.get(0) + ". Allowed: " + ALLOWED);
        }
        for (String a : args) {
            if (a.contains("..") || a.startsWith("/") || a.startsWith("~")) {
                throw new IllegalArgumentException("Paths outside your storage area are not allowed.");
            }
        }
        return args;
    }

    private void executeCommand(String command) {
        outputTextArea.appendText("$ " + command + "\n");
        try {
            ProcessBuilder pb = new ProcessBuilder(parse(command))
                    .directory(AppPaths.storageFor(Session.currentUser()).toFile())
                    .redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                output.append("(command timed out)\n");
            }
            outputTextArea.appendText(output.toString());
            AuditTrailLogger.logEvent(Session.currentUser() + " ran: " + command);
            if (secondaryController != null) {
                secondaryController.handleCommand(command);
            }
        } catch (IllegalArgumentException e) {
            outputTextArea.appendText(e.getMessage() + "\n");
        } catch (IOException e) {
            outputTextArea.appendText("Error executing the command: " + e.getMessage() + "\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FXML
    public void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            executeCommand(inputTextField.getText());
            inputTextField.clear();
        }
    }

    @FXML
    private void exitAction() {
        ((Stage) outputTextArea.getScene().getWindow()).close();
    }
}
