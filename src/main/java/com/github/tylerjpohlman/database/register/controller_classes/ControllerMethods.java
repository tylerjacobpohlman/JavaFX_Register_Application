package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAO;
import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;
import com.github.tylerjpohlman.database.register.helper_classes.Member;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * {@code ControllerMethods} - An abstract super class used primarily for the functionality between controller classes
 * which inherit it. </p>
 * The Super Class for all the Controller classes. This Abstract Class is primarily used to switch layouts between
 * the controllers and associated fxml files. Likewise, it also contains the {@code jdbcUserDAO} declaration which is
 * used by all the controllers to interface with the backend database. Any newly created controller class should extend
 * this class. Any use of backend database should utilize {@code jdbcUserDAO}.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2023-11-14
 */
public abstract class ControllerMethods {

    /**
     * Associated Member found in MySQL database
     */
    protected Member member = null;

    /**
     * Data Access Object used to interface with MySQL database.
     */
    protected JdbcUserDAO jdbcUserDAO = null;
    protected static final String mainFXMLFile = "main-view.fxml";
    protected static final String introductionFXMLFile = "introduction-view.fxml";
    protected static final String memberFXMLFile = "member-view.fxml";
    protected static final String payFXMLFile = "pay-view.fxml";


    public void setJdbcUserDAO(JdbcUserDAO jdbcUserDAO) {
        this.jdbcUserDAO = jdbcUserDAO;
    }

    /**
     * Sets the current window to a new window given the name of that window's FXML file.
     * @param fileName name of FXML file
     * @param event ActionEvent most likely representing a button click
     * @param jdbcUserDAO data access object used to interface with database
     * @return Object representing instance of controller class
     * @throws IOException if error occurs when loading FXML file
     * @throws ClosedConnectionException if Connection object is closed
     */
    protected Object goToNextWindow(String fileName, ActionEvent event, JdbcUserDAO jdbcUserDAO) throws IOException, ClosedConnectionException {

        if(!jdbcUserDAO.isConnectionReachable()) {
            throw new ClosedConnectionException();
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fileName));
        Parent root = fxmlLoader.load();//instantiates all the objects in the FXML file
        //grab Stage object using Event object
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        return fxmlLoader.getController();
    }

    /**
     * Sets the current window to the introduction window.
     * @param event ActionEvent representing button click
     * @throws IOException if error occurs while loading FXML file
     */
    protected void goToIntroductionWindow(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(introductionFXMLFile));
        Parent root = fxmlLoader.load();//instantiates all the objects in the FXML file
        //grab Stage object using Event object
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Sets the error label to text saying connection is closed and returns to login screen.
     * @param errorLabel a Label object used for displaying errors
     * @param event ActionEvent representing button click
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
     * Sets Member object.
     * @param member A given Member object.
     */
    protected void setMember(Member member) {
        this.member = member;
    }




}
