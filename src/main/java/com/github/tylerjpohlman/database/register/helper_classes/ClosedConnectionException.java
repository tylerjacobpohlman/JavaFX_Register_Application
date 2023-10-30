package com.github.tylerjpohlman.database.register.helper_classes;

import java.sql.SQLException;

/**
 * An exception thrown when Connection object is closed
 */
public class  ClosedConnectionException extends SQLException {
    public ClosedConnectionException() {
        super("Connection object is closed...");
    }
    public ClosedConnectionException(String message) {
        super(message);
    }
    public ClosedConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClosedConnectionException(Throwable cause) {
        super(cause);
    }
}
