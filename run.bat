@echo off
rem no arguments opens the desktop window; any arguments go to the command line app
setlocal
cd /d "%~dp0"
set "JAR=target\auto-grading-system-1.0-SNAPSHOT.jar"
if not exist "%JAR%" (
    echo %JAR% not found. Run compile.bat first. 1>&2
    exit /b 1
)
if "%~1"=="" goto gui
java -jar "%JAR%" %*
exit /b %errorlevel%
:gui
java -cp "%JAR%" autograder.gui.GuiLauncher
exit /b %errorlevel%
