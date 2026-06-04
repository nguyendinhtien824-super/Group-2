@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ===============================================================
echo  CAI DAT CONG CU VA CHAY LAB211 FLASH SALE CONSOLE APP
echo ===============================================================
echo Can ket noi Internet neu may chua co Java hoac Maven.
echo.

set "RESTART_REQUIRED=0"

echo [*] Kiem tra Java JDK 17...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [!] Khong tim thay Java. Dang cai Java 17...
    winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-package-agreements --accept-source-agreements --silent
    set "RESTART_REQUIRED=1"
) else (
    echo [OK] Java da san sang.
)

echo [*] Kiem tra Maven...
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [!] Khong tim thay Maven. Dang cai Maven...
    winget install --id Apache.Maven -e --accept-package-agreements --accept-source-agreements --silent
    set "RESTART_REQUIRED=1"
) else (
    echo [OK] Maven da san sang.
)

if "!RESTART_REQUIRED!"=="1" (
    echo.
    echo Vui long dong cua so nay roi chay lai script sau khi PATH duoc cap nhat.
    pause
    exit /b
)

echo.
echo [1/2] Build project...
cd NHOM_01_LAB211_FlashSale
call mvn clean package
if %errorlevel% neq 0 (
    echo [LOI] Build that bai.
    pause
    exit /b
)

echo [2/2] Chay console app...
java -jar target\flash-sale-simulator-1.0.0.jar
cd ..

pause
