@echo off
rem Pravi Windows instaler target\installer\FotoEditor-<verzija>.exe
rem Potrebno: JDK sa JavaFX-om (Liberica Full 21) i WiX Toolset 3.14 (candle.exe i light.exe).
rem Kod svakog novog izdanja povecajte VERZIJA, da bi instaler pravilno nadogradio staru verziju.
setlocal
cd /d "%~dp0"

set VERZIJA=1.0

if exist "%ProgramFiles(x86)%\WiX Toolset v3.14\bin" set "PATH=%ProgramFiles(x86)%\WiX Toolset v3.14\bin;%PATH%"
if exist "%USERPROFILE%\tools\wix314" set "PATH=%USERPROFILE%\tools\wix314;%PATH%"

where candle.exe >nul 2>nul || (
    echo GRESKA: WiX Toolset 3.14 nije pronadjen. Instalirajte ga sa:
    echo https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm
    pause
    exit /b 1
)

if exist target\exe-build rmdir /s /q target\exe-build
if exist target\installer rmdir /s /q target\installer
mkdir target\exe-build\classes target\exe-build\input

echo Kompajliram...
javac -encoding UTF-8 --release 21 --add-modules javafx.controls,javafx.swing -d target\exe-build\classes src\main\java\org\example\Main.java || goto greska
jar --create --file target\exe-build\input\FotoEditor.jar --main-class org.example.Main -C target\exe-build\classes . || goto greska

echo Pravim instaler (traje minut-dva)...
jpackage --type exe --name FotoEditor --app-version %VERZIJA% ^
    --input target\exe-build\input --main-jar FotoEditor.jar --main-class org.example.Main ^
    --add-modules java.desktop,java.prefs,javafx.controls,javafx.swing ^
    --win-shortcut --win-menu --win-dir-chooser ^
    --win-upgrade-uuid c8a168f5-7f78-4f08-9d36-112d4b50f7ab ^
    --dest target\installer || goto greska

echo.
echo Gotovo: %CD%\target\installer\FotoEditor-%VERZIJA%.exe
pause
exit /b 0

:greska
echo.
echo GRESKA pri pravljenju instalera.
pause
exit /b 1
