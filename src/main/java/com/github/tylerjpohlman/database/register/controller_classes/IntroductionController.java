package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAOImpl;
import com.github.tylerjpohlman.database.register.helper_classes.*;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
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

        //used to access SQL methods
        JdbcUserDAOImpl jdbcUserDAOImpl = new JdbcUserDAOImpl();

        try {
            connection = jdbcUserDAOImpl.getConnectionFromLogin(url, username, password, Integer.parseInt(registerNum));
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
            errorLabel.setText("Error: " + e.getMessage());
            return;
        }

        try {
            MainController mainController = (MainController)goToNextWindow(mainFXMLFile, event);
            mainController.setConnection(connection);//passes Connection object
            mainController.setRegisterNumber(Integer.parseInt(registerNum));
            mainController.setAddressLabel();//executes method defined in class
        }
        catch (ClosedConnectionException e) {
            errorLabel.setText("Connection has timed out, please try again...");
        }
        catch (SQLException e ) {
            errorLabel.setText(e.getMessage());
        }
    }
}
