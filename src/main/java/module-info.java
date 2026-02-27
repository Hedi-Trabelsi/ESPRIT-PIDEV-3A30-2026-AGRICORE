module org.example.agricorejava {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.web;
    requires jdk.jsobject;
    requires java.desktop;
    requires twilio;
    requires java.net.http;
    opens controllers to javafx.fxml, javafx.web;
    opens org.example.agricorejava to javafx.fxml, javafx.graphics;

    opens models to javafx.base;
    opens services;

    exports controllers;
    exports models;
    exports org.example.agricorejava;
}