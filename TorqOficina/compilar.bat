@echo off
chcp 65001 >nul
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "scripts\executar.ps1" -Acao compilar
exit /b %errorlevel%
