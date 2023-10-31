package com.github.tylerjpohlman.database.register.data_access_classes;

import com.github.tylerjpohlman.database.register.helper_classes.*;

import java.sql.Connection;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.List;

public interface JdbcUserDAO {

    /**
     * Checks if the current connection to database is attainable.
     * @return false if closed, empty, or any other errors; true if open
     */
    boolean isConnectionReachable();

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
    void setConnectionFromLogin(String url, String username, String password, int registerNumber)
            throws DriverNotFoundException, ServerConnectionException, InvalidCredentialsException,
            InvalidRegisterException, SQLException;

    /**
     * Gets the associated store address from the register login details.
     * @return String for concatenation of database's associated address
     * @throws SQLException if unable to get address from database
     */
    String getAddressFromConnection() throws SQLException;

    /**
     * Grabs Item information with given upc value.
     * @param upc        long representing 12 digit upc
     * @return Item object with associated details
     * @throws SQLException if unable to find Item with associated UPC in database
     */
    Item getItemFromUPC(long upc) throws SQLException;

    /**
     * Creates a receipt column in database and returns the receipt's associated number.
     * @param member      Member object
     * @throws SQLException if an error occurs while interacting with database
     */
    int createReceipt(Member member) throws SQLException;

    /**
     * Uses the created receipt number to add items to receipt in database.
     * @param list List of items
     * @param receiptNumber int representing associated receipt number
     * @param member Member object
     * @return double representing the amount due on the receipt
     * @throws SQLException if any error with creating receipt in database
     */
    public double getReceiptTotal(List<Item> list, int receiptNumber, Member member) throws SQLException;

    /**
     * Returns associated Member object from search using phone number in database.
     * @param connection Connection object
     * @param phoneNumber long representing phone number
     * @return associated Member object
     * @throws SQLException if unable to find associated Member
     */
    public Member getMemberFromPhoneNumber(long phoneNumber) throws SQLException;

    /**
     * Returns associated Member object from search using account number in database.
     * @param connection Connection object
     * @param accountNumber long representing associated phone number
     * @return associated Member object
     * @throws SQLException if unable to find associated Member
     */
    public Member getMemberFromAccountNumber(long accountNumber) throws SQLException;

    /**
     *
     * @param connection Connection object
     * @param amountPaid double representing amount paid for transaction
     * @param amountDue double representing amount due for transaction
     * @param receiptNumber long representing the receipt number
     * @return double representing the change due--i.e., difference between the amounts
     * @throws SQLException if error when executing statement to database
     * @throws IllegalArgumentException if amountPaid is less than amountDue
     */
    public double finalizeReceipt(double amountPaid, double amountDue, long receiptNumber)
            throws SQLException, IllegalArgumentException;







}
