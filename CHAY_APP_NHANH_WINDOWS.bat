@echo off
cd /d "%~dp0"

echo ==============================
echo RUN AUCTION HUB
echo ==============================

call mvn clean javafx:run

pause