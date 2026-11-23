@echo off
rem Starts the Expense Splitter with Java 17 when it is installed in the default
rem location, otherwise with the java command from PATH.
setlocal
set "JAVA_EXE=java"
if exist "%ProgramFiles%\Java\jdk-17\bin\java.exe" set "JAVA_EXE=%ProgramFiles%\Java\jdk-17\bin\java.exe"
"%JAVA_EXE%" -jar "%~dp0expense-splitter.jar"
if errorlevel 1 pause
