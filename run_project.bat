@echo off
chcp 65001 >nul
echo ===================================================
echo   LAB211 FLASH SALE - CONSOLE APP
echo ===================================================

cd NHOM_01_LAB211_FlashSale
echo [1/2] Dang build project...
call mvn clean package
if %errorlevel% neq 0 (
    echo [LOI] Build that bai. Vui long kiem tra ma nguon.
    pause
    exit /b
)

echo [2/2] Dang chay console app...
java -jar target\flash-sale-simulator-1.0.0.jar
cd ..

echo ===================================================
pause
