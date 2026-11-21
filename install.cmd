@echo off
rem =============================================================================
rem  Redisca Block - build & install (CMD / Windows)
rem
rem  Usage:
rem    install.cmd                          build + install into %APPDATA%\.minecraft\mods
rem    install.cmd "D:\Games\mods"            build + install into the given folder
rem    install.cmd /NoBuild                  install existing jar into the default folder
rem    install.cmd /NoBuild "D:\Games\mods"  install existing jar into the given folder
rem
rem  Requires: Java 25+ in PATH and internet access. See README.md.
rem =============================================================================
setlocal

set "ROOT=%~dp0"
set "JAR=%ROOT%build\libs\redisca_block-1.0.0.jar"

rem args: [optional mods folder] [optional /NoBuild flag]
set "MODS=%~1"
set "FLAG=%~2"
set "SKIPBUILD="
if /I "%~1"=="/NoBuild" (
    set "FLAG=/NoBuild"
    set "MODS="
    if not "%~2"=="" set "MODS=%~2"
)
if /I "%FLAG%"=="/NoBuild" set "SKIPBUILD=1"

rem --- sanity: java must exist ---------------------------------------------
where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: 'java' was not found in PATH.
    echo Install Java 25+ from https://adoptium.net/ and try again.
    echo.
    exit /b 1
)

rem --- build ---------------------------------------------------------------
if defined SKIPBUILD (
    echo Using existing jar (no build)...
) else (
    echo Building the mod... ^(first build downloads Gradle, Minecraft and Forge^)
    call "%ROOT%gradlew.bat" build
    if errorlevel 1 (
        echo.
        echo ERROR: build failed. Check the messages above.
        echo   - This needs internet access and Java 25.
        echo   - ForgeGradle needs maven.minecraftforge.net, plugins.gradle.org,
        echo     repo.maven.apache.org and services.gradle.org.
        echo.
        exit /b 1
    )
)

rem --- locate the jar ------------------------------------------------------
if not exist "%JAR%" (
    echo.
    echo ERROR: jar not found at "%JAR%"
    echo      Run install.cmd without /NoBuild to build it first.
    echo.
    exit /b 1
)

rem --- choose a mods folder --------------------------------------------------
if "%MODS%"=="" set "MODS=%APPDATA%\.minecraft\mods"

echo.
echo Installing into "%MODS%"
if not exist "%MODS%" mkdir "%MODS%"
copy /Y "%JAR%" "%MODS%\" >nul
if errorlevel 1 (
    echo ERROR: copy failed (destination: %MODS%)
    exit /b 1
)

echo.
echo Done. Installed: "%MODS%\redisca_block-1.0.0.jar"
echo Now launch Minecraft 26.3 + Forge 66.x - blocks will place on any height.
exit /b 0
