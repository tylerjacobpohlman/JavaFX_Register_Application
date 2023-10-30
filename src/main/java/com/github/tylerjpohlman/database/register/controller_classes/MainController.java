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
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MainController extends ControllerMethods {
    private String registerNum;

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
    protected void setRegisterNum(String registerNum) {
        this.registerNum = registerNum;
    }

    /**
     * Sets the address Label in the main view of the JavaFX program.
     */
    public void setAddressLabel() {
        //grabs the address using the registerID
        //NOTE: This is incredibly sloppy! I wasn't sure how to grab the result of a function, so I turned
        // storeAddressLookupFromRegister into a procedure and grabbed the address this way
        try {
            PreparedStatement ps = connection.prepareStatement("CALL storeAddressLookupFromRegister(?)");
            ps.setString(1, registerNum);
            String addressLookup = "Call storeAddressLookupFromRegister(" + registerNum + ")";
            ps = connection.prepareStatement(addressLookup);
            //stores the address in the result set
            rs = ps.executeQuery();
            while (rs.next()) {
                addressLabel.setText(rs.getString(1));
            }
        } catch (SQLException e) {
            addressLabel.setText("ERROR: Unable to find address");
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
        //reset the error label
        errorLabel.setText("");

        //checks if there's not any text
        if (itemUPCTextField.getText().isEmpty()) {
            errorLabel.setText("Please type in the UPC number first");
            return;
        }

        //checks if connection is closed
        if(isClosed(connection)) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
            return;
        }

        //elements of the Item to grab
        String upc = itemUPCTextField.getText();
        String name = null;
        double price = 0;
        double discount = 0;


        try {
            String itemUPCLookup = "Call itemUPCLookup('" + upc + "')";
            ps = connection.prepareStatement(itemUPCLookup);
            //stores the address in the result set
            rs = ps.executeQuery();
            while (rs.next()) {
                name = rs.getString(1);
                price = rs.getDouble(2);
                discount = rs.getDouble(3);
            }

            //creates a new Item given the grabbed attributes
            addedItemsList.getItems().add(new Item(upc, name, price, discount));


            //blank out the upc text field
            itemUPCTextField.setText("");

        } catch (SQLException e) {
            errorLabel.setText("Unable to find Item with given upc");
        }
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
        int receiptNumber = 0;
        double amountDue = 0, stateTax = 0;
        String createReceipt;

        //if there is no provided membership
        if (member == null) {

            createReceipt = "CALL createReceipt(" + registerNum + ", null)";
        }
        //a membership was provided
        else {
            createReceipt = "CALL createReceipt('" + registerNum + "', '" + member.getAccountNumber() + "')";
        }

        //checks if connection is closed
        if(isClosed(connection)) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
            return;
        }
        
        try {
            ps = connection.prepareStatement(createReceipt);
            //grabs the receipt number that was created
            rs = ps.executeQuery();
            while (rs.next()) {
                receiptNumber = Integer.parseInt(rs.getString(1));
            }
            //unlikely to throw an error, so just try again if there's an issue
        } catch (SQLException e) {
            errorLabel.setText("Please try again...");
            return;
        }

        try {
            //adds all the items to the receipt_details table
            for (int i = 0; i < addedItemsList.getItems().size(); i++) {
                String addItem = "CALL addItemToReceipt('" + addedItemsList.getItems().get(i).getUpc() + "', "
                        + receiptNumber + ")";
                ps = connection.prepareStatement(addItem);
                ps.execute();
            }
            //highly unlikely that, considering everything else worked, this would too...
            //so this is here for debugging purposes
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        //SHOULD PUT LOGIC ON SERVER SIDE!!!
        for (int i = 0; i < addedItemsList.getItems().size(); i++) {

            double itemAmount = addedItemsList.getItems().get(i).getPrice();

            double discount = 0.0;
            //only grabs the discount if there's a given member
            if (member != null) {
                discount = addedItemsList.getItems().get(i).getDiscount();
            }

            double discountedItem = itemAmount * (1 - discount);

            //adds that item price to the grand total
            amountDue += discountedItem;
        }

        //SHOULD PUT LOGIC ON SERVER SIDE!!!
        try {
            String getStateTax = "CALL getStateTax(" + receiptNumber + ")";

            ps = connection.prepareStatement(getStateTax);
            //stores the address in the result set
            rs = ps.executeQuery();
            while (rs.next()) {
                stateTax = Double.parseDouble(rs.getString(1));
            }

            //sets the amount due including state tax
            amountDue = amountDue * (1 + stateTax);

            //considering all goes well, goes on to the final scene to get the amount paid and amount due
            PayController payController = (PayController)goToNextWindow(payFXMLFile, event);
            payController.setConnection(connection);
            payController.setMember(member);
            payController.setReceiptNumber(receiptNumber);
            payController.setAmountTotalLabel(amountDue);

        } catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
        //highly unlikely that, considering everything else worked, this would too...
        //so this is here for debugging purposes
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
