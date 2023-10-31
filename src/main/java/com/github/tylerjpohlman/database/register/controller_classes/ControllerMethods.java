package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;
import com.github.tylerjpohlman.database.register.helper_classes.Member;
import com.github.tylerjpohlman.database.register.data_access_classes.JdbcUserDAOImpl;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;

public abstract class ControllerMethods {
    /**
     * MySQL Connection to database using login credentials
     */
    protected Connection connection = null;
    /**
     * Associated Member found in MySQL database
     */
    protected Member member = null;

    protected int registerNumber;

    protected static final String mainFXMLFile = "main-view.fxml";
    protected static final String introductionFXMLFile = "introduction-view.fxml";
    protected static final String memberFXMLFile = "member-view.fxml";
    protected static final String payFXMLFile = "pay-view.fxml";



    /**
     * Sets the current window to a new window given the name of that window's FXML file.
     * @param fileName name of FXML file
     * @param event ActionEvent most likely representing a button click
     * @return Object representing instance of controller class
     * @throws IOException if error occurs when loading FXML file
     * @throws ClosedConnectionException if Connection object is closed
     */
    protected Object goToNextWindow(String fileName, ActionEvent event) throws IOException, ClosedConnectionException {
        //checks if connection is still open
        JdbcUserDAOImpl jdbcUserDAOImpl = new JdbcUserDAOImpl();

        if(jdbcUserDAOImpl.isConnectionNotReachable(connection)) {
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
     * Sets the SQL Connection used in controller class.
     * @param connection an SQL Connection
     */
    protected void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Sets Member object.
     * @param member A given Member object.
     */
    protected void setMember(Member member) {
        this.member = member;
    }

    /**
     * Sets the register number used in controller class.
     * @param registerNum a String
     */
    protected void setRegisterNum(int registerNumber) {
        this.registerNumber = registerNumber;
    }


}
