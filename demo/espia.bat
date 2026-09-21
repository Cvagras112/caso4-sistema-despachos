@echo off
cd /d "%~dp0.."
java -cp "broker\build\install\broker\lib\*" Espia > "%TEMP%\espia.log" 2>&1
