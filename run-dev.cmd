@echo off
setlocal

call "%~dp0load-env.cmd"

where mvn >nul 2>nul
if %errorlevel% equ 0 (
  call mvn clean package
) else (
  call "%~dp0mvnw.cmd" clean package
)

if errorlevel 1 exit /b %errorlevel%
call "%~dp0run-jar.cmd"
