@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

:main
ECHO.
ECHO Select an action to perform:
ECHO [b]uild
ECHO [c]lean
ECHO [e]xit
SET /p action="Enter your choice: "

IF /i "%action%"=="b" GOTO build
IF /i "%action%"=="build" GOTO build
IF /i "%action%"=="c" GOTO clean
IF /i "%action%"=="clean" GOTO clean
IF /i "%action%"=="e" GOTO exit
IF /i "%action%"=="exit" GOTO exit

ECHO Allowed values: b or build, c or clean, e or exit
GOTO main

:build
ECHO.
ECHO Which JDK version to use when compiling?
ECHO JDK-[5]
ECHO JDK-[6]
ECHO [e]xit
SET /p jdkversion="Enter your choice: "

IF "%jdkversion%"=="5" (
	SET executable="C:\Program Files (x86)\Java\jdk1.5.0_22\bin\javac.exe"
	GOTO compile
) ELSE IF "%jdkversion%"=="6" (
	SET executable="C:\Program Files (x86)\Java\jdk1.6.0_45\bin\javac.exe"
	GOTO compile
) ELSE IF /i "%jdkversion%"=="e" (
	GOTO main
) ELSE (
	ECHO Allowed values: 5 or 6 or e
	GOTO build
)

:compile
CALL ant -Dexecutable=!executable! compile-jdk!jdkversion!
IF ERRORLEVEL 1 (
    ECHO Build command failed. Returning to main menu.
) ELSE (
    ECHO Build command completed successfully.
)
GOTO main

:clean
CALL ant clean
IF ERRORLEVEL 1 (
    ECHO Clean command failed. Returning to main menu.
) ELSE (
    ECHO Clean command completed successfully.
)
GOTO main

:exit
ENDLOCAL
EXIT