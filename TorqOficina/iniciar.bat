@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Antes de continuar, abra o XAMPP Control Panel e inicie MySQL.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "scripts\executar.ps1" -Acao mysql
pause
