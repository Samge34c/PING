@echo off
setlocal DisableDelayedExpansion
chcp 65001 >nul
set "PYTHONUTF8=1"
set "PYTHONDONTWRITEBYTECODE=1"
pushd "%~dp0..\.." || goto missing
where py.exe >nul 2>nul
if not errorlevel 1 (
  py.exe -3 -c "import sys;sys.exit(sys.version_info < (3,10))" >nul 2>nul
  if not errorlevel 1 (
    py.exe -3 "%~dp0ping_ops.py" %*
    goto finished
  )
)
where python.exe >nul 2>nul
if not errorlevel 1 (
  python.exe -c "import sys;sys.exit(sys.version_info < (3,10))" >nul 2>nul
  if not errorlevel 1 (
    python.exe "%~dp0ping_ops.py" %*
    goto finished
  )
)
for /d %%D in ("%LOCALAPPDATA%\Programs\Python\Python3*") do if exist "%%~fD\python.exe" (
  "%%~fD\python.exe" "%~dp0ping_ops.py" %*
  goto finished
)
:missing
echo No encontramos Python 3.10 o posterior. Repara su instalacion local y vuelve a abrir INICIAR-PING.cmd.
pause
exit /b 1
:finished
set "PING_EXIT=%errorlevel%"
popd
if not "%PING_EXIT%"=="0" (
  echo.
  echo No se completo el arranque. El motivo y la ubicacion del registro aparecen arriba.
  pause
)
exit /b %PING_EXIT%
