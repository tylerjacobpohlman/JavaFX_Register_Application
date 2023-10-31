package com.github.tylerjpohlman.database.register.data_access_classes;

import com.github.tylerjpohlman.database.register.controller_classes.MainController;
import com.github.tylerjpohlman.database.register.controller_classes.MemberController;
import com.github.tylerjpohlman.database.register.helper_classes.*;

import java.sql.*;
import java.util.List;

public class JdbcUserDAOImpl implements JdbcUserDAO {
    private PreparedStatement ps = null;
    private ResultSet rs = null;

    /**
     * Checks if the current Connection object is closed.
     *
     * @param connection Connection object for MySQL database
     * @return true if closed, empty, or any other errors; false if open
     */
    public boolean isConnectionNotReachable(Connection connection) {
        try {
            //checks if Connection is closed
            if (connection != null && connection.isClosed()) {
                return true;
            }
            //if there's no connection object at all
            else if (connection == null) {
                return true;
            }
            //any other Connection errors will also return true
        } catch (SQLException e) {
            return true;
        }

        return false;
    }

    /**
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
    public Connection getConnectionFromLogin(String url, String username, String password, int registerNumber)
            throws DriverNotFoundException, ServerConnectionException, InvalidCredentialsException,
            InvalidRegisterException, SQLException {

        Connection connection;

        try {
            //tries to establish a connection to the database
            connection = DriverManager.getConnection(url, username, password);

            //tries cashier login procedure
            ps = connection.prepareStatement("CALL cashierRegisterLogin(?, ?)");
            ps.setString(1, username);
            ps.setInt(2, registerNumber);
            ps.execute();

            ps.close();

        } catch (SQLException e) {
            String errorCode = e.getSQLState();
            switch (errorCode) {
                //if the driver isn't downloaded or defined in the url
                case "08001":
                    throw new DriverNotFoundException();
                    //if there's issue contacting the server, a database isn't selected, or the server cannot be found at all
                case "08S01", "3D000":
                case null:
                    throw new ServerConnectionException();
                    //if either the username and/or password is incorrect
                case "28000":
                    throw new InvalidCredentialsException();
                    //error defined in database: "no such register_id and/or cashier_id exists"
                    //basically, this error is invoked when an invalid register number is given
                case "45000":
                    throw new InvalidRegisterException();
                default:
                    throw new SQLException(e);
            }
        }

        return connection;
    }

    /**
     * @param connection     Connection object which has been properly logged into databased
     * @param registerNumber int representing register number
     * @return String for concatenation of database's associated address
     * @throws SQLException if unable to get address from database
     */
    public String getAddressFromConnection(Connection connection, int registerNumber) throws SQLException {
        String address = null;

        //grabs the address using the registerID
        //NOTE: This is incredibly sloppy! I wasn't sure how to grab the result of a function, so I turned
        // storeAddressLookupFromRegister into a procedure and grabbed the address this way
        ps = connection.prepareStatement("CALL storeAddressLookupFromRegister(?)");
        ps.setInt(1, registerNumber);
        //stores the address in the result set
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            address = rs.getString(1);
        }

        ps.close();
        rs.close();

        return address;
    }

    /**
     * Grabs Item information with given upc value.
     *
     * @param connection Connection object
     * @param upc        long representing 12 digit upc
     * @return Item object with associated details
     * @throws SQLException if unable to find Item with associated UPC in database
     */
    public Item getItemFromUPC(Connection connection, long upc) throws SQLException {
        String name = null;
        double price = 0.0, discount = 0.0;


        String itemUPCLookup = "Call itemUPCLookup('" + upc + "')";
        ps = connection.prepareStatement(itemUPCLookup);
        //stores the address in the result set
        rs = ps.executeQuery();
        while (rs.next()) {
            name = rs.getString(1);
            price = rs.getDouble(2);
            discount = rs.getDouble(3);
        }

        ps.close();
        rs.close();

        return new Item(upc, name, price, discount);
    }

    /**
     * Creates a receipt column in database and returns the receipt's associated number.
     *
     * @param connection  Connection object
     * @param member      Member object
     * @param registerNum int register number
     * @throws SQLException if an error occurs while interacting with database
     */
    public int createReceipt(Connection connection, Member member, int registerNum) throws SQLException {
        String createReceipt;
        int receiptNumber = 0;
        double amountDue = 0.0, stateTax = 0.0;

        //if there is no provided membership
        if (member == null) {

            createReceipt = "CALL createReceipt(" + registerNum + ", null)";
        }
        //a membership was provided
        else {
            createReceipt = "CALL createReceipt('" + registerNum + "', '" + member.getAccountNumber() + "')";
        }

        ps = connection.prepareStatement(createReceipt);
        //grabs the receipt number that was created
        rs = ps.executeQuery();
        while (rs.next()) {
            receiptNumber = Integer.parseInt(rs.getString(1));
        }

        return receiptNumber;
    }

    /**
     * Uses the created receipt number to add items to receipt in database.
     * @param connection Connection object
     * @param list List of items
     * @param receiptNumber int representing associated receipt number
     * @param member Member object
     * @return double representing the amount due on the receipt
     * @throws SQLException if any error with creating receipt in database
     */
    public double getReceiptTotal(Connection connection, List<Item> list, int receiptNumber, Member member)
            throws SQLException {
        double amountDue = 0.0, stateTax = 0.0;

        //adds all the items to the receipt_details table
        for (int i = 0; i < list.size(); i++) {
            String addItem = "CALL addItemToReceipt('" + list.get(i).getUpc() + "', " + receiptNumber + ")";
            ps = connection.prepareStatement(addItem);
            ps.execute();
        }

        //SHOULD PUT LOGIC ON SERVER SIDE!!!
        for (int i = 0; i < list.size(); i++) {
            double itemAmount = list.get(i).getPrice();
            double discount = 0.0;
            //only grabs the discount if there's a given member
            if (member != null) {
                discount = list.get(i).getDiscount();
            }
            double discountedItem = itemAmount * (1 - discount);
            //adds that item price to the grand total
            amountDue += discountedItem;
        }

        //SHOULD PUT LOGIC ON SERVER SIDE!!!
        String getStateTax = "CALL getStateTax(" + receiptNumber + ")";

        ps = connection.prepareStatement(getStateTax);
        //stores the address in the result set
        rs = ps.executeQuery();
        while (rs.next()) {
            stateTax = Double.parseDouble(rs.getString(1));
        }
        //sets the amount due including state tax
        amountDue = amountDue * (1 + stateTax);
        return amountDue;
    }

    /**
     * Returns associated Member object from search using phone number in database.
     * @param connection Connection object
     * @param phoneNumber long representing phone number
     * @return associated Member object
     * @throws SQLException if unable to find associated Member
     */
    public Member getMemberFromPhoneNumber(Connection connection, long phoneNumber) throws SQLException {
        Member member = null;

        String sqlStatement = "Call memberPhoneLookup('" + phoneNumber + "')";

        ps = connection.prepareStatement(sqlStatement);
        //stores the member in the result set
        rs = ps.executeQuery();

        while (rs.next()) {
            long accountNumber = Long.parseLong(rs.getString(1));
            String firstName = rs.getString(2);
            String lastName = rs.getString(3);

            member = new Member(accountNumber, firstName, lastName);
        }

        return member;
    }

    /**
     * Returns associated Member object from search using account number in database.
     * @param connection Connection object
     * @param accountNumber long representing associated phone number
     * @return associated Member object
     * @throws SQLException if unable to find associated Member
     */
    public Member getMemberFromAccountNumber(Connection connection, long accountNumber) throws SQLException {
        Member member = null;

        String sqlStatement = "Call memberAccountNumberLookup(" + accountNumber + ")";

        ps = connection.prepareStatement(sqlStatement);
        //stores the member in the result set
        rs = ps.executeQuery();

        while (rs.next()) {
            String firstName = rs.getString(1);
            String lastName = rs.getString(2);

            member = new Member(accountNumber, firstName, lastName);
        }

        return member;
    }
}
