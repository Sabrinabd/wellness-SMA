@echo off
set CP=out;lib\jade.jar;lib\commons-codec-1.3.jar;lib\jackson-databind.jar;lib\jackson-core.jar;lib\jackson-annotations.jar
echo Demarrage backend Wellness SMA...
java --add-opens java.base/sun.net.www.protocol.http=ALL-UNNAMED --add-exports jdk.httpserver/sun.net.httpserver=ALL-UNNAMED -cp "%CP%" sma.MainLauncher
pause
