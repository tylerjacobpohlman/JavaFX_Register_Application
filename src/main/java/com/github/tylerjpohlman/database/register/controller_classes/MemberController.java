package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAOImpl;
import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;
import com.github.tylerjpohlman.database.register.helper_classes.Member;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;


public class MemberController extends ControllerMethods{
    @FXML
    private Label errorLabel;

    @FXML
    private TextField memberIDTextField;

    @FXML
    private TextField phoneNumberTextField;

    /**
     * Logic when clicking enter button in GUI.
     * @param event ActionEvent representing button click
     */
    public void enterButtonOnClick(ActionEvent event) {
        //reset the error label
        errorLabel.setText("");

        //used to access SQL statements
        JdbcUserDAOImpl jdbcUserDAOImpl = new JdbcUserDAOImpl();
        try {
            //if there isn't any entered text
            if (phoneNumberTextField.getText().isEmpty() && memberIDTextField.getText().isEmpty()) {
                errorLabel.setText("Please enter either a member number or phone number.");
                return;//exit method
            }
            //if there's a phone number provided
            else if (!phoneNumberTextField.getText().isEmpty()) {
                //grabs the phone number
                //also removes all the misc. chars when someone types in a phone number and just keeps the digits
                long phoneNumber = Long.parseLong(phoneNumberTextField.getText().replaceAll("[^0-9]", ""));
                member = jdbcUserDAOImpl.getMemberFromPhoneNumber(connection, phoneNumber);
            }
            //if only the account number is provided
            else {
                //grabs the account number
                long accountNumber = Long.parseLong(memberIDTextField.getText().replaceAll("[^0-9]", ""));
                member = jdbcUserDAOImpl.getMemberFromAccountNumber(connection, accountNumber);
            }
        } catch (SQLException e) {
            errorLabel.setText("Unable to find membership with provided phone number / member id.");

            //resets the text fields
            phoneNumberTextField.setText("");
            memberIDTextField.setText("");
            return;
        }

        try {
            MainController mainController = (MainController)goToNextWindow(mainFXMLFile, event);
            mainController.setMemberLabel(member);
            mainController.setConnection(connection);
            mainController.setMember(member);

        } catch (ClosedConnectionException | IOException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel,event);
        }
    }

    /**
     * Logic when clicking 'Go Back' Button in GUI.
     * @param event ActionEvent represent by button click in GUI
     * @throws IOException if unable to read FXML file
     */
    public void goBackOnClick(ActionEvent event) throws IOException {
        try {
            goToNextWindow(mainFXMLFile, event);
        } catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
