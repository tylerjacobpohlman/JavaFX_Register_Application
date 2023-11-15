package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.Item;
import com.github.tylerjpohlman.database.register.helper_classes.Member;
import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

/**
 * {@code MainController} - Another controller class which extends {@code ControllerMethods}. Acts as the main menu of
 * the program, mainly serving to add items to the given {@code ListView<Item>} and having different number of
 * {@code Button} to go to other layouts.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2023-11-15
 */
public class MainController extends ControllerMethods {

    @FXML
    private Label addressLabel;

    @FXML
    private Label membershipLabel;

    @FXML
    private ListView<Item> addedItemsList;

    @FXML
    private TextField itemUPCTextField;

    @FXML
    private Label errorLabel;

    /**
     * Sets the address Label in the main view of the JavaFX program.
     */
    public void setAddressLabel() {
        try {
            String address = jdbcUserDAO.getAddressFromConnection();
            addressLabel.setText(address);
        } catch (SQLException e) {
            addressLabel.setText("Unable to obtain address from server!");
        }
    }

    /**
     * Sets the membership Label in the main view of the JavaFX program.
     * @param member Member object
     */
    public void setMemberLabel(Member member) {
        if(member == null) {
            membershipLabel.setText("");
        }
        else {
            membershipLabel.setText(member.toString());
        }
    }

    /**
     * Logic when clicking "ADD ITEM" in main view.
     * @param event an ActionEvent of type "button click"
     */
    public void addItemOnClick(ActionEvent event) {

        //resets the error label
        errorLabel.setText("");

        //checks if there's not any text
        if (itemUPCTextField.getText().isEmpty()) {
            errorLabel.setText("Please type in the UPC number first");
            return;
        }

        //checks if connection is closed
        if(!jdbcUserDAO.isConnectionReachable()) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
            return;
        }


        //grabs upc and checks if it's only numeric values
        long upc;
        try {
            upc = Long.parseLong(itemUPCTextField.getText());
        } catch (NumberFormatException e) {
            errorLabel.setText("Type in only a 12 digit numeric value!");
            itemUPCTextField.clear();
            return;
        }

        //Item object
        Item item;

        try {
            item = jdbcUserDAO.getItemFromUPC(upc);
        } catch (SQLException e) {
            errorLabel.setText("Unable to find item!");
            //blank out the upc text field
            itemUPCTextField.clear();
            return;
        }
            //blank out the upc text field
            itemUPCTextField.clear();
            //adds grabbed Item object and adds it to added items list
            addedItemsList.getItems().add(item);
    }

    /**
     * Logic when clicking 'Member Lookup' in main view.
     * @param event ActionEvent from button click in GUI
     * @throws IOException if unable to read associated FXML document
     */
    public void memberLookupOnCLick(ActionEvent event) throws IOException {
        //check if one or more items are added
        if(!addedItemsList.getItems().isEmpty()) {
            errorLabel.setText("Cannot input membership after adding items");
            return;
        }

        //checks for already inputted membership
        if(member != null) {
            errorLabel.setText("Membership already inputted...");
            return;
        }

        try {
            MemberController memberController = (MemberController) goToNextWindow(memberFXMLFile, event, jdbcUserDAO);
            memberController.setJdbcUserDAO(jdbcUserDAO);//passes Connection
        }
        catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }

    /**
     * Logic when clicking 'FINISH AND PAY' in GUI.
     * @param event ActionEvent representing button click
     * @throws IOException if unable to read associated FXML file
     */
    public void finishAndPayOnClick(ActionEvent event) throws IOException {
        if(addedItemsList.getItems().isEmpty()) {
            errorLabel.setText("Cannot finalize a transaction with no items!");
            return;
        }

        //checks if connection is closed
        if(!jdbcUserDAO.isConnectionReachable()) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
            return;
        }

        int receiptNumber;
        double amountDue;

        try {
            receiptNumber = jdbcUserDAO.createReceipt(member);
            amountDue = jdbcUserDAO.getReceiptTotal(addedItemsList.getItems(), receiptNumber, member);

        //error is unlikely to be thrown if connection was already check, so just click the button again
        } catch (SQLException e) {
            errorLabel.setText(e.getMessage());
            return;
        }

        try {
            //considering all goes well, goes on to the final scene to get the amount paid and amount due
            PayController payController = (PayController)goToNextWindow(payFXMLFile, event, jdbcUserDAO);
            payController.setJdbcUserDAO(jdbcUserDAO);
            payController.setMember(member);
            payController.setReceiptNumber(receiptNumber);
            payController.setAmountTotalLabel(amountDue);

        } catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
