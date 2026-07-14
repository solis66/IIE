@echo off
title Stop SAM2 Services

echo Stopping SAM2 related processes...

REM Close ngrok
taskkill /f /fi "WINDOWTITLE eq ngrok*" 2>nul

REM Close uvicorn (SAM2 server)
taskkill /f /fi "WINDOWTITLE eq SAM2-Server*" 2>nul

REM Fallback: kill by port 8800
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8800" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a 2>nul
)

echo All services stopped.
pause
