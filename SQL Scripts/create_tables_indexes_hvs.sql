-- *************************
-- CREATE TABLES AND INDEXES
-- *************************
-- CREATES TABLE STATES
-- This table is used for state taxes.
DROP TABLE IF EXISTS states;
CREATE TABLE states
(
    -- can act as the primary key since CHAR(2) isn't overly complex
    -- also, makes it easier for inputting state into stores table
	state_name CHAR(2) PRIMARY KEY,
    -- each state must have a specified tax percentage
    state_tax_percentage DECIMAL(2,2) NOT NULL
);
-- CREATES THE STORES TABLE
-- *WARNING* must create table states first
DROP TABLE IF EXISTS stores;
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
-- CREATES THE CASHIERS TABLE
-- 	*WARNING* must create stores table first
DROP TABLE IF EXISTS cashiers;
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

-- CREATES THE REGISTERS TABLE
-- *WARNING* must create the stores table first
DROP TABLE IF EXISTS registers;
CREATE TABLE registers
(
	register_id INT PRIMARY KEY AUTO_INCREMENT,
    -- foreign key
    store_id INT NOT NULL,
    -- the type is important because self always use a SELF HELP cashier
    register_type ENUM('Self', 'Clerk', 'Other'),
    -- The relationship to the stores table isn't necessarily needed, it just speeds up inquiries since going from registers
    -- to stores is faster than going from registers, to cashier_assignments, to cashiers, and then to store. Likewise, cashiers
    -- come and go, so having the relationship between stores and registers depend on cashiers can cause issues
    CONSTRAINT registers_fk_stores FOREIGN KEY (store_id) REFERENCES stores(store_id)
);

-- 	CREATES CASHIER_ASSIGNMENTS TABLE
-- *WARNING* must create cashiers and registers tables first
-- This table is implemented in such a way that it is updated frequently--i.e., cashiers are logging into and
-- logging out of registers many times a day.
DROP TABLE IF EXISTS cashier_assignments;
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
-- CREATES TABLE ITEMS
DROP TABLE IF EXISTS items;
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
-- CREATES TABLE MEMBERS
-- Basically, this stores whomever rewards members are. Rewards are able to access the given savings,
-- while nonmembers always pay the full price.
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
-- phone number is used to look up rewards membership, so it is indexed
CREATE INDEX idx_phone
ON members (member_phone_number);
-- CREATES RECEIPTS TABLE
-- *WARNING* must create registers, states, and members tables first
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
-- CREATES RECEIPT_DETAILS TABLE
-- *WARNING* must create receipts table first
CREATE TABLE receipt_details
(
	-- foreign keys
	receipt_id INT NOT NULL,
    item_id INT NOT NULL,
    item_quantity INT NOT NULL,
    item_total DECIMAL(9,2) DEFAULT 0.0,
    --
    -- The following are copied from the items table since these values can change in that table.
    --
    item_price DECIMAL(9,2) DEFAULT 0.00,
    item_discount_percentage DECIMAL(2,2) DEFAULT 0.00,
    CONSTRAINT details_fk_receipts FOREIGN KEY (receipt_id) REFERENCES receipts(receipt_id),
    CONSTRAINT details_fk_items FOREIGN KEY (item_id) REFERENCES items(item_id)
);
-- CREATES RECEIPT_DETAILS_AUDIT TABLE
CREATE TABLE cashier_assignments_audit
(
    -- stores the assignments
    register_id INT,
    cashier_id INT,
    -- stores the date of the change and what type change took place
    action_type ENUM('Sign in', 'Sign out'),
    action_date DATETIME DEFAULT NOW()
);
-- CREATES ACCUMULATIVE_SALES_PER_PRODUCT
CREATE TABLE accumulative_sales_per_product
(
    item_id INT,
    sold_qty INT DEFAULT 0,
    total_sales DECIMAL(10,2) DEFAULT 0.0
);
-- CREATES INVENTORY TABLE
CREATE TABLE inventory (
    store_id INT,
    item_id INT,
    item_qty INT NOT NULL,
    CONSTRAINT pk_inventory PRIMARY KEY (store_id, item_id),
    CONSTRAINT inventory_fk_stores FOREIGN KEY (store_id) REFERENCES stores(store_id),
    CONSTRAINT inventory_fk_items FOREIGN KEY (item_id) REFERENCES items(item_id)
);
-- CREATES RETURNED_INVENTORY TABLE
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