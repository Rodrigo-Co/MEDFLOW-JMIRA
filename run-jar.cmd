@echo off
setlocal

call "%~dp0load-env.cmd"

set "JAR_PATH=%~dp0target\medflow-backend-1.0.0.jar"

if not exist "%JAR_PATH%" (
  echo Arquivo nao encontrado: "%JAR_PATH%"
  echo Gere o pacote antes com:
  echo .\mvnw.cmd clean package
  exit /b 1
)

java -jar "%JAR_PATH%"
