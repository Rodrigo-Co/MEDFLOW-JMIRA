@echo off
setlocal

call "%~dp0mvnw.cmd" clean package
if errorlevel 1 exit /b %errorlevel%
call "%~dp0run-jar.cmd"
