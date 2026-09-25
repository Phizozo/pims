-- =============================================
-- HEALTHFIRST PHARMACY INVENTORY MANAGEMENT SYSTEM (PIMS)
-- DATABASE SCRIPT
-- Run this script in MySQL Workbench or via command line
-- by Menu: File -> Run SQL Script... OR
-- command: mysql -u root -p < database.sql
-- =============================================

CREATE DATABASE IF NOT EXISTS pims_db;
USE pims_db;

-- Drop tables in reverse order of dependencies (so re-running the script is safe)
DROP TABLE IF EXISTS sale_items;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS medicines;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS users;

-- =============================================
-- TABLE 1: users
-- Stores login credentials and roles
-- =============================================
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('Admin', 'Cashier') NOT NULL,
    full_name   VARCHAR(100) NOT NULL
);

-- =============================================
-- TABLE 2: suppliers
-- Stores medicine supplier information
-- =============================================
CREATE TABLE suppliers (
    supplier_id     INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    contact_person  VARCHAR(100),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    address         TEXT
);

-- =============================================
-- TABLE 3: medicines
-- The core inventory table
-- =============================================
CREATE TABLE medicines (
    medicine_id         INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    company             VARCHAR(100),
    medicine_type       VARCHAR(50),
    price               DECIMAL(10,2) NOT NULL,
    quantity_in_stock   INT NOT NULL,
    reorder_level       INT NOT NULL DEFAULT 10,
    expiry_date         DATE,
    supplier_id         INT,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

-- =============================================
-- TABLE 4: sales
-- Stores header information for each transaction
-- =============================================
CREATE TABLE sales (
    sale_id         INT AUTO_INCREMENT PRIMARY KEY,
    sale_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount    DECIMAL(10,2) NOT NULL,
    user_id         INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- =============================================
-- TABLE 5: sale_items
-- Stores the line items for each sale (normalized form)
-- =============================================
CREATE TABLE sale_items (
    sale_item_id    INT AUTO_INCREMENT PRIMARY KEY,
    sale_id         INT,
    medicine_id     INT,
    quantity_sold   INT NOT NULL,
    price_at_sale   DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);

-- =============================================
-- SAMPLE DATA
-- =============================================

-- Default admin user (password: admin123) and two cashiers (password: cashier123)
INSERT INTO users (username, password, role, full_name) VALUES
('admin',    'admin123', 'Admin',   'Simphiwe'),
('cashier1','cashier123', 'Cashier', 'John Smith'),
('cashier2','cashier123', 'Cashier', 'Mary Johnson');

-- Sample suppliers
INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
('MediSupply Co.',     'Peter Hadebe',   '083 111 2222', 'sales@medisupply.co.za', '12 Main Road, Durban'),
('Global Pharma Ltd',  'Sarah Nkosi',    '082 333 4444', 'info@globalpharma.co.za', '88 Industrial Park, Johannesburg'),
('CareMed Distributors', 'Thabo Mokoena', '071 555 6666', 'orders@caremed.co.za',  '45 Commerce Street, Pretoria');

-- Sample medicines (expiry dates chosen so the Expiry Report has some data)
INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES
('Panado 500mg',        'Adcock Ingram',  'Tablet',   45.50, 120, 20, '2027-06-30', 1),
('Grandpa Headache Powder', 'Adcock Ingram', 'Powder', 23.00,  80, 20, '2027-03-15', 1),
('Grippomax',           'Mentis',         'Syrup',    65.90,  60, 10, '2026-12-20', 2),
('Aspirin 300mg',       'Dis-Chem',       'Tablet',   35.25, 200, 30, '2026-11-30', 2),
('Brufen 400mg',        'Abbott',         'Capsule',  78.40,  45, 15, '2026-10-05', 3),
('Cetirizine 10mg',     'Aspen',          'Tablet',   55.00, 150, 25, '2027-01-25', 3),
('Amoxicillin 250mg',   'Sandoz',         'Capsule',  95.20,  35, 15, '2026-10-31', 1),
('Betadine Cream 20g',  'Mundipharma',    'Cream',    45.75,  90, 15, '2027-02-14', 2),
('Sodium Chloride 0.9%', 'Fresenius Kabi', 'Injection', 28.90, 70, 10, '2026-11-10', 3),
('Omeprazole 20mg',     'Aspen',          'Capsule',  88.60, 110, 20, '2026-12-15', 1);

-- A couple of sample sales (so the Sales Report is not empty)
INSERT INTO sales (sale_date, total_amount, user_id) VALUES
('2026-09-01 10:15:00', 155.40, 2),
('2026-09-02 14:30:00', 173.60, 2);

INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES
(1, 1, 2, 45.50),
(1, 4, 1, 35.25),
(1, 9, 1, 29.15),
(2, 5, 1, 78.40),
(2, 7, 1, 95.20);