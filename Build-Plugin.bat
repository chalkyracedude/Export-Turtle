@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat build shadowJar --console=plain
if errorlevel 1 pause
