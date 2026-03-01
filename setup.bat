@echo off
REM ═══════════════════════════════════════════════════════════
REM  Wellness SMA — Installation (Windows)
REM ═══════════════════════════════════════════════════════════

echo.
echo  Wellness SMA - Installation automatique (Windows)
echo =====================================================

REM Vérification Java
java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo ERREUR : Java non trouve. Installez Java 17+ depuis https://adoptium.net
    pause & exit /b 1
)
echo [OK] Java detecte

REM Vérification Node
node --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo ERREUR : Node.js non trouve. Installez depuis https://nodejs.org
    pause & exit /b 1
)
echo [OK] Node.js detecte

REM Téléchargement Jackson via curl (disponible Windows 10+)
echo.
echo Telechargement des dependances Jackson...
IF NOT EXIST lib\jackson-databind.jar (
    curl -L -o lib\jackson-databind.jar "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar"
    curl -L -o lib\jackson-core.jar "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar"
    curl -L -o lib\jackson-annotations.jar "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar"
)

REM Vérification jade.jar
IF NOT EXIST lib\jade.jar (
    echo.
    echo ATTENTION : jade.jar manquant !
    echo.
    echo  1. Telecharge JADE depuis : https://jade.tilab.com
    echo  2. Copie jade.jar dans le dossier : %cd%\lib\
    echo  3. Relance ce script
    echo.
    pause & exit /b 1
)
echo [OK] jade.jar present

REM Compilation
echo.
echo Compilation Java...
IF NOT EXIST out mkdir out
dir /s /b src\*.java > sources.txt
javac --add-exports jdk.httpserver/sun.net.httpserver=ALL-UNNAMED -encoding UTF-8 -cp "lib\jade.jar;lib\commons-codec-1.3.jar;lib\jackson-databind.jar;lib\jackson-core.jar;lib\jackson-annotations.jar" -d out @sources.txt
IF %ERRORLEVEL% NEQ 0 (
    echo ERREUR de compilation
    pause & exit /b 1
)
del sources.txt
echo [OK] Compilation reussie

REM Frontend
echo.
echo Installation frontend...
cd frontend
npm install
cd ..
echo [OK] Frontend pret

echo.
echo  Installation terminee !
echo  Pour lancer :
echo    Terminal 1 : run-backend.bat
echo    Terminal 2 : run-frontend.bat
echo.
pause
