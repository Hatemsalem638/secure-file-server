package com.mycompany.javafxapplication1;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Entry point: prepares storage and the database, then shows the login screen. */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        new DB().createTable();
        RecycleBinManager.setupPermanentDeletionScheduler();

        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("primary.fxml")), 640, 480));
        stage.setTitle("Secure File Server — Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
