@echo off
setlocal enabledelayedexpansion

title ASPbackup Backup Receiver

:: Get script directory
set SCRIPT_DIR=%~dp0
set JAR_FILE=%SCRIPT_DIR%ASPbackup-receiver.jar

:: Check if jar exists
if not exist "%JAR_FILE%" (
    echo [ERROR] Jar file not found: %JAR_FILE%
    echo Please ensure ASPbackup-receiver.jar is in the same directory as this script.
    pause
    exit /b 1
)

echo ============================================
echo   ASPbackup Backup Receiver v1.0.0
echo ============================================
echo.
echo Starting receiver...
echo.

:: Default config
set PORT=9876
set OUTPUT_DIR=%SCRIPT_DIR%received-backups
set TOKEN=change-me

:: Uncomment and modify below to change defaults:
:: set PORT=9876
:: set OUTPUT_DIR=D:\backups
:: set TOKEN=your-secure-token

echo   Port:       %PORT%
echo   Output Dir: %OUTPUT_DIR%
echo   Jar:        %JAR_FILE%
echo.

java -jar "%JAR_FILE%" --port %PORT% --dir "%OUTPUT_DIR%" --token %TOKEN%

pause