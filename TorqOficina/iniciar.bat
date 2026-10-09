@echo off
chcp 65001 >nul
cd /d "%~dp0"
call compilar.bat
if errorlevel 1 goto fim
echo Abra http://localhost:8080 no navegador.
echo Encerre o servidor com Ctrl+C.
if exist config.local.properties (
 java -Dtorq.config=config.local.properties -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App
) else (
 java -cp "build;lib\mysql-connector-j-8.4.0.jar" oficina.App
)
:fim
pause
