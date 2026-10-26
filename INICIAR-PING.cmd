@echo off
setlocal DisableDelayedExpansion
call "%~dp0tools\windows\Launch-Ping.cmd" start %*
exit /b %errorlevel%
