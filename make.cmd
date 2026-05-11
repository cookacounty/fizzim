@echo off
setlocal enabledelayedexpansion

set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=jar"

if /I "%TARGET%"=="all" set "TARGET=jar"

if /I "%TARGET%"=="help" goto :help
if /I "%TARGET%"=="jar" goto :jar
if /I "%TARGET%"=="clean" goto :clean
if /I "%TARGET%"=="test" goto :test
if /I "%TARGET%"=="test-verilog" goto :test
if /I "%TARGET%"=="test-fuzz" goto :test_fuzz

echo Unknown target: %TARGET%
echo.
goto :help

:tools
if "%JAVA_RELEASE%"=="" set "JAVA_RELEASE=11"
if not "%JAVA_HOME%"=="" (
  set "JAVAC=%JAVA_HOME%\bin\javac.exe"
  set "JAR=%JAVA_HOME%\bin\jar.exe"
) else (
  set "JAVAC=javac"
  set "JAR=jar"
)
exit /b 0

:jar
call :tools
echo Compiling Java sources for Java %JAVA_RELEASE%
"%JAVAC%" --release %JAVA_RELEASE% *.java
if errorlevel 1 exit /b %errorlevel%
echo Creating fizzim.jar
"%JAR%" cfm fizzim.jar manifest.txt *.class splash.png icon.png org *.properties
exit /b %errorlevel%

:clean
del /q *.class 2>nul
del /q *.jar 2>nul
del /q jar.log 2>nul
for /d %%D in (*_jar) do rmdir /s /q "%%D"
exit /b 0

:test
bash testcases/run_backend_flow.sh
exit /b %errorlevel%

:test_fuzz
call :jar
if errorlevel 1 exit /b %errorlevel%
node testcases\tools\fuzz_backend_compare.js
exit /b %errorlevel%

:help
echo Fizzim build and test helper
echo.
echo   make jar              Build fizzim.jar with GNU Make
echo   make clean            Remove Java build artifacts with GNU Make
echo   make test             Run public backend regression with GNU Make
echo.
echo Windows fallback when GNU Make is not installed:
echo.
echo   make.cmd jar
echo   make.cmd clean
echo   make.cmd test
echo   make.cmd test-fuzz
echo.
echo JAVA_HOME may point at a JDK directory.
echo JAVA_RELEASE defaults to 11 and may be overridden.
exit /b 0
