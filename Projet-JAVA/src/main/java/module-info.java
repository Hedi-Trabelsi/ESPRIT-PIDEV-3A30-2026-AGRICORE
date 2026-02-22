module org.example.agricorejava {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.web;

    // --- ADD THESE TWO LINES ---
    requires jdk.jsobject;
    // ---------------------------

    // JavaFX packages
    opens controllers to javafx.fxml;
    opens ui to javafx.graphics, javafx.fxml;

    // Model & Service packages — allow reflection for JavaFX and tests
    opens models;
    opens services;

    // Exports for other packages
    exports controllers;
    exports models;
    exports org.example.agricorejava;
}