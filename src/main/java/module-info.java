module com.mycompany.javafxapplication1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.logging;

    opens com.mycompany.javafxapplication1 to javafx.fxml, javafx.base;
    exports com.mycompany.javafxapplication1;
}
