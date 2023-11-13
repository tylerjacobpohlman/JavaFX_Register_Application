package com.github.tylerjpohlman.database.register.helper_classes;

import java.sql.SQLException;

public class InvalidCashierException extends SQLException {
    public InvalidCashierException() {
        super("Cashier id not found in database.");
    }
}
