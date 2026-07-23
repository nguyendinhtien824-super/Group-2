@echo off
setlocal
call mvn test
exit /b %ERRORLEVEL%
