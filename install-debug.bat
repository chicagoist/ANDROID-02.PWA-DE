@echo off
echo Installing Just German on connected device...
call gradlew.bat installDebug
if %ERRORLEVEL% EQU 0 (
    echo.
    echo App installed successfully!
) else (
    echo.
    echo Installation failed! Make sure device is connected via ADB.
)
pause
