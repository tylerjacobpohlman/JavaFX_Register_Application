package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller class for finish and pay view. Acts at the last step in the register application in which a total is
 * displayed and an amount is given to finalize the transaction within the database. <p>
 * The methods {@link #setReceiptNumber} and {@link #setAmountTotalLabel} are called from outside the class to set these
 * two. <p>
 * The method {@link #finishButtonOnClick(ActionEvent)} computes the finalization in the database and returns the amount
 * due while {@link #setStartNewTransactionButtonOnClick} allows the creation of a new transaction after the current one
 * is finished.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2024-08-19
 */
public class PayController extends BaseController {
    /**
     * Used to determine whether a receipt has been processed.
     */
    private boolean finishedReceipt = false;

    /**
     * Stores the receipt number generated in the database.
     */
    private int receiptNumber;

    /**
     * Label which shows the amount due for the order.
     */
    @FXML
    private Label amountTotalLabel;

    /**
     * Text field used to input the amount of money applied to the order.
     */
    @FXML
    private TextField amountPaidTextField;

    /**
     * Label used to display generated errors.
     */
    @FXML
    private Label errorLabel;

    /**
     * The Label used to display the amount of due after the amount paid has been given.
     */
    @FXML
    private Label changeDueField;

    /**
     * Sets the receipt number to finalize the receipt.
     * @param receiptNumber int representing receipt number
     */
    protected void setReceiptNumber(int receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    /**
     * Sets the total amount as a double to two decimal places.
     * @param amount double representing the total for the receipt.
     */
    public void setAmountTotalLabel(double amount) {
        amountTotalLabel.setText(String.format("%.2f", amount));
    }

    /**
     * Logic for "FINISH AND PAY" button click in GUI.
     * @param event {@link ActionEvent} object represented by Button click
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
        }
    }

    /**
     * Logic for when "START NEW TRANSACTION" is clicked in the GUI
     * @param event {@link ActionEvent} object representing button click
     * @throws IOException if unable to read the associated FXML file
     */
    public void setStartNewTransactionButtonOnClick(ActionEvent event) throws IOException {

        try {
            goToMainWindow(event);
        }
        catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
