@echo off
rem builds the runnable jar with the maven wrapper; only a jdk 17+ is required
setlocal
cd /d "%~dp0"
call mvnw.cmd -q -DskipTests package
if errorlevel 1 (
    echo Build failed. Check that a JDK 17 or newer is on PATH ^(javac -version^). 1>&2
    exit /b 1
)
echo Built target\auto-grading-system-1.0-SNAPSHOT.jar
