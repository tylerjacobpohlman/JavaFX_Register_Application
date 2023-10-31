package com.github.tylerjpohlman.database.register.data_access_classes;

import com.github.tylerjpohlman.database.register.helper_classes.*;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcUserDAO {

    /**
     * Checks if the current Connection object is closed.
     * @param connection Connection object for MySQL database
     * @return true if closed, empty, or any other errors; false if open
     */
    boolean isConnectionNotReachable(Connection connection);

    /**
     * Tries logging in using credentials and returns connection object if able to do so.
     * @param url            String containing url to database (append 'jdbc:mysql://' to the beginning)
     * @param username       String representing associated username
     * @param password       String representing associated password
     * @param registerNumber int register register number
     * @return Connection which is logged into database
     * @throws DriverNotFoundException     if not suitable jdbc driver is found in program
     * @throws ServerConnectionException   if server-related issue: issues contacting, no database selected, no server found
     * @throws InvalidCredentialsException if either the username and/or password is incorrect
     * @throws InvalidRegisterException    if the given register number is found in the database
     * @throws SQLException                if an unknown exception occurs that is accounted for
     */
    Connection getConnectionFromLogin(String url, String username, String password, int registerNumber)
            throws DriverNotFoundException, ServerConnectionException, InvalidCredentialsException,
            InvalidRegisterException, SQLException;

    /**
     * @param connection     Connection object which has been properly logged into databased
     * @param registerNumber int representing register number
     * @return String for concatenation of database's associated address
     * @throws SQLException if unable to get address from database
     */
    String getAddressFromConnection(Connection connection, int registerNumber) throws SQLException;

    /**
     * Grabs Item information with given upc value.
     *
     * @param connection Connection object
     * @param upc        long representing 12 digit upc
     * @return Item object with associated details
     * @throws SQLException if unable to find Item with associated UPC in database
     */
    Item getItemFromUPC(Connection connection, long upc) throws SQLException;

    /**
     * Creates a receipt column in database and returns the receipt's associated number.
     *
     * @param connection  Connection object
     * @param member      Member object
     * @param registerNum int register number
     * @throws SQLException if an error occurs while interacting with database
     */
    int createReceipt(Connection connection, Member member, int registerNum) throws SQLException;





}
