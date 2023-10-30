package com.github.tylerjpohlman.database.register.helper_classes;

import java.sql.SQLException;

public class DriverNotFoundException extends SQLException {
    public DriverNotFoundException() {
        super("jdbc driver for connecting isn't in program.");
    }
}
