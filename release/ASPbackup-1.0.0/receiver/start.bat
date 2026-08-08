@echo off
chcp 65001 >nul
title ASPbackup 备份接收端

echo ============================================
echo   ASPbackup 备份接收端 v1.0.0
echo ============================================
echo.
echo 正在启动接收端...

:: 预设配置
set PORT=9876
set OUTPUT_DIR=received-backups
set TOKEN=change-me

:: 可在此修改预设值
:: set PORT=9876
:: set OUTPUT_DIR=D:\backups
:: set TOKEN=your-secure-token

java -jar ASPbackup-receiver.jar --port %PORT% --dir "%OUTPUT_DIR%" --token %TOKEN%

pause