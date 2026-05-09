@echo off
title BUILD EXE WINDOWS
color 0A

echo ============================
echo CLEAN + PACKAGE MAVEN
echo ============================

call mvn clean package

if %errorlevel% neq 0 (
    echo.
    echo BUILD MAVEN FAILED
    pause
    exit /b
)

echo.
echo ============================
echo BUILD APP IMAGE
echo ============================

jpackage ^
 --type app-image ^
 --name UngDungDauGia ^
 --input target ^
 --main-jar dau-gia-truc-tuyen-1.0-SNAPSHOT.jar ^
 --main-class com.auction.AuctionApp ^
 --dest dist ^
 --win-console

if %errorlevel% neq 0 (
    echo.
    echo BUILD EXE FAILED
    pause
    exit /b
)

echo.
echo ============================
echo DONE
echo ============================
echo.
echo Open:
echo dist\UngDungDauGia\UngDungDauGia.exe
echo.

pause