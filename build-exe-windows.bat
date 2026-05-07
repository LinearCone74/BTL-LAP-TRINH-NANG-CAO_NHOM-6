@echo off
setlocal
cd /d "%~dp0"

echo [1/4] Kiem tra JDK...
where java >nul 2>nul
if errorlevel 1 (
  echo Khong tim thay Java. Hay cai JDK 17 hoac 21, sau do chay lai file nay.
  pause
  exit /b 1
)
where jpackage >nul 2>nul
if errorlevel 1 (
  echo Khong tim thay jpackage. Ban dang dung JRE hoac JDK thieu jpackage.
  echo Hay cai JDK 17/21 va them thu muc bin cua JDK vao PATH.
  pause
  exit /b 1
)

echo [2/4] Build project bang Maven Wrapper...
call mvnw.cmd -DskipTests clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\package-input
if errorlevel 1 (
  echo Build that bai. Hay xem loi phia tren.
  pause
  exit /b 1
)

echo [3/4] Chuan bi file jar...
copy /Y target\dau-gia-truc-tuyen-1.0-SNAPSHOT.jar target\package-input\ >nul
if errorlevel 1 (
  echo Khong tim thay file jar sau khi build.
  pause
  exit /b 1
)

echo [4/4] Tao file cai dat .exe...
if exist dist rmdir /S /Q dist
jpackage ^
  --type exe ^
  --name UngDungDauGia ^
  --app-version 1.0 ^
  --vendor "Nhom 6" ^
  --input target\package-input ^
  --main-jar dau-gia-truc-tuyen-1.0-SNAPSHOT.jar ^
  --main-class com.auction.AuctionApp ^
  --dest dist ^
  --win-shortcut ^
  --win-menu

if errorlevel 1 (
  echo Tao .exe that bai. Hay xem loi phia tren.
  pause
  exit /b 1
)

echo.
echo XONG! File .exe nam trong thu muc: dist
echo Ban co the mo dist va chay UngDungDauGia-1.0.exe de cai app.
pause
