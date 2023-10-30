package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;



public class IntroductionController extends ControllerMethods {

    @FXML
    private Label errorLabel;

    @FXML
    private TextField urlTextField;

    @FXML
    private TextField usernameTextField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private TextField registerNumTextField;

    /**
     * Logic for clicking enter button in GUI.
     * @param event ActionEvent representing button click in GUI
     * @throws IOException if unable to read associated FXML file
     */
    @FXML
    public void enterButtonOnClick(ActionEvent event) throws IOException {
        //resets error label
        errorLabel.setText("");

        //grab information from all the text field
        String url = urlTextField.getText();
        String username = usernameTextField.getText();
        String password = passwordTextField.getText();
        String registerNum = registerNumTextField.getText();

        //check if the text fields are empty
        if (url.isEmpty() || username.isEmpty() || password.isEmpty() || registerNum.isEmpty()) {
            errorLabel.setText("Error: One or more of the text fields are empty!");
            return;//exit the method
        }

        //add driver part to url if it isn't empty
        url = "jdbc:mysql://" + url;

        //try logging in using credentials
        try {
            //tries to establish a connection to the database
            connection = DriverManager.getConnection(url, username, password);

            //tries cashier login procedure
            PreparedStatement ps = connection.prepareStatement("CALL cashierRegisterLogin(?, ?)");
            ps.setString(1, username);
            ps.setString(2, registerNum);
            ps.execute();

        } catch (SQLException e) {
            String errorCode = e.getSQLState();
            switch (errorCode) {
                //if the driver isn't downloaded or defined in the url
                case "08001":
                    errorLabel.setText("Error: Driver for connecting to database not found. Please exit program");
                    break;
                //if there's issue contacting the server, a database isn't selected, or the server cannot be found at all
                case "08S01", "3D000":
                case null:
                    errorLabel.setText("Error: Cannot access database! Try a different url or try again.");
                    break;
                //if either the username and/or password is incorrect
                case "28000":
                    errorLabel.setText("Error: Invalid Username or Password!");
                    break;
                //error defined in database: "no such register_id and/or cashier_id exists"
                //basically, this error is invoked when an invalid register number is given
                case "45000":
                    errorLabel.setText("Error: Invalid register number!");
                    break;
                default:
                    errorLabel.setText("Error: " + errorCode);
                    break;
            }
        }

        try {
            MainController mainController = (MainController)goToNextWindow(mainFXMLFile, event);
            mainController.setConnection(connection);//passes Connection object
            mainController.setRegisterNum(registerNum);//passes String object
            mainController.setAddressLabel();//executes method defined in class
        }
        catch (ClosedConnectionException e) {
            errorLabel.setText("Connection has timed out, please try again...");
        }
    }
}
