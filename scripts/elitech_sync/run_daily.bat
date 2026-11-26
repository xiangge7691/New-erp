@echo off
rem Elitech temperature/humidity sync - scheduled task entry
rem Sync yesterday data and write to ERP database.
chcp 65001 >nul
set PYTHONIOENCODING=utf-8
cd /d "%~dp0"
python sync_room_th.py --date yesterday --commit %* >> sync.log 2>&1
exit /b %ERRORLEVEL%
