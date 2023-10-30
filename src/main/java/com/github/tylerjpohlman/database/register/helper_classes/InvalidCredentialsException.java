package com.github.tylerjpohlman.database.register.helper_classes;

import java.sql.SQLException;

public class InvalidCredentialsException extends SQLException {
    public InvalidCredentialsException() {
        super("Username and/or password is incorrect.");
    }
}
