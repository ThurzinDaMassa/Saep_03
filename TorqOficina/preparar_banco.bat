@echo off
chcp 65001 >nul
cd /d "%~dp0"
call compilar.bat
if errorlevel 1 goto fim
if exist config.local.properties (
 java -Dtorq.config=config.local.properties -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App --init
) else (
 java -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App --init
)
:fim
pause
