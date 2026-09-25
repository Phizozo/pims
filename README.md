# PIMS - Pharmacy Inventory Management System

HealthFirst Pharmacy desktop application built with **Java Swing/AWT** and **JDBC (MySQL)**.
It provides three modules:

1. **Authentication** - secure login with role-based redirection.
2. **Cashier Module** - Point of Sale, billing, and stock check (read-only).
3. **Administrator Module** - full CRUD for medicines, suppliers, users, plus Sales and Expiry reports.

---

## Demo Logins

| Role      | Username  | Password     |
|-----------|-----------|--------------|
| Admin     | `admin`   | `admin123`   |
| Cashier   | `cashier1`| `cashier123` |
| Cashier   | `cashier2`| `cashier123` |

---

## Features

- **Login screen** -> checks credentials and opens Admin *or* Cashier dashboard.
- **Cashier (POS)**: search medicines, add to cart with quantity, remove / clear cart,
  checkout with cash tendered + change calculation, `sales` and `sale_items` saved in a
  transaction, stock automatically reduced, bill preview that can be saved to a `.txt` file.
- **Stock Check** (cashier): lookup price, quantity and expiry of any medicine.
- **Admin - Medicines**: add, update, delete, search. Supplier dropdown, expiry date,
  reorder levels. (Low-stock items are highlighted on the Overview tab.)
- **Admin - Suppliers**: full CRUD.
- **Admin - Users**: create/update/delete Cashier (and Admin) accounts.
- **Reports**:
  - **Sales Report** - all transactions with date-range filter and totals.
  - **Expiry Report** - medicines expiring in the next 30 days, already-expired, or all.

---

## Database

Run the script `database.sql` (MySQL). It creates the `pims_db` database with tables:

`users`, `suppliers`, `medicines`, `sales`, `sale_items`

plus sample data (admin user, suppliers, medicines and a couple of test sales).

---

## How to Run (from a JDK + MySQL machine)

1. Install **Java 8+** and **MySQL**.
2. In MySQL Workbench: `File -> Run SQL Script -> database.sql`.
3. Put the **MySQL Connector/J** jar (`mysql-connector-j-8.x.jar`) into the `lib` folder.
4. If your MySQL username/password are not `root`/`root`, edit them in
   `src/com/pims/db/DatabaseConnection.java`.
5. Double-click `run.bat` (compiles and launches) **or**:

   ```
   javac -cp "lib\*" -d classes @sources.txt
   java -cp "classes;lib\*" com.pims.Main
   ```

---

## Project Structure

```
PIMS/
|-- database.sql                Create database, tables and sample data
|-- run.bat                     One-click compile + run (Windows)
|-- lib/                        Place mysql-connector-j jar here
`-- src/com/pims/
    |-- Main.java               Entry point
    |-- db/DatabaseConnection.java   JDBC connection helper
    |-- model/  Medicine.java, Supplier.java, User.java
    `-- ui/     LoginFrame, AdminDashboard, CashierDashboard,
                MedicineManager, SupplierManager, UserManager,
                SalesReport, ExpiryReport, BillingWindow, PrintUtility
```

---

## Building the .exe (submission requirement)

Use **Launch4j** (https://launch4j.sourceforge.net/):

1. Create a jar of the program:
   ```
   jar cfe pims.jar com.pims.Main -C classes .
   ```
   and include the MySQL Connector/J classes (or set the classpath in Launch4j).
2. In Launch4j: `Output file = yourname_pims.exe`, `Main class = com.pims.Main`,
   `Classpath = pims.jar;lib/mysql-connector-j-8.x.jar`, then build.
3. To bundle the database for easy testing, run `database.sql` on your target MySQL
   install (or bundle a portable MySQL under `lib/` and call it on first launch).

> Note: an `.exe` produced by Launch4j is a thin launcher around the Java jar - it is the
> standard lightweight approach used for Swing student projects and keeps the zip under 50 MB.