@echo off
setlocal DisableDelayedExpansion
call "%~dp0tools\windows\Launch-Ping.cmd" stop %*
exit /b %errorlevel%
