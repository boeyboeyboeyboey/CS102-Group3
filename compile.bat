@echo off
rem builds the runnable jar with the maven wrapper; only a jdk 17+ is required
setlocal
cd /d "%~dp0"
set "STATUS=0"
echo Building... the first run downloads Maven and the dependencies (needs internet)
call mvnw.cmd -DskipTests package
if errorlevel 1 set "STATUS=1"
if "%STATUS%"=="0" echo Built target\auto-grading-system-1.0-SNAPSHOT.jar
if "%STATUS%"=="1" echo Build failed. Check that javac -version and JAVA_HOME point to a JDK 17 or newer. 1>&2
rem pause only when opened by double-click so the message stays visible
if defined CI exit /b %STATUS%
echo %cmdcmdline% | find /i "/c" >nul
if not errorlevel 1 pause
exit /b %STATUS%
