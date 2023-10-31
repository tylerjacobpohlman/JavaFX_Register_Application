package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAO;
import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAOImpl;
import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PayController extends ControllerMethods {
    private boolean finishedReceipt = false;
    private Integer receiptNumber;

    @FXML
    private Label amountTotalLabel;
    @FXML
    private TextField amountPaidTextField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label changeDueField;

    /**
     * Sets the receipt number to finalize the receipt.
     * @param receiptNumber Integer representing receipt number
     */
    protected void setReceiptNumber(Integer receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    /**
     * Sets the total amount as a double to two decimal places.
     * @param amount double representing total of receipt.
     */
    public void setAmountTotalLabel(double amount) {
        amountTotalLabel.setText(String.format("%.2f", amount));
    }

    /**
     * Logic for "FINISH AND PAY" button click in GUI.
     * @param event ActionEvent represented by Button click
     */
    public void finishButtonOnClick(ActionEvent event) {
        //resets the error label
        errorLabel.setText("");

        if(finishedReceipt) {
            errorLabel.setText("Transaction already complete! Click 'Start New Transaction' or exit program");
            return;
        }

        if(amountPaidTextField.getText().isEmpty()) {
            errorLabel.setText("Amount paid field is empty! Please type in a numeric value");
            return;
        }

        double amountPaid;
        double amountDue;

        //check for proper double formatting
        try {
            amountPaid = Double.parseDouble(amountPaidTextField.getText());
            amountDue = Double.parseDouble(amountTotalLabel.getText());

            //invalid input where the amount paid isn't a numeric value
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid input! Enter numeric values only...");
            amountPaidTextField.clear();
            return;
        }

        try {
            double changeDue = jdbcUserDAO.finalizeReceipt(amountPaid, amountDue, receiptNumber);
            changeDueField.setText(String.valueOf(changeDue));

            finishedReceipt = true;

        }

        catch (IllegalArgumentException e) {
            errorLabel.setText("Amount paid must be greater or equal to amount due");
            amountPaidTextField.clear();
        }
        //connection is closed
        catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
        //highly unlikely this will fail considering everything else succeeded up to this point
        catch (SQLException e) {
            errorLabel.setText(e.getMessage());
            return;
        }


    }

    /**
     * Logic for when "START NEW TRANSACTION" is clicked in GUI
     * @param event ActionEvent representing button click
     * @throws IOException if unable to read associated FXML file
     */
    public void setStartNewTransactionButtonOnClick(ActionEvent event) throws IOException {

        try {
            //go back to main scene
            MainController mainController = (MainController) goToNextWindow(mainFXMLFile, event, jdbcUserDAO);
            mainController.setJdbcUserDAO(jdbcUserDAO);
            mainController.setAddressLabel();
        }
        catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
