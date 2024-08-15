-- *******************************************************
-- FUNCTIONS/PROCEDURES SPECIFIC TO FRONT-END APPLICATIONS
-- *******************************************************

-- --------------------
-- cashierRegisterLogin
-- Used to add a row to cashier_assignments to indicate a given register is assigned to a given cashier.
-- @EXCEPTION SQLSTATE '45000' if cashier_id isn't found
-- @EXCEPTION SQLSTATE '45001' if register_id isn't found
-- --------------------
DROP PROCEDURE IF EXISTS cashierRegisterLogin;
DELIMITER //
CREATE PROCEDURE cashierRegisterLogin(
    given_cashier_id INT,
    given_register_id INT
)
BEGIN
    -- creates exception for invalid cashier_id
    DECLARE no_such_cashier_id CONDITION FOR SQLSTATE '45000';
    -- create exception for invalid register_id
    DECLARE no_such_register CONDITION FOR SQLSTATE '45001';
    -- Check for valid cashier_id
    IF given_cashier_id NOT IN (SELECT cashier_id FROM cashiers) THEN
        SIGNAL no_such_cashier_id SET MESSAGE_TEXT = 'No such cashier_id exists';
    END IF;
    -- Check for valid register_id
    IF given_register_id NOT IN (SELECT register_id FROM registers) THEN
        SIGNAL no_such_register SET MESSAGE_TEXT = 'No such register_id exists';
    END IF;

    UPDATE cashier_assignments
    SET cashier_id = (SELECT cashier_id FROM cashiers WHERE cashier_id = given_cashier_id)
    WHERE register_id = (SELECT register_id FROM registers WHERE register_id = given_register_id);
END //
DELIMITER ;

-- ------------------------------
-- storeAddressLookupFromRegister
-- Grabs the full address of a store from its associated register.
-- @EXCEPTION SQLSTATE '45001' if register_id isn't found
-- ------------------------------
DROP FUNCTION IF EXISTS storeAddressLookupFromRegister;
DELIMITER //
CREATE FUNCTION storeAddressLookupFromRegister(
    given_register_id INT
)
RETURNS VARCHAR(150)
DETERMINISTIC
BEGIN
    -- create exception for invalid register_id
    DECLARE no_such_register CONDITION FOR SQLSTATE '45001';
    -- declare variable to return
    DECLARE store_address VARCHAR(150);
    
    IF given_register_id NOT IN (SELECT register_id FROM registers) THEN
        SIGNAL no_such_register SET MESSAGE_TEXT = 'No such register_id exists';
    END IF;
    
    SET store_address =
    (
    SELECT CONCAT(store_address, ', ', store_city, ', ', store_state, ' ', store_zip)
    FROM stores
    WHERE store_id = (SELECT store_id FROM registers WHERE register_id = given_register_id)
    );
    RETURN(store_address);
END //
DELIMITER ;

-- -------------
-- itemUPCLookup
-- Grabs item_name, item_upc, and item_discount for a given upc.
-- @EXCEPTION SQLSTATE '45002' if item_upc doesn't exist
-- -------------
DROP PROCEDURE IF EXISTS itemUPCLookup;
DELIMITER //
CREATE PROCEDURE itemUPCLookup(
    -- upc is only 12 characters long
    given_upc VARCHAR(12)
)
BEGIN
    -- creates exception for invalid upc
    DECLARE no_such_upc CONDITION FOR SQLSTATE '45002';
    IF given_upc NOT IN (SELECT item_upc FROM items) THEN
        SIGNAL no_such_upc SET MESSAGE_TEXT = 'No such item_upc exists';
    END IF;

    SELECT item_name, item_price, item_discount_percentage
    FROM items
    WHERE item_upc = given_upc;
END //
DELIMITER ;

-- -------------
-- createReceipt
-- Begins the creation of a receipt populating the details of a new row in receipts given the register and member id.
-- @EXCEPTION SQLSTATE '45001' if register_id isn't found
-- @EXCEPTION SQLSTATE '45003' if member_id isn't found
-- -------------
DROP PROCEDURE IF EXISTS createReceipt;
DELIMITER //
CREATE PROCEDURE createReceipt(
    given_register_id INT,
    given_member_id INT
)
BEGIN
    -- create exception for invalid register_id
    DECLARE no_such_register CONDITION FOR SQLSTATE '45001';
    -- create exception for invalid member_id
    DECLARE no_such_member CONDITION FOR SQLSTATE '45003';
    IF given_register_id NOT IN (SELECT register_id FROM registers) THEN
        SIGNAL no_such_register SET MESSAGE_TEXT = 'No such register_id exists';
	END IF;
    IF given_member_id NOT IN (SELECT member_id FROM members) THEN
        SIGNAL no_such_member SET MESSAGE_TEXT = 'No such member_id exists';
	END IF;

    INSERT INTO receipts (register_id, member_id, receipt_date_time, receipt_cashier_full_name)
    VALUES
    (
    given_register_id,
    given_member_id,
    -- null for now before items are added
    null,
    receiptsCashierName((SELECT register_id FROM registers WHERE register_id = given_register_id))

    );

    -- returns the PRIMARY KEY value of the last row inserted
    -- is per user and is unaffected by other queries that might be running on the server from other users
    SELECT LAST_INSERT_ID();
END //
DELIMITER;

-- ----------------
-- addItemToReceipt
-- Used to add a given item based on the upc to receipt_details.
-- @EXCEPTION SQLSTATE '45002' if item_upc doesn't exist
-- @EXCEPTION SQLSTATE '45004' if receipt_id doesn't exist
-- ----------------
DROP PROCEDURE IF EXISTS addItemToReceipt;
DELIMITER //
CREATE PROCEDURE addItemToReceipt(
    given_upc VARCHAR(20),
    given_receipt_id INT
)
BEGIN
     -- creates exception for invalid upc
      DECLARE no_such_upc CONDITION FOR SQLSTATE '45002';
      -- creates exception for invalid receipt
      DECLARE no_such_receipt_id CONDITION FOR SQLSTATE '45004';
      IF given_upc NOT IN (SELECT item_upc FROM items) THEN
        SIGNAL no_such_upc SET MESSAGE_TEXT = 'No such item_upc exists';
      END IF;
      IF given_receipt_id NOT IN (SELECT receipt_id FROM receipts) THEN
        SIGNAL no_such_receipt_id SET MESSAGE_TEXT = 'No such receipt_id exists';
      END IF;

    INSERT INTO receipt_details (receipt_id, item_id, item_total, item_discount_percentage, item_price, item_quantity)
    VALUES
    (
    given_receipt_id,
    itemIDFromUPC(given_upc),
    null,
    detailsDiscount(given_receipt_id, itemIDFromUPC(given_upc)),
    detailsPrice(given_receipt_id, itemIDFromUPC(given_upc)),
    1
    );

    UPDATE inventory
    -- remove 1 item from inventory
    SET inventory.item_qty = inventory.item_qty - 1
    WHERE item_id = itemIDFromUPC(given_upc) AND
    store_id = storeIDFromReceiptID(given_receipt_id);
END //

-- ---------------
-- getReceiptTotal
-- Tallies up the totals for all the associated receipt_details, updates that receipt, and returns the total.
-- @EXCEPTION SQLSTATE '45004' if receipt_id doesn't exist
-- @EXCEPTION SQLSTATE '45003' if member_id isn't found
-- ---------------
DROP FUNCTION IF EXITS getReceiptTotal;
DELIMITER //
CREATE FUNCTION getReceiptTotal(
    given_receipt_id INT,
    given_member_id INT
)
RETURNS DECIMAL(12,2)
DETERMINISTIC
BEGIN
      -- creates exception for invalid receipt
      DECLARE no_such_receipt_id CONDITION FOR SQLSTATE '45004';
      -- create exception for invalid member_id
      DECLARE no_such_member CONDITION FOR SQLSTATE '45003';
      -- declare variable to return
      DECLARE given_receipt_total DECIMAL(12,2);
      IF given_receipt_id NOT IN (SELECT receipt_id FROM receipts) THEN
        SIGNAL no_such_receipt_id SET MESSAGE_TEXT = 'No such receipt_id exists';
      END IF;
      IF given_member_id NOT IN (SELECT member_id FROM members) THEN
        SIGNAL no_such_member SET MESSAGE_TEXT = 'No such member_id exists';
      END IF;
      
      -- update the subtotal
      UPDATE receipts
      SET receipt_subtotal = (SElECT SUM(item_total) FROM receipt_details WHERE receipt_id = given_receipt_id)
      WHERE receipt_id = given_receipt_id;
      
      -- update total including tax
      UPDATE receipts
      SET receipt_total = receipt_subtotal * (1 + receiptsStateTax(given_receipt_id))
      WHERE receipt_id = given_receipt_id;
      
      SET given_receipt_total = (SELECT receipt_total FROM receipts WHERE receipt_id = given_receipt_id);

      RETURN(given_receipt_total);
END //
DELIMITER ;

-- --------------
-- finalizeReceipt
-- @EXCEPTION SQLSTATE '45004' if receipt_id doesn't exist
-- @EXCEPTION SQLSTATE '45005' if given_cash is less than total
-- ---------------
DROP PROCEDURE IF EXISTS finalizeReceipt;
DELIMITER //
CREATE PROCEDURE finalizeReceipt(
    given_receipt_id INT,
    given_cash DECIMAL(9,2)
)
BEGIN
	  -- used for comparisons
	  DECLARE actual_receipt_total DECIMAL(9,2);
	  -- creates exception for invalid receipt
      DECLARE no_such_receipt_id CONDITION FOR SQLSTATE '45004';
	  -- spit out an error if given_cash is less than receipt_total
      DECLARE invalid_payment_amount CONDITION FOR SQLSTATE '45005';
      IF given_receipt_id NOT IN (SELECT receipt_id FROM receipts) THEN
		SIGNAL no_such_receipt_id SET MESSAGE_TEXT = 'No such receipt_id exists';
      END IF;
	  SELECT receipt_total INTO actual_receipt_total FROM receipts WHERE receipt_id = given_receipt_id;
      IF given_cash < actual_receipt_total THEN
		SIGNAL invalid_payment_amount SET MESSAGE_TEXT = 'Amount must be >= total';
	  END IF;
      
	
    UPDATE receipts
    SET receipt_date_time = NOW()
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_charge = given_cash
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_change_due = receipt_charge - receipt_total
    WHERE receipt_id = given_receipt_id;
END //
DELIMITER ;

-- -----------------
-- memberPhoneLookup
-- -----------------
DELIMITER //
CREATE PROCEDURE memberPhoneLookup(
    given_phone_number VARCHAR(16)
)
BEGIN
    -- creates exception member no matching member is found
    DECLARE no_such_member CONDITION FOR SQLSTATE '45000';

    IF given_phone_number NOT IN (SELECT member_phone_number FROM members)
    THEN
        SIGNAL no_such_member SET MESSAGE_TEXT = 'No such phone_number exists';
    END IF;

    SELECT member_id, member_first_name, member_last_name
    FROM members
    WHERE member_phone_number = given_phone_number;
END //
DELIMITER ;
-- -------------------------
-- memberAccountNumberLookup
-- -------------------------
DELIMITER //
CREATE PROCEDURE memberAccountNumberLookup(
    given_member_id INT
)
BEGIN
    -- creates exception member no matching member is found
    DECLARE no_such_account_num CONDITION FOR SQLSTATE '45000';

    IF given_member_id NOT IN (SELECT member_id FROM members)
    THEN
        SIGNAL no_such_account_num SET MESSAGE_TEXT = 'No such account_number exists';
    END IF;

    SELECT member_first_name, member_last_name
    FROM members
    WHERE member_id = given_member_id;
END //
DELIMITER ;
-- --------------
-- finalizeReceipt
-- ---------------
DROP PROCEDURE IF EXISTS finalizeReceipt;
DELIMITER //
CREATE PROCEDURE finalizeReceipt(
    given_receipt_id INT,
    given_cash DECIMAL(9,2)
)
BEGIN
    -- spit out an error if given_cash is less than receipt_total

    UPDATE receipts
    SET receipt_subtotal
    = (SELECT SUM(item_total) FROM receipt_details WHERE receipt_id = given_receipt_id)
    -- avoid wasting time and only change the receipt_detail that correspond to the receipt
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_total = receipt_subtotal * (1 + receiptsStateTax(given_receipt_id))
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_date_time = NOW()
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_charge = given_cash
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_change_due = receipt_charge - receipt_total
    WHERE receipt_id = given_receipt_id;
END //
DELIMITER ;
-- **************************************************************
-- HELPER FUNCTIONS/PROCEDURES SPECIFIC TO FRONT-END APPLICATIONS
-- **************************************************************








-- cashierRegisterLogoff
DELIMITER //
CREATE PROCEDURE cashierRegisterLogoff(
    given_register_id INT
)
BEGIN
    -- creates exception for invalid register_id and/or cashier_id
    DECLARE no_such_register CONDITION FOR SQLSTATE '45000';
    IF given_register_id NOT IN (SELECT register_id FROM registers) THEN
        SIGNAL no_such_register SET MESSAGE_TEXT = 'No such register_id exists';
    END IF;

    UPDATE cashier_assignments
    SET cashier_id = null
    WHERE register_id = (SELECT register_id FROM registers WHERE register_id = given_register_id);
END //
DELIMITER ;
-- storeAddressLookupFromRegister
DELIMITER //
CREATE PROCEDURE storeAddressLookupFromRegister(
    given_register_id INT
)
BEGIN
    SELECT CONCAT(store_address, ', ', store_city, ', ', store_state, ' ', store_zip)
    FROM stores
    WHERE store_id = (SELECT store_id FROM registers WHERE register_id = given_register_id)
    ;

END //
DELIMITER ;

-- memberAccountNumberLookup
DELIMITER //
CREATE PROCEDURE memberAccountNumberLookup(
    given_member_id INT
)
BEGIN
    -- creates exception member no matching member is found
    DECLARE no_such_account_num CONDITION FOR SQLSTATE '45000';

    IF given_member_id NOT IN (SELECT member_id FROM members)
    THEN
        SIGNAL no_such_account_num SET MESSAGE_TEXT = 'No such account_number exists';
    END IF;

    SELECT member_first_name, member_last_name
    FROM members
    WHERE member_id = given_member_id;
END //
DELIMITER ;

-- cancelReceipt
-- Here in case a receipt is cancelled--i.e, someone doesn't have enough to pay
DROP PROCEDURE IF EXISTS cancelReceipt;
DELIMITER //
CREATE PROCEDURE cancelReceipt(
    given_receipt_id INT
)
BEGIN
    -- returned_inventory_id is AUTO INCREMENT and DATETIME is NOW()
    INSERT INTO returned_inventory (item_id, store_id, employee, item_qty)
    -- multiple rows are added into returned_inventory table
    SELECT DISTINCT item_id, storeIDFromReceiptID(given_receipt_id), CURRENT_USER(), COUNT(item_id)
    FROM receipt_details
    WHERE receipt_id = given_receipt_id
    GROUP BY item_id;

    -- remove all associated receipt details
    DELETE FROM receipt_details
    WHERE receipt_id = given_receipt_id;

    -- remove from receipts table
    DELETE FROM receipts
    WHERE receipt_id = given_receipt_id;
END //
DELIMITER ;
-- finalizeReceipt
DROP PROCEDURE IF EXISTS finalizeReceipt;
DELIMITER //
CREATE PROCEDURE finalizeReceipt(
    given_receipt_id INT,
    given_cash DECIMAL(9,2)
)
BEGIN
    -- spit out an error if given_cash is less than receipt_total

    UPDATE receipts
    SET receipt_subtotal
    = (SELECT SUM(item_total) FROM receipt_details WHERE receipt_id = given_receipt_id)
    -- avoid wasting time and only change the receipt_detail that correspond to the receipt
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_total = receipt_subtotal * (1 + receiptsStateTax(given_receipt_id))
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_date_time = NOW()
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_charge = given_cash
    WHERE receipt_id = given_receipt_id;

    UPDATE receipts
    SET receipt_change_due = receipt_charge - receipt_total
    WHERE receipt_id = given_receipt_id;
END //
DELIMITER ;
