-- something, something can't update w/o check for key in where clause
SET SQL_SAFE_UPDATES = 0;
-- I have no clue why the database refuses to connect to Java without this line.
SET GLOBAL time_zone = '+3:00';
-- I don't know why I put this here, but I don't want to remove it and everything breaks.
FLUSH PRIVILEGES;

-- ***********************************
-- CREATE AND SELECTS THE HVS DATABASE
-- ***********************************
DROP DATABASE IF EXISTS hvs;
CREATE DATABASE hvs;
USE hvs;

-- *************************
-- CREATE TABLES AND INDEXES
-- *************************
-- ------------
-- TABLE States
-- Stores information about the tax rate of each store code.
-- ------------
CREATE TABLE states
(
    -- can act as the primary key since CHAR(2) isn't overly complex
    -- also, makes it easier for inputting state into stores table
	state_name CHAR(2) PRIMARY KEY,
    -- each state must have a specified tax percentage
    state_tax_percentage DECIMAL(2,2) NOT NULL
);
-- ------------
-- TABLE stores
-- Used to store information about each store.
-- *WARNING* must create table states first
-- ------------
CREATE TABLE stores
(
	store_id INT PRIMARY KEY AUTO_INCREMENT,
    store_address VARCHAR(50),
    store_city VARCHAR(50),
	-- for state taxes
    store_state CHAR(2) NOT NULL,
    store_zip VARCHAR(20),
	-- no 2 stores can have the same number, and phone number is numerically important
    store_phone VARCHAR(20) UNIQUE,
    CONSTRAINT stores_fk_states FOREIGN KEY (store_state) REFERENCES states(state_name)
);
-- --------------
-- TABLE cashiers
-- Stores information about each cashier.
-- *WARNING* must create stores table first
-- Related to TABLE 'stores' in which each cashier has an associated store.
-- --------------
CREATE TABLE cashiers
(
	cashier_id INT PRIMARY KEY AUTO_INCREMENT,
    -- foreign key, every cashier must be assigned to a store
    store_id INT NOT NULL,
    -- cashiers might share the same name, so no need for UNIQUE
    cashier_first_name VARCHAR(32),
    cashier_last_name VARCHAR(32),
    cashier_password VARCHAR(32),
    CONSTRAINT cashiers_fk_stores FOREIGN KEY (store_id) REFERENCES stores (store_id)
);
-- ---------------
-- TABLE registers
-- Stores information about each register
-- *WARNING* must create the stores table first
-- Related to TABLE 'store' in which each register has an associated store.
-- ---------------
CREATE TABLE registers
(
	register_id INT PRIMARY KEY AUTO_INCREMENT,
    -- foreign key
    store_id INT NOT NULL,
    -- the type is important because self always use a SELF HELP cashier
    register_type ENUM('Self', 'Clerk', 'Other'),
    -- The relationship to the stores table isn't necessarily needed, it just speeds up inquiries since going from
    -- registers to stores is faster than going from registers, to cashier_assignments, to cashiers, and then to store.
    -- Likewise, cashiers come and go, so having the relationship between stores and registers depend on cashiers can
    -- cause issues.
    CONSTRAINT registers_fk_stores FOREIGN KEY (store_id) REFERENCES stores(store_id)
);
-- -------------------------
-- TABLE cashier_assignments
-- Used to link the relationship between cashiers and registers. One cashier is assigned to one register.
-- *WARNING* must create cashiers and registers tables first
-- This table is implemented in such a way that it is updated frequently--i.e., cashiers are logging into and
-- logging out of registers many times a day.
-- -------------------------
CREATE TABLE cashier_assignments
(
    -- automatically NOT NULL and UNIQUE
    -- setup in such a way that there can be used at a time
	register_id INT PRIMARY KEY,
    -- setup in such a way that a cashier can be assigned to multiple registers
    -- Also, NULL value means the register is unassigned
    cashier_id INT,
    CONSTRAINT details_fk_cashiers FOREIGN KEY (cashier_id) REFERENCES cashiers(cashier_id),
    CONSTRAINT details_fk_registers FOREIGN KEY (register_id) REFERENCES registers(register_id)
);
-- -----------
-- TABLE items
-- Stores information relevant to an item.
-- -----------
CREATE TABLE items
(
	item_id INT PRIMARY KEY AUTO_INCREMENT,
    -- refers to the barcode for items at the store
    -- each barcode is unique and the item must have a barcode in order to check out
    item_upc VARCHAR(20) NOT NULL UNIQUE,
    -- each item is unique, so there shouldn't be duplicates
    item_name VARCHAR(200) NOT NULL UNIQUE,
    -- up to $99,999.99 for a single item
    -- default ensures no issues when calculations are done on the entire column
    item_price DECIMAL(9,2) DEFAULT 0.00,
    -- range from 0% to 99%
    item_discount_percentage DECIMAL(2,2) DEFAULT 0.00
);
-- items are scanned in order to pull it up from the table, so this index speeds up that process
CREATE INDEX idx_upc
ON items (item_upc);
-- -------------
-- TABLE members
-- Basically, this stores whomever rewards members are. Rewards are able to access the given savings,
-- while nonmembers always pay the full price.
-- -------------
CREATE TABLE members
(
	member_id INT PRIMARY KEY AUTO_INCREMENT,
    member_first_name VARCHAR(32),
    member_last_name VARCHAR(32),
    -- multiple rewards account can't share the same credentials
    member_phone_number VARCHAR(16) UNIQUE,
    member_email_address VARCHAR(64) UNIQUE,
    -- accumulative total of savings
    member_total_savings DECIMAL(12,2) DEFAULT 0.00
);
-- ---------------
-- INDEX idx_phone
-- Phone number is used to look up rewards membership, so it is indexed.
-- ---------------
CREATE INDEX idx_phone
ON members (member_phone_number);
-- --------------
-- TABLE receipts
-- Used to store preliminary information about a receipt. List of items specific to receipt is found in receipt_details.
-- *WARNING* must create registers, states, and members tables first
-- --------------
CREATE TABLE receipts
(
	receipt_id INT PRIMARY KEY AUTO_INCREMENT,
    -- foreign keys
    register_id INT NOT NULL,
    -- foreign key, but can be null if customer isn't a member
    member_id INT,
    -- the time and date of purchase
    receipt_date_time DATETIME DEFAULT NOW(),
    receipt_subtotal DECIMAL(9,2) DEFAULT 0.0,
    receipt_total DECIMAL(9,2) DEFAULT 0.0,
    receipt_charge DECIMAL(9,2) DEFAULT 0.0,
    receipt_change_due DECIMAL(9,2) DEFAULT 0.0,
    -- could use JOIN statements to get the same info, but indexing the database takes more time--i.e.,
    -- might as well store this value here too since it's unchanging
    receipt_cashier_full_name VARCHAR(128) NOT NULL,
    CONSTRAINT receipts_fk_registers FOREIGN KEY (register_id) REFERENCES registers(register_id),
    CONSTRAINT receipts_fk_members FOREIGN KEY (member_id) REFERENCES members(member_id)
);
-- ---------------------
-- TABLE receipt_details
-- Store information about the items for each receipt. Is the link between receipts and items.
-- *WARNING* must create receipts table first
-- ---------------------
CREATE TABLE receipt_details
(
	-- foreign keys
	receipt_id INT NOT NULL,
    item_id INT NOT NULL,

    -- total after discount is applied (or not, based on membership details)
    item_total DECIMAL(9,2) DEFAULT 0.0,

    --
    -- The following are copied from the items table since these values can change in that table.
    --
    item_price DECIMAL(9,2) DEFAULT 0.00,
    item_discount_percentage DECIMAL(2,2) DEFAULT 0.00,

    CONSTRAINT details_fk_receipts FOREIGN KEY (receipt_id) REFERENCES receipts(receipt_id),
    CONSTRAINT details_fk_items FOREIGN KEY (item_id) REFERENCES items(item_id)
);
-- -------------------------------
-- TABLE cashier_assignment_audits
-- Used to log what cashier was on what register.
-- -------------------------------
CREATE TABLE cashier_assignments_audit
(
    -- stores the assignments
    register_id INT,
    cashier_id INT,
    -- stores the date of the change and what type change took place
    action_type ENUM('Sign in', 'Sign out'),
    action_date DATETIME DEFAULT NOW()
);
-- ------------------------------------
-- TABLE accumulative_sales_per_product
-- Stores number of items sold and the total revenue generated.
-- ------------------------------------
CREATE TABLE accumulative_sales_per_product
(
    item_id INT,
    sold_qty INT DEFAULT 0,
    total_sales DECIMAL(10,2) DEFAULT 0.0
);
-- ---------------
-- TABLE inventory
-- Stores the specify quantity of items at each store.
-- ---------------
CREATE TABLE inventory (
    store_id INT,
    item_id INT,
    item_qty INT NOT NULL,
    CONSTRAINT pk_inventory PRIMARY KEY (store_id, item_id),
    CONSTRAINT inventory_fk_stores FOREIGN KEY (store_id) REFERENCES stores(store_id),
    CONSTRAINT inventory_fk_items FOREIGN KEY (item_id) REFERENCES items(item_id)
);
-- ------------------------
-- TABLE returned_inventory
-- Stores information about returns.
-- ------------------------
CREATE TABLE returned_inventory (
    -- each returned item has a unique return id
    returned_inventory_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL,
    item_id INT NOT NULL,
    item_qty INT NOT NULL,
    -- used to store CURRENT_USER()
    employee VARCHAR(64) NOT NULL,
    date_time DATETIME DEFAULT NOW()
);

-- ***********************
-- CREATE HELPER FUNCTIONS
-- ***********************
-- ---------------
-- detailsDiscount
-- Returns the discount percentage given for a given item_id and it's associated receipt_id.
-- ---------------
DELIMITER //
CREATE FUNCTION detailsDiscount(
    receipt_id_search INT,
    item_id_search INT
)
RETURNS DECIMAL(2,2)
DETERMINISTIC
BEGIN
	DECLARE discount DECIMAL(2,2);

    -- subquery to grab member_id
    IF (SELECT member_id FROM receipts WHERE receipt_id = receipt_id_search) IS NOT NULL THEN
		-- subquery if true, sets the discount to the item discount
		SET discount = (SELECT item_discount_percentage FROM items WHERE item_id = item_id_search);
    ELSEIF (SELECT member_id FROM receipts WHERE receipt_id = receipt_id_search) IS NULL THEN
		-- sets the discount percentage to 0% if false
		SET discount = 0.00;
    END IF;

    RETURN(discount);
END //
DELIMITER ;
-- ---------
-- itemPriceFromID
-- Returns the price of an item before an discount is applied.
-- ---------
DELIMITER //
CREATE FUNCTION itemPriceFromID(
	item_id_search INT
)
RETURNS DECIMAL(9,2)
DETERMINISTIC
BEGIN
	DECLARE price DECIMAL(9,2);
    SELECT item_price INTO price FROM items WHERE item_id = item_id_search;
    RETURN(price);
END //
DELIMITER ;
-- ------------
-- detailsPrice
-- Gets the price of an item for receipt_details given the receipt_id and item_id. Includes any possible discounts.
-- *WARNING* must create detailsDiscount procedure first
-- ------------
DELIMITER //
CREATE FUNCTION detailsPrice(
    receipt_id_search INT,
    item_id_search INT
)
RETURNS DECIMAL(9,2)
DETERMINISTIC
BEGIN
    DECLARE price DECIMAL(9,2);

    -- calculates the price with the given discount
    -- subquery grabs the price of the given item
    SET price = (SELECT item_price FROM items WHERE item_id = item_id_search)
    * (1 - detailsDiscount(receipt_id_search, item_id_search) );

    RETURN(price);

END //
DELIMITER ;
-- ----------------
-- receiptsStateTax
-- Find the tax rate percentage based on the receipt_id.
-- ----------------
DELIMITER //
CREATE FUNCTION receiptsStateTax(
    receipt_id_search INT
)
RETURNS DECIMAL(2,2)
DETERMINISTIC
BEGIN
    DECLARE tax DECIMAL(2,2);

    SET tax =
    (
    SELECT state_tax_percentage
    FROM states
        JOIN stores ON states.state_name = stores.store_state
        JOIN registers ON stores.store_id = registers.store_id
        JOIN receipts ON registers.register_id = receipts.register_id
    WHERE receipt_id = receipt_id_search
    );

    RETURN(tax);
END //
DELIMITER ;
-- -------------------
-- receiptsCashierName
-- Finds the cashier's name given the register_id.
-- Function is only ever used when a new receipt is created, so the assignment is current and accurate.
-- -------------------
DELIMITER //
CREATE FUNCTION receiptsCashierName(
    register_id_search INT
)
RETURNS VARCHAR(128)
DETERMINISTIC
BEGIN
    DECLARE name VARCHAR(128);

    SET name =
    (
    SELECT CONCAT(cashier_first_name, ' ', cashier_last_name)
    -- joins the cashiers and cashier_assignments tables
    FROM cashier_assignments ca JOIN cashiers c ON ca.cashier_id = c.cashier_id
    -- checks if the date is during a previous assignment range, or checks if the date is during a current assignment
    WHERE register_id = register_id_search
    );

    RETURN(name);
END //
DELIMITER ;
-- -------------
-- itemIDFromUPC
-- Finds the associated item_id given the upc.
-- -------------
DROP FUNCTION IF EXISTS itemIDFromUPC;
DELIMITER //
CREATE FUNCTION itemIDFromUPC(
    given_upc VARCHAR(20)
)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE id INT;
    SET id = (SELECT item_id FROM items WHERE item_upc = given_upc);

    RETURN(id);
END //
DELIMITER ;
-- --------------------
-- storeIDFromReceiptID
-- Finds an associated receipt_id given the store_id.
-- --------------------
DROP FUNCTION IF EXISTS storeIDFromReceiptID
DELIMITER //
CREATE FUNCTION storeIDFromReceiptID(
    given_store_id INT
)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE id INT;
    SET id =
    (
        SELECT s.store_id
		FROM receipts rec
			JOIN registers reg ON rec.register_id = reg.register_id
			JOIN stores s ON s.store_id = reg.store_id
        WHERE rec.receipt_id = given_store_id
    );

    RETURN(id);
END //
DELIMITER ;

-- *******************************************************************************************
-- The following have simple insert statements since they lack concrete foreign key restraints
-- *******************************************************************************************
INSERT INTO states
VALUES
('OH', 0.08),
('KY', 0.04),
('NY', 0.10),
('AK', 0.02),
('PA', 0.07)
;
INSERT INTO stores (store_id, store_address, store_city, store_state, store_zip, store_phone)
VALUES
(3329, '11706 Clifton Boulevard 117th & Clifton', 'Lakewood', 'OH', '44107', '(216) 228-9296'),
(3301, '28100 Chagrin Blvd', 'Woodmere', 'OH', '44122', '(216) 831-1466'),
(5759, '3950 Turkeyfoot Rd', 'Erlanger', 'KY', '41018', '(859) 647-6211'),
(2558, '360 6th Avenue', 'New York City', 'NY', '10011', '(212) 375-9401'),
(3999, '401 Chestnut St.', 'Carnegie', 'PA', '15106', '(412) 279-5020')
;
INSERT INTO cashiers (store_id, cashier_id, cashier_first_name, cashier_last_name, cashier_password)
VALUES
-- for sake of simplicity, the employee number is their password
-- each store has a unique self help cashier for self checkout
(3329, 718111, 'SELF', 'HELP', '718111'),
(3301, 72575, 'SELF', 'HELP', '72575'),
(2558, 648172, 'SELF', 'HELP', '648172'),
(3329, 540367, 'Sally', 'Sue', '540367'),
(2558, 535113, 'Dwanye', 'The Rock', '535113'),
(3301, 394137, 'Liam', 'Wasserman', '394137'),
(3301, 716281, 'Jace', 'Margs', '716281'),
(3999, 347242, 'Josh', 'Margulies', '347242')
;
INSERT INTO registers (store_id, register_id, register_type)
VALUES
(3329, 552, 'Self'),
(3329, 443, 'Self'),
(3301, 987, 'Self'),
(5759, 448, 'Clerk'),
(3329, 580, 'Clerk'),
(3301, 3452, 'Clerk'),
(5759, 1234, 'Clerk'),
(3999, 3344, 'Clerk')
;
INSERT INTO items (item_upc, item_name, item_price, item_discount_percentage)
VALUES
('4334523664435', 'Sprite Zero Lemon-Lime Soda 20 fl oz', 2.59, 0.0),
('4235234532453', 'Owyn 20 g Plant-Based Drink Dark Chocolate 12 fl oz', 3.69, 0.10),
('8764353453456', 'Eboost Super Fuel Energy Drink Sparkling Blue Raspberry 11.5 fl oz', 2.99, 0.50),
('9723456897324', 'Met-Rx Crispy Apple Pie Meal Replacement Bar 3.52 oz', 3.59, 0.35),
('1224321345435', 'Gold Emblem Abound Dried Organic Mango 4 oz', 2.99, 0.33),
('3232321323444', 'Wrigley Extra Long Lasting Flavor Sugarfree Gum Peppermint 15 sticks', 1.59, 0.0),
('3245345253464', 'Pepto-Bismol 5 Symptom Relief Liquid 4 fl oz', 4.99, 0.0),
('3245786342577', 'MT DEW CD RED BTL 20Z', 2.59, 0.15),
('4378345897689', 'Buncha Crunch Bunches Of Crunchy Milk Chocolate 8 oz', 4.79, 0.0),
('2349082345999', 'TRLI SR DUO CRWLRS 6.3Z', 3.99, 0.0),
('3287237327771', 'pH Perfect Hydration Alkaline Water 12 pack 202 fl oz', 11.99, 0.10),
('9083458976342', 'Life Savers Mints Wint O Green 6.25 oz', 3.59, 0.0),
('2347863425897', 'Swiffer Heavy Duty Dusters 3 dusters', 6.99, 0.0),
('0980983425980', 'Lysol Disinfecting Wipes Lemon & Lime Blossom 80 wet wipes 20.3 oz', 10.29, 0.0);

INSERT INTO members (member_id, member_first_name, member_last_name, member_phone_number, member_email_address)
VALUES
(6142965, 'Tyler', 'Pohlman', '2169700354', 'tylerpp@gmail.com'),
(29166057, 'Spencer', 'Kornspan', '4406427483', 'spencervenom@gmail.com'),
(24389822, 'Phillip', 'McCourt', '3125537890', 'prmc64@icloud.com'),
(28305188, 'Duane', 'Pohlman', '2163435478', 'duanedd@gmail.com'),
(49403382, 'John', 'Smith', '9312333387', 'smithingsmith@smith.com')
;
-- Realistically, these inserts shouldn't be here b/c it implies that the given cashiers are signed into the given
-- registers... However, do to how to the following insert statements are structured, they are needed to grab the
-- cashier's name for a receipt.
INSERT INTO cashier_assignments (register_id, cashier_id)
VALUES
(552, 718111),
(443, 718111),
(987, 648172),
(448, 535113),
(580, 535113),
-- these last 3 rows are needed since the login procedure only updates rows
-- these following rows imply that the registers exist, but no one is logged in
(3452, null),
(1234, null),
(3344, null)
;
-- all combination of stores and items must be inserted here
-- I gave everything an initial value of 10
INSERT INTO inventory (store_id, item_id, item_qty)
VALUES
-- store 2558
(2558, 1, 10),
(2558, 2, 10),
(2558, 3, 10),
(2558, 4, 10),
(2558, 5, 10),
(2558, 6, 10),
(2558, 7, 10),
(2558, 8, 10),
(2558, 9, 10),
(2558, 10, 10),
(2558, 11, 10),
(2558, 12, 10),
(2558, 13, 10),
(2558, 14, 10),
-- store 3301
(3301, 1, 10),
(3301, 2, 10),
(3301, 3, 10),
(3301, 4, 10),
(3301, 5, 10),
(3301, 6, 10),
(3301, 7, 10),
(3301, 8, 10),
(3301, 9, 10),
(3301, 10, 10),
(3301, 11, 10),
(3301, 12, 10),
(3301, 13, 10),
(3301, 14, 10),
-- store 3329
(3329, 1, 10),
(3329, 2, 10),
(3329, 3, 10),
(3329, 4, 10),
(3329, 5, 10),
(3329, 6, 10),
(3329, 7, 10),
(3329, 8, 10),
(3329, 9, 10),
(3329, 10, 10),
(3329, 11, 10),
(3329, 12, 10),
(3329, 13, 10),
(3329, 14, 10),
-- store 3999
(3999, 1, 10),
(3999, 2, 10),
(3999, 3, 10),
(3999, 4, 10),
(3999, 5, 10),
(3999, 6, 10),
(3999, 7, 10),
(3999, 8, 10),
(3999, 9, 10),
(3999, 10, 10),
(3999, 11, 10),
(3999, 12, 10),
(3999, 13, 10),
(3999, 14, 10),
-- store 5759
(5759, 1, 10),
(5759, 2, 10),
(5759, 3, 10),
(5759, 4, 10),
(5759, 5, 10),
(5759, 6, 10),
(5759, 7, 10),
(5759, 8, 10),
(5759, 9, 10),
(5759, 10, 10),
(5759, 11, 10),
(5759, 12, 10),
(5759, 13, 10),
(5759, 14, 10)
;
-- have to update each time a new product is added
INSERT INTO accumulative_sales_per_product (item_id)
VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13), (14);

-- *************************
-- COMPLEX INSERT STATEMENTS
-- *************************
INSERT INTO receipts (register_id, member_id, receipt_id, receipt_date_time, receipt_cashier_full_name)
VALUES
(552, 6142965, 49654864,'2023-01-01 22:10:26', receiptsCashierName(552) ),
(448, NULL, 23930097,'2023-04-08 13:05:00', receiptsCashierName(448) ),
(443, 24389822, 52286396, NOW(), receiptsCashierName(443) ),
(552, 6142965, 68883706, '2023-05-01 12:06:53', receiptsCashierName(552) ),
(987, NULL, 44351438, '2023-05-04 10:53', receiptsCashierName(987));

INSERT INTO receipt_details (receipt_id, item_id, item_discount_percentage, item_price)
VALUES
(49654864, 2, detailsDiscount(1, 2), detailsPrice(1, 2)),
(49654864, 1, detailsDiscount(1, 1), detailsPrice(1, 1)),
(23930097, 3, detailsDiscount(2, 3), detailsPrice(2, 3)),
(52286396, 1, detailsDiscount(3, 1), detailsPrice(3, 1)),
(68883706, 4, detailsDiscount(4, 4), detailsPrice(4, 4)),
(44351438, 4, detailsDiscount(5, 4), detailsPrice(5, 4))
;
-- could have done in the insert statement, but it would have gotten too long
UPDATE receipt_details
SET item_total = item_price;
-- How does this work? I have no clue because I copied it from someone on StackOverflow...
-- The purpose of this update is to set the receipt_subtotal equal to all the receipt_details item_totals
UPDATE receipts
SET receipt_subtotal
    = (SELECT SUM(item_total) FROM receipt_details WHERE receipts.receipt_id = receipt_details.receipt_id)
;
-- sets the total equal to the the subtotal plus state tax
UPDATE receipts
SET receipt_total = receipt_subtotal * (1 + receiptsStateTax(receipt_id) )
;
-- I'm lazy, so I assumed all the receipts were paid for in the exact amount
UPDATE receipts
SET receipt_charge = receipt_total
;
-- here for future-proofing...
UPDATE receipts
SET receipt_change_due = receipt_charge - receipt_total
;

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

    given_upc VARCHAR(20)
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

    INSERT INTO receipt_details (receipt_id, item_id, item_total, item_discount_percentage, item_price)
    VALUES
    (
    given_receipt_id,
    itemIDFromUPC(given_upc),
    detailsPrice(given_receipt_id, itemIDFromUPC(given_upc)),
    detailsDiscount(given_receipt_id, itemIDFromUPC(given_upc)),
    itemPriceFromID(itemIDFromUPC(given_upc))
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
    
    -- returns amount given as change
    SELECT receipt_change_due FROM receipts WHERE receipt_id = given_receipt_id;
END //
DELIMITER ;
-- -----------------
-- memberPhoneLookup
-- Used to look up member information for a given phone number.
-- @EXCEPTION SQLSTATE '45006' if phone_number doesn't exist
-- -----------------
DELIMITER //
CREATE PROCEDURE memberPhoneLookup(
    given_phone_number VARCHAR(16)
)
BEGIN
    -- creates exception member no matching member is found
    DECLARE no_such_member CONDITION FOR SQLSTATE '45006';

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
-- Used to lookup up member information from a given member_id.
-- @EXCEPTION SQLSTATE '45003' if member_id isn't found
-- -------------------------
DELIMITER //
CREATE PROCEDURE memberAccountNumberLookup(
    given_member_id INT
)
BEGIN
    -- creates exception member no matching member is found
    DECLARE no_such_account_num CONDITION FOR SQLSTATE '45003';

    IF given_member_id NOT IN (SELECT member_id FROM members)
    THEN
        SIGNAL no_such_account_num SET MESSAGE_TEXT = 'No such account_number exists';
    END IF;

    SELECT member_first_name, member_last_name
    FROM members
    WHERE member_id = given_member_id;
END //
DELIMITER ;






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


-- *****
-- ROLES
-- *****
-- cashier
DROP ROLE IF EXISTS cashier;
CREATE ROLE cashier;
-- here so the role can even select the database in the first place
GRANT SELECT, INSERT ON hvs.* TO cashier;
-- given the procedures used in the cashier terminal application
GRANT EXECUTE ON PROCEDURE hvs.cashierRegisterLogin TO cashier;
GRANT EXECUTE ON PROCEDURE hvs.cashierRegisterLogoff TO cashier;
GRANT EXECUTE ON FUNCTION hvs.storeAddressLookupFromRegister TO cashier;
GRANT EXECUTE ON PROCEDURE hvs.itemUPCLookup TO cashier;
GRANT EXECUTE ON PROCEDURE hvs.memberPhoneLookup TO cashier;
GRANT EXECUTE ON PROCEDURE hvs.memberAccountNumberLookup TO cashier;
GRANT EXECUTE ON PROCEDURE hvs.createReceipt TO cashier;
GRANT EXECUTE ON PROCEDURE hvs.addItemToReceipt TO cashier;
GRANT EXECUTE ON PROCEDURE hvs.finalizeReceipt TO cashier;
GRANT EXECUTE ON FUNCTION hvs.getReceiptTotal TO cashier;

-- *****
-- USERS
-- *****
-- creates accounts for all the previously defined cashiers with a default password
-- of their employee number
-- their password expire every 90 days and they cannot reuse a password that was a previous 10 passwords
DROP USER IF EXISTS '718111';
CREATE USER '718111' IDENTIFIED BY '718111';
GRANT cashier TO '718111';
SET DEFAULT ROLE cashier to '718111';
ALTER USER '718111'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

DROP USER IF EXISTS '72575';
CREATE USER '72575' IDENTIFIED BY '72575';
GRANT cashier TO '72575';
SET DEFAULT ROLE cashier to '72575';
ALTER USER '72575'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

DROP USER IF EXISTS '648172';
CREATE USER '648172' IDENTIFIED BY '648172';
GRANT cashier TO '648172';
SET DEFAULT ROLE cashier to '648172';
ALTER USER '648172'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

DROP USER IF EXISTS '540367';
CREATE USER '540367' IDENTIFIED BY '540367';
GRANT cashier TO '540367';
SET DEFAULT ROLE cashier to '540367';
ALTER USER '540367'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

DROP USER IF EXISTS '535113';
CREATE USER '535113' IDENTIFIED BY '535113';
GRANT cashier TO '535113';
SET DEFAULT ROLE cashier to '535113';
ALTER USER '535113'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

DROP USER IF EXISTS '394137';
CREATE USER '394137' IDENTIFIED BY '394137';
GRANT cashier TO '394137';
SET DEFAULT ROLE cashier to '394137';
ALTER USER '394137'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

DROP USER IF EXISTS '716281';
CREATE USER '716281' IDENTIFIED BY '716281';
GRANT cashier TO '716281';
SET DEFAULT ROLE cashier to '716281';
ALTER USER '716281'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

DROP USER IF EXISTS '347242';
CREATE USER '347242' IDENTIFIED BY '347242';
GRANT cashier TO '347242';
SET DEFAULT ROLE cashier to '347242';
ALTER USER '347242'
PASSWORD HISTORY 10
PASSWORD EXPIRE INTERVAL 90 DAY;

-- ********
-- TRIGGERS
-- ********
-- cashier_assignments_after_update
DROP TRIGGER IF EXISTS cashier_assignments_after_update;
DELIMITER //
CREATE TRIGGER cashier_assignments_after_update
    BEFORE UPDATE ON cashier_assignments
    FOR EACH ROW
BEGIN
    -- only limitation is that, if the register has a NULL cashier_id, then there will be a sign out
    -- for a NULL cashier
    INSERT INTO cashier_assignments_audit
    VALUES (OLD.register_id, OLD.cashier_id, 'Sign out', NOW() );

    INSERT INTO cashier_assignments_audit
    VALUES (NEW.register_id, NEW.cashier_id, 'Sign in', NOW() );

    -- fixes edge case, removing rows where a NULL employee logins
    DELETE FROM cashier_assignments_audit
    WHERE cashier_id IS NULL;
END //
DELIMITER ;
-- stores_after_insert
-- after a new store is added, that store is added to the inventory table
-- with each unique item
DROP TRIGGER IF EXISTS stores_after_insert;
DELIMITER //
CREATE TRIGGER stores_after_insert
    AFTER INSERT ON stores
    FOR EACH ROW
BEGIN
    -- the default inventory is 0
    INSERT INTO inventory (item_id, store_id, item_qty)
    SELECT DISTINCT item_id, NEW.store_id, 0 FROM inventory
    ;
END //
DELIMITER ;

-- ******
-- EVENTS
-- ******
-- ensure event scheduler is enabled
SET GLOBAL event_scheduler = ON;

DROP EVENT IF EXISTS monthly_items_total;
DELIMITER //
CREATE EVENT monthly_items_total
    -- every month of the 9th, the accumulative_sales_per_product
    -- table is updated
    ON SCHEDULE AT '2023-05-09 10:30:00' + INTERVAL 1 MONTH
DO BEGIN
    -- item_id 1
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 1)
    WHERE item_id = 1;
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 1)
    WHERE item_id = 1;

    -- item_id 2
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 2)
    WHERE item_id = 2;
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 2)
    WHERE item_id = 2;

    -- item_id 3
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 3)
    WHERE item_id = 3;
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 3)
    WHERE item_id = 3;

    -- item_id 4
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 4)
    WHERE item_id = 4;
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 4)
    WHERE item_id = 4;

    -- item_id 5
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 5)
    WHERE item_id = 5;
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 5)
    WHERE item_id = 5;

    -- item_id 6
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 6)
    WHERE item_id = 6;
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 6)
    WHERE item_id = 6;

    -- item_id 7
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 7)
    WHERE item_id = 7;
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 7)
    WHERE item_id = 7;

    -- item_id 8
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 8)
    WHERE item_id = 8;
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 8)
    WHERE item_id = 8;

    -- item_id 9
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 9)
    WHERE item_id = 9;
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 9)
    WHERE item_id = 9;

    -- item_id 10
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 10)
    WHERE item_id = 10;
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 10)
    WHERE item_id = 10;

    -- item_id 11
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 11)
    WHERE item_id = 11;
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 11)
    WHERE item_id = 11;

    -- item_id 12
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 12)
    WHERE item_id = 12;
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 12)
    WHERE item_id = 12;

    -- item_id 13
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 13)
    WHERE item_id = 13;
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 13)
    WHERE item_id = 13;

        -- item_id 14
    UPDATE accumulative_sales_per_product
    SET total_sales = (SELECT SUM(item_total) FROM receipt_details WHERE item_id = 14)
    WHERE item_id = 14;
    UPDATE accumulative_sales_per_product
    SET sold_qty = (SELECT SUM(item_quantity) FROM receipt_details WHERE item_id = 14)
    WHERE item_id = 14;
END //
DELIMITER ;

-- *****
-- VIEWS
-- *****
CREATE VIEW receipts_view AS
SELECT receipt_id, receipt_total
FROM receipts
WHERE receipt_total > (SELECT AVG(receipt_total) FROM receipts);