package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAOImpl;
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

public class MainController extends ControllerMethods {
    private int registerNum;

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
     * Sets the register number used in controller class.
     * @param registerNum a String
     */
    protected void setRegisterNum(int registerNum) {
        this.registerNum = registerNum;
    }

    /**
     * Sets the address Label in the main view of the JavaFX program.
     */
    public void setAddressLabel() {
        //used to access SQL methods
        JdbcUserDAOImpl jdbcUserDAOImpl = new JdbcUserDAOImpl();

        try {
            String address = jdbcUserDAOImpl.getAddressFromConnection(connection, registerNum);
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
        //used to access SQL methods
        JdbcUserDAOImpl jdbcUserDAOImpl = new JdbcUserDAOImpl();

        //resets the error label
        errorLabel.setText("");

        //checks if there's not any text
        if (itemUPCTextField.getText().isEmpty()) {
            errorLabel.setText("Please type in the UPC number first");
            return;
        }

        //checks if connection is closed
        if(jdbcUserDAOImpl.isConnectionNotReachable(connection)) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
            return;
        }


        //grabs upc and checks if it's only numeric values
        long upc;
        try {
            String upcString = itemUPCTextField.getText();
            upc = Long.parseLong(itemUPCTextField.getText());
        } catch (NumberFormatException e) {
            errorLabel.setText("Type in only a 12 digit numeric value!");
            itemUPCTextField.clear();
            return;
        }

        //Item object
        Item item;

        try {
            item = jdbcUserDAOImpl.getItemFromUPC(connection, upc);
        } catch (SQLException e) {
            errorLabel.setText("Unable to find item!");
            //blank out the upc text field
            itemUPCTextField.clear();
            return;
        }

            //blank out the upc text field
            itemUPCTextField.setText("");
            //adds grabbed Item object and adds it to added items list
            addedItemsList.getItems().add(item);

    }


    /**
     * Logic when clicking 'Member Lookup' in main view.
     * @param event ActionEvent from button click in GUI
     * @throws IOException if unable to read associated FXML document
     */
    public void memberLookupOnCLick(ActionEvent event) throws IOException {
        //checks for already inputted membership
        if(member != null) {
            errorLabel.setText("Membership already inputted...");
            return;
        }

        try {
            MemberController memberController = (MemberController) goToNextWindow(memberFXMLFile, event);
            memberController.setConnection(connection);//passes Connection object
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

        //used to access SQL methods
        JdbcUserDAOImpl jdbcUserDAOImpl = new JdbcUserDAOImpl();

        //checks if connection is closed
        if(jdbcUserDAOImpl.isConnectionNotReachable(connection)) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
            return;
        }

        int receiptNumber = 0;
        double amountDue = 0.0;

        try {
            receiptNumber = jdbcUserDAOImpl.createReceipt(connection, member, registerNum);
            amountDue = jdbcUserDAOImpl.finalizeReceipt(connection, addedItemsList.getItems(), receiptNumber, member);

        //error is unlikely to be thrown if connection was already check, so just click the button again
        } catch (SQLException e) {
            errorLabel.setText("Please try again...");
            return;
        }

        try {
            //considering all goes well, goes on to the final scene to get the amount paid and amount due
            PayController payController = (PayController)goToNextWindow(payFXMLFile, event);
            payController.setConnection(connection);
            payController.setMember(member);
            payController.setReceiptNumber(receiptNumber);
            payController.setAmountTotalLabel(amountDue);

        } catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
