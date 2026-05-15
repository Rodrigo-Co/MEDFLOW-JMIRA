@echo off
setlocal EnableDelayedExpansion

set "ENV_FILE=%~dp0.env"

if not exist "%ENV_FILE%" exit /b 0

for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
  set "ENV_KEY=%%A"
  if not "!ENV_KEY!"=="" if not "!ENV_KEY:~0,1!"=="#" endlocal & set "%%A=%%B" & setlocal EnableDelayedExpansion
)
