package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;
import com.github.tylerjpohlman.database.register.helper_classes.Member;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller class for the member lookup view, which tries to find an associated {@link Member} from the database. <p>
 * Most notably, {@link #enterButtonOnClick} adds the member to {@link MainController} upon success while
 * {@link #goBackOnClick} returns to {@link MainController}.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2023-11-24
 */
public class MemberController extends BaseController{
    @FXML
    private Label errorLabel;

    @FXML
    private TextField memberIDTextField;

    @FXML
    private TextField phoneNumberTextField;

    /**
     * Logic when clicking enter button in GUI.
     * @param event {@link ActionEvent} object representing button click
     */
    public void enterButtonOnClick(ActionEvent event) {
        //reset the error label
        errorLabel.setText("");

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
                long phoneNumber =
                        Long.parseLong(phoneNumberTextField.getText().replaceAll("[^0-9]", ""));
                member = jdbcUserDAO.getMemberFromPhoneNumber(phoneNumber);
            }
            //if only the account number is provided
            else {
                //grabs the account number
                long accountNumber =
                        Long.parseLong(memberIDTextField.getText().replaceAll("[^0-9]", ""));
                member = jdbcUserDAO.getMemberFromAccountNumber(accountNumber);
            }
        } catch (SQLException e) {
            errorLabel.setText("Unable to find membership with provided phone number / member id.");

            //resets the text fields
            phoneNumberTextField.setText("");
            memberIDTextField.setText("");
            return;
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid phone number / member id.");

            //resets the text fields
            phoneNumberTextField.setText("");
            memberIDTextField.setText("");
            return;
        }

        try {
            MainController mainController = goToNextWindow(mainFXMLFile, event, jdbcUserDAO);
            mainController.setJdbcUserDAO(jdbcUserDAO);
            mainController.setMemberLabel(member);
            mainController.setMember(member);
            mainController.setAddressLabel();

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
            MainController mainController = goToNextWindow(mainFXMLFile, event, jdbcUserDAO);
            mainController.setJdbcUserDAO(jdbcUserDAO);
            mainController.setAddressLabel();
        } catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
