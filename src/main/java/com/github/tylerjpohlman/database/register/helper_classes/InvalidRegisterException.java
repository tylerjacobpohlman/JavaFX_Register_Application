package com.github.tylerjpohlman.database.register.helper_classes;

import java.sql.SQLException;

public class InvalidRegisterException extends SQLException {
    public InvalidRegisterException() {
        super("Register id not found in database.");
    }
}
