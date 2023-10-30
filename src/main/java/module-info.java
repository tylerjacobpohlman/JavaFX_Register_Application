module com.github.tylerjpohlman.database.register.register_application {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.github.tylerjpohlman.database.register.controller_classes to javafx.fxml;
    exports com.github.tylerjpohlman.database.register.controller_classes;

    opens com.github.tylerjpohlman.database.register to javafx.fxml;
    exports com.github.tylerjpohlman.database.register;


}