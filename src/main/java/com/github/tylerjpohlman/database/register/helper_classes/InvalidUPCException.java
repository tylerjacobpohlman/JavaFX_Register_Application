package com.github.tylerjpohlman.database.register.helper_classes;

import java.sql.SQLException;

/**
 * Exception throw when an invalid UPC is given.
 */
public class InvalidUPCException extends SQLException {

        public InvalidUPCException() {
            super("Connection object is closed...");
        }
        public InvalidUPCException(String message) {
            super(message);
        }
        public InvalidUPCException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidUPCException(Throwable cause) {
            super(cause);
        }

}
