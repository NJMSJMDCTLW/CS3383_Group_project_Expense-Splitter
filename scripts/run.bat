@echo off
rem Starts the Expense Splitter.
rem The jar is looked up in this order:
rem   1. next to this script (the submission Release folder)
rem   2. the Release folder at the project root (when run from Source\scripts)
rem   3. the Maven build output (Source\target)
setlocal
set "JAVA_EXE=java"
if exist "%ProgramFiles%\Java\jdk-17\bin\java.exe" set "JAVA_EXE=%ProgramFiles%\Java\jdk-17\bin\java.exe"

set "JAR="
if exist "%~dp0expense-splitter.jar" set "JAR=%~dp0expense-splitter.jar"
if not defined JAR if exist "%~dp0..\..\Release\expense-splitter.jar" set "JAR=%~dp0..\..\Release\expense-splitter.jar"
if not defined JAR if exist "%~dp0..\target\expense-splitter.jar" set "JAR=%~dp0..\target\expense-splitter.jar"

if not defined JAR (
  echo The jar was not found. Build it with "mvn package", or use the Release folder at the project root.
  pause
  exit /b 1
)

"%JAVA_EXE%" -jar "%JAR%"
if errorlevel 1 pause
