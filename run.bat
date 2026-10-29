@echo off
rem no arguments opens the desktop window; any arguments go to the command line app
setlocal
cd /d "%~dp0"
set "JAR=target\auto-grading-system-1.0-SNAPSHOT.jar"
set "STATUS=1"
if not exist "%JAR%" goto missing
if "%~1"=="" goto gui
java -jar "%JAR%" %*
set "STATUS=%errorlevel%"
goto finish
:gui
echo Opening the grading window; keep this console open while it runs.
java -cp "%JAR%" autograder.gui.GuiLauncher
set "STATUS=%errorlevel%"
goto finish
:missing
echo %JAR% not found. Run compile.bat first. 1>&2
:finish
if "%STATUS%"=="0" exit /b 0
rem pause on failure only when opened by double-click so the message stays visible
if defined CI exit /b %STATUS%
echo %cmdcmdline% | find /i "/c" >nul
if not errorlevel 1 pause
exit /b %STATUS%
