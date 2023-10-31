package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;
import com.github.tylerjpohlman.database.register.helper_classes.Member;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MemberController extends ControllerMethods{
    PreparedStatement ps;
    ResultSet rs;

    private enum Type{
        PHONE_NUMBER,
        MEMBER_ID
    }

    @FXML
    private Label errorLabel;

    @FXML
    private TextField memberIDTextField;

    @FXML
    private TextField phoneNumberTextField;

    /**
     * Logic when clicking enter button in GUI.
     * @param event ActionEvent representing button click
     * @throws IOException if unable to read FXML file
     */
    public void enterButtonOnClick(ActionEvent event) throws IOException {
        //reset the error label
        errorLabel.setText("");

        String sqlStatement;
        //stores whether phone number or member id was given
        Type givenInput;

        //elements of the member to add
        String accountNumber = null;
        String phoneNumber;
        String firstName;
        String lastName;

        //if there isn't any entered text
        if (phoneNumberTextField.getText().isEmpty() && memberIDTextField.getText().isEmpty()) {
            errorLabel.setText("Please enter either a member number or phone number.");
            return;//exit method
        }
        //if there's a phone number provided
        else if (!phoneNumberTextField.getText().isEmpty()) {
            //grabs the phone number
            //also removes all the misc. chars when someone types in a phone number and just keeps the digits
            phoneNumber = phoneNumberTextField.getText().replaceAll("[^0-9]", "");
            sqlStatement = "Call memberPhoneLookup('" + phoneNumber + "')";
            givenInput = Type.PHONE_NUMBER;
        }
        //if only the account number is provided
        else {
            //grabs the account number
            accountNumber = memberIDTextField.getText().replaceAll("[^0-9]", "");
            sqlStatement = "Call memberAccountNumberLookup(" + accountNumber + ")";
            givenInput = Type.MEMBER_ID;
        }

        try {
            ps = connection.prepareStatement(sqlStatement);
            //stores the member in the result set
            rs = ps.executeQuery();
            if(givenInput == Type.PHONE_NUMBER) {
                while (rs.next()) {
                    accountNumber = rs.getString(1);
                    firstName = rs.getString(2);
                    lastName = rs.getString(3);
                    //initializes the member using the grabbed attributes
                    member = new Member(accountNumber, firstName, lastName);
                }
            }
            else {
                while (rs.next()) {
                    firstName = rs.getString(1);
                    lastName = rs.getString(2);
                    //initializes the member using the grabbed attributes
                    member = new Member(accountNumber, firstName, lastName);
                }
            }
            MainController mainController = (MainController)goToNextWindow(mainFXMLFile, event);
            mainController.setMember(member);//passes Connection object
            mainController.setMemberLabel(member);

        } catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
        catch (SQLException e) {
            errorLabel.setText("Unable to find membership with provided phone number / member id.");
        }

        //resets the text fields
        phoneNumberTextField.setText("");
        memberIDTextField.setText("");
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
