@echo off
title SAM2 Server Launcher

echo ================================================
echo    SAM2 Matting Server - One-Click Start
echo ================================================
echo.

echo [1/3] Starting SAM2 FastAPI server (port 8800) ...
start "SAM2-Server" cmd /c "cd /d %~dp0 && uvicorn server:app --host 0.0.0.0 --port 8800"

echo [2/3] Starting ngrok tunnel ...
start "ngrok" cmd /c "ngrok http 8800"

echo [3/3] Waiting for ngrok and updating Android config ...
echo.
echo Fetching ngrok public URL, please wait...

REM Wait for ngrok to establish tunnel (~6 seconds)
timeout /t 6 /nobreak >nul

REM Auto-detect ngrok URL and update Kotlin source
python "%~dp0update_url.py"

echo.
echo ================================================
echo   SAM2 server is running!
echo.
echo   SAM2 API : http://localhost:8800/health
echo   ngrok UI : http://127.0.0.1:4040
echo.
echo   Rebuild & install APP to use remote matting.
echo ================================================
echo.
echo Press any key to close this window...
echo (Services will keep running in background)
pause >nul
