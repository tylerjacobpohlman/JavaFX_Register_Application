package com.github.tylerjpohlman.database.register.helper_classes;

import java.sql.SQLException;

public class ServerConnectionException extends SQLException {
    public ServerConnectionException() {
        super("Unable to connect to database");
    }
}
