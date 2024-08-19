package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.Item;
import com.github.tylerjpohlman.database.register.helper_classes.Member;
import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller class which controls the logic behind the main menu view of the program. <p>
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2024-08-19
 */
public class MainController extends BaseController {
    @FXML
    private Label addressLabel;

    @FXML
    private Label membershipLabel;

    /**
     * List View of Items added to the current shopping session. Displayed in the MainController window.
     */
    @FXML
    protected ListView<Item> addedItemsList;

    /**
     * Text Field used to type in an item's upc.
     */
    @FXML
    private TextField itemUPCTextField;

    /**
     * Label used to display generated errors.
     */
    @FXML
    Label errorLabel;

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
     */
    public void setMemberLabel() {
        if(member == null) {
            membershipLabel.setText("");
        }
        else {
            membershipLabel.setText("Welcome " + member.toString() + "!");
        }
    }

    /**
     * Logic when clicking "ADD ITEM" in the main view.
     * @param event {@link ActionEvent} Object representing the button click.
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
        if(jdbcUserDAO.isConnectionNotReachable()) {
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
            //adds a grabbed Item object and adds it to the added items list
            addedItemsList.getItems().add(item);
    }

    /**
     * Logic when clicking "LOOKUP ITEM" in the main view.
     * @param event {@link ActionEvent} object representing the button click
     * @throws IOException if unable to read associated FXML document
     */
    public void lookupItemOnClick(ActionEvent event) throws IOException {
        //resets the error label
        errorLabel.setText("");

        try {
            goToLookupWindow(event);
        }
        catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }

    /**
     * Logic when clicking 'Member Lookup' in the main view.
     * @param event {@link ActionEvent} object representing button click in GUI
     * @throws IOException if unable to read associated FXML document
     */
    public void memberLookupOnCLick(ActionEvent event) throws IOException {
        //checks for already inputted membership
        if(member != null) {
            errorLabel.setText("Membership already inputted...");
            return;
        }

        try {
            goToMemberWindow(event);
        }
        catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }

    /**
     * Logic when clicking 'FINISH AND PAY' in GUI.
     * @param event {@link ActionEvent} representing button click
     * @throws IOException if unable to read the associated FXML file
     */
    public void finishAndPayOnClick(ActionEvent event) throws IOException {
        if(addedItemsList.getItems().isEmpty()) {
            errorLabel.setText("Cannot finalize a transaction with no items!");
            return;
        }

        try {
            goToPayWindow(event);

        } catch (SQLException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
