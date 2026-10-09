@echo off
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
 echo Instale um JDK 17 ou superior e adicione javac ao PATH.
 exit /b 1
)
if not exist "lib\mysql-connector-j-8.4.0.jar" (
 echo Driver JDBC nao encontrado em lib.
 exit /b 1
)
if not exist build mkdir build
javac --release 17 -encoding UTF-8 -d build src\oficina\Json.java src\oficina\App.java
exit /b %errorlevel%
