@ECHO OFF

SET DIR=%~dp0
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIR%

IF NOT "%JAVA_HOME%"=="" (
  SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
) ELSE (
  SET JAVA_EXE=java.exe
)

"%JAVA_EXE%" -version >NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  ECHO.
  ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  ECHO.
  EXIT /B 1
)

"%JAVA_EXE%" -Xmx64m -cp "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

