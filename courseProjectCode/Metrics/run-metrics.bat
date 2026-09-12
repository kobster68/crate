@echo off

cd /d "%~dp0"

if not exist "build" mkdir "build"

javac -d "build" "src\metrics\Main.java" "src\metrics\CodeStructureMetrics.java" "src\metrics\TestabilityMetrics.java"

if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

java -cp "build" metrics.Main

pause