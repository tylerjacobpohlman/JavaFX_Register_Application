package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAOImpl;
import com.github.tylerjpohlman.database.register.helper_classes.*;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;


/**
 * {@code IntroductionController} - A controller class which extends {@code ControllerMethods}. Acts as the login layout
 * to enter credentials applicable to the accessing database. <p>
 *
 * Its use to grab the login information to initialize {@code jdbcUserDAO} within {@link BaseController}.
 * Upon successful initialization, the layout is changed to {@link MainController} and passes over {@code jdbcUserDAO}
 * along with executing method {@link MainController#setAddressLabel()} in {@link MainController}.
 *
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2023-11-24
 */
public class IntroductionController extends BaseController {
    /**
     * file name for the introduction view FXML file
     */
    public static final String introductionFXMLFile = "introduction-view.fxml";

    /**
     * Label used to display generated errors.
     */
    @FXML
    private Label errorLabel;

    /**
     * Text field used to input the url to the database.
     */
    @FXML
    private TextField urlTextField;

    /**
     * Text field used to input the associated username for logging into the database.
     */
    @FXML
    private TextField usernameTextField;

    /**
     * Text field used to input the associated password for logging into the database.
     */
    @FXML
    private TextField passwordTextField;

    /**
     * Text field used to input the register number. May be hard coded in the future for obvious reasons.
     */
    @FXML
    private TextField registerNumTextField;

    /**
     * Logic for clicking enter button in GUI.
     * @param event {@link ActionEvent} representing button click in GUI
     * @throws IOException if unable to read the associated FXML file
     */
    public void enterButtonOnClick(ActionEvent event) throws IOException {
        //resets error label
        errorLabel.setText("");

        //check if the text fields are empty
        if (urlTextField.getText().isEmpty() || usernameTextField.getText().isEmpty() ||
                passwordTextField.getText().isEmpty() || registerNumTextField.getText().isEmpty()) {
            errorLabel.setText("Error: One or more of the text fields are empty!");
            return;//exit the method
        }

        //grab information from all the text field
        String url = urlTextField.getText();
        String username = usernameTextField.getText();
        String password = passwordTextField.getText();
        int registerNum;

        try {
            registerNum = Integer.parseInt(registerNumTextField.getText());
        } catch(NumberFormatException e) {
            errorLabel.setText("Register id can only contain numeric values!");
            registerNumTextField.clear();
            return;
        }

        //add driver part to the url if it isn't empty
        url = "jdbc:mysql://" + url;

        try {
            jdbcUserDAO = new JdbcUserDAOImpl(url, username, password, registerNum);
        } catch (DriverNotFoundException e) {
            errorLabel.setText("Error: Driver for connecting to database not found. Please exit program");
            return;
        } catch (ServerConnectionException e) {
            errorLabel.setText("Error: Cannot access database! Try a different url or try again.");
            return;
        } catch (InvalidCredentialsException e) {
            errorLabel.setText("Error: Invalid Username or Password!");
            return;
        } catch (InvalidRegisterException e) {
            errorLabel.setText("Error: Invalid register number!");
            return;
        } catch (SQLException e) {
            errorLabel.setText("Unknown error: " + e.getMessage());
            return;
        }

        try {
            MainController mainController = goToNextWindow(MainController.mainFXMLFile, event, jdbcUserDAO);
            mainController.setJdbcUserDAO(jdbcUserDAO);//passes Connection
            mainController.setAddressLabel();//executes method defined in class
        }
        catch (ClosedConnectionException e) {
            errorLabel.setText("Connection has timed out, please try again...");
        }
    }
}
