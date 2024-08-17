package com.github.tylerjpohlman.database.register.data_access_classes;
import com.github.tylerjpohlman.database.register.helper_classes.*;

import java.sql.*;
import java.util.List;

/**
 * Implementation of {@link JdbcUserDAO} which contains the logic for the data access object.
 * @author Tyler Pohlman
 * @version 1.0, Date Created: 2023-11-14
 * @lastModified 2023-11-24
 */
public class JdbcUserDAOImpl implements JdbcUserDAO {
    /**
     * MySQL Connection to database using login credentials
     */
    private Connection connection = null;
    /**
     * Register number used to sign in to MySQL database
     */
    private final int registerNumber;

    private PreparedStatement ps = null;
    private ResultSet rs = null;

    /**
     * Constructor with tries logging into database and establishing connection when invoked.
     * @param url String representing database url
     * @param username String representing username
     * @param password String representing password
     * @param registerNumber int representing the register number
     * @throws SQLException if there's an error logging in to database
     */
    public JdbcUserDAOImpl(String url, String username, String password, int registerNumber) throws SQLException {
        setConnectionFromLogin(url, username, password, registerNumber);
        this.registerNumber = registerNumber;
    }

    public boolean isConnectionNotReachable() {
        try {
            //checks if Connection is closed
            if (connection != null && connection.isClosed()) {
                return true;
            }
            //if there's no connection object at all
            else if (connection == null) {
                return true;
            }
            //any other Connection errors will also return false
        } catch (SQLException e) {
            return true;
        }

        //if all goes well, return true
        return false;
    }

    public void setConnectionFromLogin(String url, String username, String password, int registerNumber)
            throws SQLException {
        try {
            //tries to establish a connection to the database
            connection = DriverManager.getConnection(url, username, password);

            //tries cashier login procedure
            ps = connection.prepareStatement("CALL cashierRegisterLogin(?, ?)");
            ps.setString(1, username);
            ps.setInt(2, registerNumber);
            ps.execute();

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
                //error defined in database procedure
                //invoked when an invalid register_id is given
                case "45001":
                    throw new InvalidRegisterException();
                //error defined in database procedure
                //basically, this error is invoked when an invalid cashier number is given
                //VERY UNLIKELY TO BE THROWN IF LOGIN CREDENTIALS WORKED
                case "45000":
                    throw new InvalidCredentialsException();
                default:
                    throw e;
            }
        }
    }

    public String getAddressFromConnection() throws SQLException {
        String address = null;

        try {
            //grabs the address using the registerID
            ps = connection.prepareStatement("SELECT storeAddressLookupFromRegister(?)");
            ps.setInt(1, registerNumber);
            //stores the address in the result set
            rs = ps.executeQuery();
            while (rs.next()) {
                address = rs.getString(1);
            }
        } catch (SQLException e) {
            //invalid register id given
            if(e.getSQLState().equals("45001")) {
                throw new InvalidRegisterException();
            //otherwise, rethrow the SQLException
            } else {
                throw e;
            }
        }

        return address;
    }

    public Item getItemFromUPC(long upc) throws SQLException {
        String name = null;
        double price = 0.0, discount = 0.0;

        try {
            ps = connection.prepareStatement("CALL itemUPCLookup(?)");
            ps.setLong(1, upc);
            //stores the address in the result set
            rs = ps.executeQuery();

            while (rs.next()) {
                name = rs.getString(1);
                price = rs.getDouble(2);
                discount = rs.getDouble(3);
            }
        } catch (SQLException e) {
            //invalid UPC exception defined in database
            if(e.getSQLState().equals("45002")) {
                throw new InvalidUPCException();
            }
            //otherwise, rethrow the exception
            else {
                throw e;
            }
        }

        ps.close();
        rs.close();

        return new Item(upc, name, price, discount);
    }

    public int createReceipt(Member member) throws SQLException {

        int receiptNumber = 0;

        //if there is no provided membership
        if (member == null) {
            ps = connection.prepareStatement("CALL createReceipt(?, ?)");
            ps.setInt(1, registerNumber);
            ps.setNull(2, java.sql.Types.INTEGER);
        }
        //a membership was provided
        else {
            ps = connection.prepareStatement("CALL createReceipt(?, ?)");
            ps.setInt(1, registerNumber);
            ps.setLong(2, member.getAccountNumber());
        }

        //grabs the receipt number that was created
        rs = ps.executeQuery();
        while (rs.next()) {
            receiptNumber = Integer.parseInt(rs.getString(1));
        }

        ps.close();
        rs.close();

        return receiptNumber;
    }

    public double getReceiptTotal(List<Item> list, int receiptNumber, Member member)
            throws SQLException {
        double amountDue = 0.0;

        //adds all the items to the receipt_details table
        for (Item item : list) {
            ps = connection.prepareStatement("CALL addItemToReceipt(?,?)");
            ps.setLong(1, item.getUpc());
            ps.setInt(2, receiptNumber);
            ps.execute();
        }

        if (member == null) {
            ps = connection.prepareStatement("SELECT getReceiptTotal(?,?)");
            ps.setInt(1, receiptNumber);
            ps.setNull(2, java.sql.Types.INTEGER);
        }
        else {
            ps = connection.prepareStatement("SELECT getReceiptTotal(?,?)");
            ps.setInt(1, receiptNumber);
            ps.setLong(2, member.getAccountNumber());
        }
        rs = ps.executeQuery();
        while(rs.next()) {
            amountDue = rs.getDouble(1);
        }

        ps.close();
        rs.close();

        return amountDue;
    }

    public Member getMemberFromPhoneNumber(long phoneNumber) throws SQLException {
        Member member = null;

        ps = connection.prepareStatement("Call memberPhoneLookup(?)");
        ps.setLong(1, phoneNumber);
        //stores the member in the result set
        rs = ps.executeQuery();


        while (rs.next()) {
            long accountNumber = Long.parseLong(rs.getString(1));
            String firstName = rs.getString(2);
            String lastName = rs.getString(3);

            member = new Member(accountNumber, firstName, lastName);
        }

        ps.close();
        rs.close();

        return member;
    }

    public Member getMemberFromAccountNumber(long accountNumber) throws SQLException {
        Member member = null;

        ps = connection.prepareStatement("Call memberAccountNumberLookup(?)");
        ps.setLong(1, accountNumber);
        //stores the member in the result set
        rs = ps.executeQuery();

        while (rs.next()) {
            String firstName = rs.getString(1);
            String lastName = rs.getString(2);

            member = new Member(accountNumber, firstName, lastName);
        }

        ps.close();
        rs.close();

        return member;
    }

    public double finalizeReceipt(double amountPaid, double amountDue, long receiptNumber)
            throws SQLException {
        if(isConnectionNotReachable()) {
            throw new ClosedConnectionException();
        }

        ps = connection.prepareStatement("CALL finalizeReceipt(?,?)");
        ps.setLong(1, receiptNumber);
        ps.setDouble(2, amountPaid);
        rs = ps.executeQuery();
        
        double amountGiven = 0;
        
        while(rs.next()) {
            amountGiven = rs.getDouble(1);
        }

        ps.close();
        rs.close();

        
        return amountGiven;
    }
}
