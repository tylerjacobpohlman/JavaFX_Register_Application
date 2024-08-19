package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAO;
import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;
import com.github.tylerjpohlman.database.register.helper_classes.Item;
import com.github.tylerjpohlman.database.register.helper_classes.Member;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * An abstract superclass used for the functionality between controller classes which inherit it. </p>
 * The Super Class for all the Controller classes. This Abstract Class is primarily used to switch layouts between
 * the controllers and associated fxml files. Likewise, it also contains the {@link JdbcUserDAO} class which is
 * used by all the controllers to interface with the backend database. Any newly created controller class should extend
 * this class. Any use of backend database should utilize {@link JdbcUserDAO} class which is declared as
 * {@link #jdbcUserDAO}.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2024-08-19
 */
public abstract class BaseController {
    /**
     * file name for the main view FXML file
     */
    public static final String mainFXMLFile = "main-view.fxml";
    /**
     * file name for the member view FXML file
     */
    public static final String memberFXMLFile = "member-view.fxml";
    /**
     * file name for the pay view FXML file
     */
    public static final String payFXMLFile = "pay-view.fxml";
    /**
     * file name for the member lookup FXML file
     */
    public static final String itemLookupFXMLFile = "lookup-view.fxml";


    /**
     * Associated Member found in MySQL database
     */
    protected Member member = null;
    /**
     * List containing all the items added to the current session. Used to persist the list between windows.
     */
    protected ArrayList<Item> itemsList = new ArrayList<>(); //need to initialize to avoid null pointer error
    /**
     * Data Access Object used to interface with MySQL database.
     */
    protected JdbcUserDAO jdbcUserDAO = null;


    /**
     * Sets the current window to the main window.
     * @param event {@link ActionEvent} representing a button click
     * @throws IOException if error occurs when loading FXML file
     * @throws ClosedConnectionException if there's an issue when reaching the database
     */
    protected void goToMainWindow(ActionEvent event) throws ClosedConnectionException, IOException {
        MainController mainController =
                (MainController) goToNextWindow(MainController.mainFXMLFile, event, jdbcUserDAO, member);

        mainController.setAddressLabel();
        mainController.setMemberLabel();

        //persist the item list across windows
        for(Item item: itemsList) {
            mainController.addedItemsList.getItems().add(item);
        }
    }

    /**
     * Sets the current window to the lookup window. </p>
     * WARNING: This method can only be invoked from {@link MainController}.
     * Otherwise, a runtime exception will occur.
     * @param event {@link ActionEvent} representing a button click
     * @throws IOException if error occurs when loading FXML file
     * @throws ClosedConnectionException if there's an issue when reaching the database
     */
    protected void goToLookupWindow(ActionEvent event) throws ClosedConnectionException, IOException {
        LookupController lookupController
                = (LookupController) goToNextWindow(itemLookupFXMLFile,event, jdbcUserDAO, member);

        //persist the item list across windows
        lookupController.itemsList.addAll(((MainController) this).addedItemsList.getItems());
    }

    /**
     * Sets the current window to the membership window. </p>
     * WARNING: This method can only be invoked from {@link MainController}.
     * Otherwise, a runtime exception will occur.
     * @param event {@link ActionEvent} representing a button click
     * @throws IOException if error occurs when loading FXML file
     * @throws ClosedConnectionException if there's an issue when reaching the database
     */
    protected void goToMemberWindow(ActionEvent event) throws ClosedConnectionException, IOException {
        MemberController memberController
                = (MemberController) goToNextWindow(memberFXMLFile, event, jdbcUserDAO, member);

        //persist the item list across windows
        memberController.itemsList.addAll(((MainController) this).addedItemsList.getItems());
    }

    /**
     * Sets the current window to the payment window. </p>
     * WARNING: This method can only be invoked from {@link MainController}.
     * Otherwise, a runtime exception will occur.
     * @param event {@link ActionEvent} representing a button click
     * @throws IOException if error occurs when loading FXML file
     * @throws SQLException if there's an issue when reaching the database
     */
    protected void goToPayWindow(ActionEvent event) throws SQLException, IOException {
        PayController payController
                = (PayController) goToNextWindow(payFXMLFile,event, jdbcUserDAO, member);

        //persist the item list across windows
        payController.itemsList.addAll(((MainController) this).addedItemsList.getItems());

        int receiptNumber;
        double amountDue;

        receiptNumber = jdbcUserDAO.createReceipt(member);
        amountDue = jdbcUserDAO.getReceiptTotal(payController.itemsList, receiptNumber, member);

        payController.setReceiptNumber(receiptNumber);
        payController.setAmountTotalLabel(amountDue);

        //reset all applicable fields for a new transaction
        payController.member = null;
        payController.itemsList.clear();
    }

    /**
     * Sets the current window to a new window given the name of that window's FXML file. Used a helper class for </p>
     * for all the other goTo[]Window methods.
     * <p></p>
     * @param fileName name of FXML file
     * @param event {@link ActionEvent} representing a button click
     * @param jdbcUserDAO {@link JdbcUserDAO} data access object used to interface with the database
     * @param member {@link Member} associated membership information
     * @return {@link BaseController} associated with the loaded FXML file
     * @throws IOException if error occurs when loading FXML file
     * @throws ClosedConnectionException if there's an issue when reaching the database
     */
    private BaseController goToNextWindow(String fileName, ActionEvent event, JdbcUserDAO jdbcUserDAO, Member member)
            throws IOException, ClosedConnectionException {

        if(jdbcUserDAO.isConnectionNotReachable()) {
            throw new ClosedConnectionException();
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fileName));
        Parent root = fxmlLoader.load();//instantiates all the objects in the FXML file
        //grabs the Stage object using the Event object
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        BaseController baseController = fxmlLoader.getController();//grabs associated controller generated above
        baseController.jdbcUserDAO = jdbcUserDAO;//passes jdbcUserDAO instance to controller
        baseController.member = member;//passes membership information to controller
        return baseController;
    }




    /**
     * Sets the error label to text saying connection is closed and returns to the login screen.
     * Used when an unforeseen error occurs which requires a "hard" restart of the program.
     * @param errorLabel {@link Label} object used for displaying errors
     * @param event {@link ActionEvent} object representing button click
     */
    protected void setErrorLabelAndGoBackToIntroduction(Label errorLabel, ActionEvent event) {
            errorLabel.setText("Connection is closed... Now returning to login screen");
            //pauses thread to show error text
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            //goes back to log in screen
            try {
                goToIntroductionWindow(event);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
    }
    /**
     * Sets the current window to the introduction window.
     * @param event {@link ActionEvent}, representing button click
     * @throws IOException if error occurs while loading FXML file
     */
    protected void goToIntroductionWindow(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(IntroductionController.introductionFXMLFile));
        Parent root = fxmlLoader.load();//instantiates all the objects in the FXML file
        //grab the Stage object using the Event object
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
