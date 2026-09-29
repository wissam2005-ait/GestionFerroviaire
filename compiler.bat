@echo off
javac -encoding UTF-8 -cp ".;*" *.java
if %errorlevel% neq 0 (
    echo Erreur de compilation.
    pause
    exit /b 1
)
echo Compilation reussie.
