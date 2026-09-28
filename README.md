PIMS - Pharmacy Inventory Management System
Student: Simphiwe Skosana

A simple desktop app for a pharmacy. It is built with Java Swing (for the
screen buttons, tables and menus) and MySQL (to store the data).

The system has two types of users:

Role	Purpose
Admin	Manage medicines, suppliers and users. View sales and expiry reports.
Cashier	Sell medicines, make sales and print bills



Demo Login Details

| Role    | Username  | Password     |
|---------|-----------|--------------|
| Admin   | 'admin'   | 'admin123'   |
| Cashier | 'cashier1'| 'cashier123' |
| Cashier | 'cashier2'| 'cashier123' |



Features

Login
- Type your username and password.
- Wrong details => error message.
- Admin users go to the Admin screen. Cashiers go to the Cashier screen.

Cashier (Point of Sale)
- Search for a medicine.
- Add medicines to the cart, choose the quantity.
- Remove items or clear the whole cart.
- Checkout: see the total, take payment, print the bill.
- After a sale the stock is reduced automatically.

Admin
- Manage Medicines - add, update, delete or search medicines.
- Manage Suppliers - add, update or delete suppliers.
- Manage Users - add, update or delete users.
- Sales Report - see all sales, filter by date.
- Expiry Report - see which medicines expire in the next 30 days
  (or which have already expired).


How do you Set Up (step by step)

You need: Java 8+ (JDK) and MySQL installed on your computer.

1. Put the folder anywhere (e.g. 'C:\PIMS').
2. Open MySQL Workbench (or the MySQL command line).
3. Run the script 'database.sql'. It will create the 'pims_db' database,
   the tables and some sample data for you.
4. Make sure the MySQL Connector/J jar file is in the 'lib' folder.
5. If  MySQL username / password are different from the default,
   open 'src/com/pims/db/DatabaseConnection.java' and change:
   '''java
   private static final String DB_USER = "root";
   private static final String DB_PASSWORD = "Phizozo.1@";
   '''

How to Run?

1. Import database.sql in MySQL
2. Run run.bat or Main.java

Log in with 'admin / admin123' or 'cashier1 / cashier123'.


Project Files

Folder/ File	What it does?
'database.sql'	Creates the database, tables and sample data
'run.bat'	Compiles and runs the program (one click)
'lib'	Holds the MySQL Connector/J jar
'src/com/pims/Main.java'	Where the program starts
'src/com/pims/ui'	Screens (login, admin, cashier, reports)
'src/com/pims/db'	How the program connects to MySQL
'src/com/pims/model'	Data objects(Medicine, Supplier, User)





