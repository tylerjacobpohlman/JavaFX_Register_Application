package com.github.tylerjpohlman.database.register;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        //had to manually path to fxml file b/c resources file is set under 'controller_classes' package
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/github/tylerjpohlman/database/register/controller_classes/introduction-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 600, 400);

        stage.setTitle("Register Application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}