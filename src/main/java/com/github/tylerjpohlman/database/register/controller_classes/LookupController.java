package com.github.tylerjpohlman.database.register.controller_classes;

import com.github.tylerjpohlman.database.register.helper_classes.ClosedConnectionException;
import com.github.tylerjpohlman.database.register.helper_classes.Item;
import com.github.tylerjpohlman.database.register.helper_classes.Member;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.sql.SQLException;

public class LookupController extends MainController{
    public Button CancelButton;
    @FXML
    private TextField itemSearchTextField;
    @FXML
    private Button searchForItemButton;
    @FXML
    private ListView<Item> searchedItemsList;
    @FXML
    private Button addItemButton;
    @FXML
    private Label errorLabel;

    private Item selectedSearchedItem;

    public void searchForItemOnClick(ActionEvent actionEvent) {
        //reset the error label
        errorLabel.setText("");
    }

    public void addSelectedItemInSearchedItems(MouseEvent mouseEvent) {
        //reset the error label
        errorLabel.setText("");
    }

    public void addItemOnClick(ActionEvent actionEvent) {
        //reset the error label
        errorLabel.setText("");
    }

    public void cancelButtonOnCLick(ActionEvent event) throws IOException{
        try {
            goToMainWindow(event);
        } catch (ClosedConnectionException e) {
            setErrorLabelAndGoBackToIntroduction(errorLabel, event);
        }
    }
}
