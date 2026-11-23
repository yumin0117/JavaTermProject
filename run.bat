@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist target\JavaTermProject-1.0.0.jar (
  echo IntelliJ Maven에서 package를 먼저 실행해 주세요.
  pause
  exit /b 1
)
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\javaw.exe" -jar target\JavaTermProject-1.0.0.jar
) else if exist "%USERPROFILE%\.jdks\jdk-17.0.18+8\bin\javaw.exe" (
  "%USERPROFILE%\.jdks\jdk-17.0.18+8\bin\javaw.exe" -jar target\JavaTermProject-1.0.0.jar
) else (
  javaw -jar target\JavaTermProject-1.0.0.jar
)
