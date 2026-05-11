@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
java -cp "%SCRIPT_DIR%fizzim.jar" FizzimJavaBackend %*
exit /b %ERRORLEVEL%
