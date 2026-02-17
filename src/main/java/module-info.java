module org.example.agricorejava {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens controllers to javafx.fxml;
    opens models to javafx.fxml;
    exports controllers;
    exports models;
    opens services to javafx.base;
    opens ui to javafx.graphics, javafx.fxml;
    opens org.example.agricorejava to javafx.fxml;
    exports org.example.agricorejava;

}
