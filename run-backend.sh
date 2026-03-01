#!/bin/bash
# ================================================
#  Wellness SMA - Demarrage Backend (JADE + HTTP)
# ================================================

# Detection OS : Windows Git Bash utilise ";" pour java -cp
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OS" == "Windows_NT" ]]; then
    SEP=";"
else
    SEP=":"
fi

CLASSPATH="out${SEP}lib/jade.jar${SEP}lib/commons-codec-1.3.jar${SEP}lib/jackson-databind.jar${SEP}lib/jackson-core.jar${SEP}lib/jackson-annotations.jar"

echo ""
echo "Demarrage du backend Wellness SMA..."
echo "Agents JADE + Serveur HTTP sur port 8080"
echo "Ctrl+C pour arreter"
echo ""

java --add-opens java.base/sun.net.www.protocol.http=ALL-UNNAMED \
     --add-exports jdk.httpserver/sun.net.httpserver=ALL-UNNAMED \
     -cp "$CLASSPATH" sma.MainLauncher
