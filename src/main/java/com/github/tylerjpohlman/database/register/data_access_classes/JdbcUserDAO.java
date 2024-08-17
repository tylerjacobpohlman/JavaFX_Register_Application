package com.github.tylerjpohlman.database.register.data_access_classes;

import com.github.tylerjpohlman.database.register.helper_classes.*;

import java.sql.SQLException;
import java.util.List;

/**
 * An interface which acts as the template for a data access object used to interact with the MySQL "hvs" database.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2023-11-24
 */
public interface JdbcUserDAO {

    /**
     * Checks if the current connection to the database is attainable.
     * @return true if closed, empty, or any other errors; false if open
     */
    boolean isConnectionNotReachable();

    /**
     * Tries logging in using credentials and returns connection object if able to do so.
     * @param url String containing url to the database (append 'jdbc:mysql://' to the beginning)
     * @param username String representing associated username
     * @param password String representing associated password
     * @param registerNumber int register register number
     * @throws DriverNotFoundException if not suitable jdbc driver is found in the program
     * @throws ServerConnectionException if server-related issue: issues contacting, no database selected, no server found
     * @throws InvalidCredentialsException if either the username and/or password is incorrect
     * @throws InvalidRegisterException if the given register number is found in the database
     * @throws SQLException if an unknown exception occurs
     */
    void setConnectionFromLogin(String url, String username, String password, int registerNumber)
            throws DriverNotFoundException, ServerConnectionException, InvalidCredentialsException,
            InvalidRegisterException, SQLException;

    /**
     * Gets the associated store address from the register login details.
     * @return String for concatenation of database's associated address
     * @throws InvalidRegisterException if unable to get address from the database using register id
     * @throws SQLException if any other error when contacting the database occurs
     */
    String getAddressFromConnection() throws InvalidRegisterException, SQLException;

    /**
     * Grabs Item information with given upc value.
     * @param upc long representing 12 digit upc
     * @return {@link Item} object with associated details
     * @throws SQLException if unable to find Item with associated UPC in the database
     */
    Item getItemFromUPC(long upc) throws SQLException;

    /**
     * Creates a receipt column in the database and returns the receipt's associated number.
     * @param member {@link Member} object
     * @return int representing receipt number
     * @throws SQLException if an error occurs while interacting with the database
     */
    int createReceipt(Member member) throws SQLException;

    /**
     * Uses the created receipt number to add items to receipt in the database.
     * @param list {@link List} of items
     * @param receiptNumber int representing associated receipt number
     * @param member {@link Member} object
     * @return double representing the amount due on the receipt
     * @throws SQLException if any error with creating receipt in the database
     */
    double getReceiptTotal(List<Item> list, int receiptNumber, Member member) throws SQLException;

    /**
     * Returns associated Member object from search using phone number in the database.
     * @param phoneNumber long representing phone number
     * @return associated Member object
     * @throws SQLException if unable to find associated Member
     */
    Member getMemberFromPhoneNumber(long phoneNumber) throws SQLException;

    /**
     * Returns associated Member object from search using account number in the database.
     * @param accountNumber long representing associated phone number
     * @return associated Member object
     * @throws SQLException if unable to find associated Member
     */
    Member getMemberFromAccountNumber(long accountNumber) throws SQLException;

    /**
     * Finalizes the receipt in the database.
     * @param amountPaid double representing amount paid for transaction
     * @param amountDue double representing amount due for transaction
     * @param receiptNumber long representing the receipt number
     * @return double representing the change due--i.e., difference between the amounts
     * @throws SQLException if error when executing statement to database
     * @throws IllegalArgumentException if amountPaid is less than amountDue
     */
    double finalizeReceipt(double amountPaid, double amountDue, long receiptNumber)
            throws SQLException, IllegalArgumentException;







}
