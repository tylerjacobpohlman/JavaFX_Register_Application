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
 * An abstract superclass used for the functionality between controller classes
 * which inherit it. </p>
 * The Super Class for all the Controller classes. This Abstract Class is primarily used to switch layouts between
 * the controllers and associated fxml files. Likewise, it also contains the {@link JdbcUserDAO} class which is
 * used by all the controllers to interface with the backend database. Any newly created controller class should extend
 * this class. Any use of backend database should utilize {@link JdbcUserDAO} class which is declared as
 * {@link #jdbcUserDAO}.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2023-11-24
 */
public abstract class BaseController {

    /**
     * Associated Member found in MySQL database
     */
    protected Member member = null;

    /**
     * Data Access Object used to interface with MySQL database.
     */
    protected JdbcUserDAO jdbcUserDAO = null;

    /**
     * Sets the data access object passed between controller classes.
     * Could also implement, for example, {@code mainController.jdbcUserDAO = jdbcUserDAO}, but this allows for
     * better readability.
     * @param jdbcUserDAO {@link JdbcUserDAO}, the given data access object
     */
    public void setJdbcUserDAO(JdbcUserDAO jdbcUserDAO) {
        this.jdbcUserDAO = jdbcUserDAO;
    }

    /**
     * Sets Member object.
     * Could also implement, for example, {@code mainController.member = member}, but this allows for
     * better readability.
     * @param member {@link Member} object.
     */
    protected void setMember(Member member) {
        this.member = member;
    }

    /**
     * Sets the current window to a new window given the name of that window's FXML file. <p></p>
     * WARNING: This method returns a generic type and requires the caller to ensure that the type of the controller in
     * the FXML file matches the expected type.
     * A {@link ClassCastException} will be thrown at runtime if the types do not match.
     * <p></p>
     * @param fileName name of FXML file
     * @param event {@link ActionEvent} representing a button click
     * @param jdbcUserDAO {@link JdbcUserDAO} data access object used to interface with the database
     * @return {@code <T> T} the controller of type T associated with the loaded FXML file
     * @throws IOException if error occurs when loading FXML file
     * @throws ClosedConnectionException if there's an issue when reaching the database
     */
    protected <T> T goToNextWindow(String fileName, ActionEvent event, JdbcUserDAO jdbcUserDAO) throws IOException,
            ClosedConnectionException {

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

        return fxmlLoader.getController();
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
}
