@echo off
rem grades the 100-student demo cohort, generating it first when it is missing
rem demo.bat [workers]                 grade build\demo-cohort with demo.properties
rem demo.bat generate [count] [seed]   rebuild the cohort (default 94 synthetic plus the 6 real)
setlocal
cd /d "%~dp0"
set "JAR=target\auto-grading-system-1.0-SNAPSHOT.jar"
set "STATUS=1"
if not exist "%JAR%" goto missing
if /i "%~1"=="generate" goto generate
if not "%~2"=="" goto usage
if exist "build\demo-cohort\submissions\" goto grade
java -cp "%JAR%" autograder.demo.DemoCohortGenerator
if errorlevel 1 goto finish
:grade
if "%~1"=="" (
    java -jar "%JAR%" demo.properties
) else (
    java -jar "%JAR%" --workers %1 demo.properties
)
set "STATUS=%errorlevel%"
goto finish
:generate
java -cp "%JAR%" autograder.demo.DemoCohortGenerator %2 %3 %4
set "STATUS=%errorlevel%"
goto finish
:usage
echo usage: demo.bat [workers] ^| demo.bat generate [count] [seed] 1>&2
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
