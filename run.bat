@echo off
REM =====================================================
REM  PIMS - Pharmacy Inventory Management System
REM  Quick start script: compiles and runs the application.
REM
REM  BEFORE RUNNING:
REM  1. Install Java 8 or later (java and javac on PATH).
REM  2. Install MySQL and run database.sql in MySQL Workbench.
REM  3. Edit DatabaseConnection.java if your MySQL username /
REM     password are not root / root.
REM  4. Put the mysql-connector-j jar in the lib folder.
REM =====================================================

title HealthFirst Pharmacy - PIMS

REM Compile all Java sources into the classes folder
mkdir classes 2>nul
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\*" -d classes @sources.txt
if errorlevel 1 (
    echo.
    echo COMPILATION FAILED. Make sure Java JDK is installed and on PATH.
    pause
    exit /b 1
)

REM Run the application
java -cp "classes;lib\*" com.pims.Main
pause