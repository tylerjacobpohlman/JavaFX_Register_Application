package com.github.tylerjpohlman.database.register.data_access_classes;
import com.github.tylerjpohlman.database.register.helper_classes.*;

import java.sql.*;
import java.util.List;

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

    public boolean isConnectionReachable() {
        try {
            //checks if Connection is closed
            if (connection != null && connection.isClosed()) {
                return false;
            }
            //if there's no connection object at all
            else if (connection == null) {
                return false;
            }
            //any other Connection errors will also return false
        } catch (SQLException e) {
            return false;
        }

        //if all goes well, return true
        return true;
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
                    //error defined in database: "no such register_id and/or cashier_id exists"
                    //basically, this error is invoked when an invalid register number is given
                case "45000":
                    throw new InvalidRegisterException();
                default:
                    throw new SQLException(e);
            }
        }
    }

    public String getAddressFromConnection() throws SQLException {
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

    public Item getItemFromUPC(long upc) throws SQLException {
        String name = null;
        double price = 0.0, discount = 0.0;

        ps = connection.prepareStatement("Call itemUPCLookup(?)");
        ps.setString(1, String.valueOf(upc));
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
        //TURN INTO FUNCTION ON DATABASE SIDE!!!
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
        double amountDue = 0.0, stateTax = 0.0;

        //adds all the items to the receipt_details table
        for (int i = 0; i < list.size(); i++) {
            ps = connection.prepareStatement("CALL addItemToReceipt(?,?)");
            ps.setLong(1, list.get(i).getUpc());
            ps.setInt(2, receiptNumber);
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

        ps = connection.prepareStatement("CALL getStateTax(?)");
        ps.setInt(1, receiptNumber);
        //stores the address in the result set
        rs = ps.executeQuery();
        while (rs.next()) {
            stateTax = Double.parseDouble(rs.getString(1));
        }
        //sets the amount due including state tax
        amountDue = amountDue * (1 + stateTax);

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

        //ALSO NEED TO CHANGE TO FUNCTION ON SERVER SIDE
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

        //CHANGE TO FUNCTION ON SERVER SIDE
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
            throws SQLException, IllegalArgumentException {
        if(!isConnectionReachable()) {
            throw new ClosedConnectionException();
        }

        //WANT TO ADD LOGIC TO SERVER SIDE IN FUTURE!
        if (amountPaid < amountDue) {
            throw new IllegalArgumentException("amountPaid must be greater than or equal to amountDue");
        }

        ps = connection.prepareStatement("CALL finalizeReceipt(?,?)");
        ps.setLong(1, receiptNumber);
        ps.setDouble(2, amountPaid);
        ps.execute();

        ps.close();
        rs.close();

        //ADD ROUNDING TO SERVER SIDE!
        return Double.parseDouble(String.format("%.2f", amountPaid - amountDue));
    }
}
