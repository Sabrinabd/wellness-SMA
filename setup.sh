#!/bin/bash
# ═══════════════════════════════════════════════════════════
#  Wellness SMA — Script d'installation automatique
#  Lance ce script UNE SEULE FOIS avant de démarrer le projet
# ═══════════════════════════════════════════════════════════

set -e
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

echo -e "${BLUE}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║      🌿 Wellness SMA — Installation automatique      ║"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ── Vérifications prérequis ─────────────────────────────────
echo -e "${YELLOW}▶ Vérification des prérequis...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java non trouvé. Installez Java 17+ : https://adoptium.net${NC}"; exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -1 | grep -o '[0-9]*' | head -1)
echo -e "${GREEN}  ✓ Java ${JAVA_VER} détecté${NC}"

if ! command -v javac &> /dev/null; then
    echo -e "${RED}✗ javac non trouvé. Installez le JDK (pas seulement JRE)${NC}"; exit 1
fi

if ! command -v node &> /dev/null; then
    echo -e "${RED}✗ Node.js non trouvé. Installez Node 18+ : https://nodejs.org${NC}"; exit 1
fi
echo -e "${GREEN}  ✓ Node $(node --version) détecté${NC}"

if ! command -v npm &> /dev/null; then
    echo -e "${RED}✗ npm non trouvé${NC}"; exit 1
fi

# ── Téléchargement de JADE ──────────────────────────────────
echo ""
echo -e "${YELLOW}▶ Téléchargement de JADE 4.6.0...${NC}"

JADE_URL="https://jade.tilab.com/maven/com/tilab/jade/JADE/4.6.0/JADE-4.6.0-bin.zip"
JADE_FALLBACK="https://github.com/tilab-T/JADE/releases/download/JADE-4-6-0/JADE-4-6-0-bin.zip"

if [ ! -f "lib/jade.jar" ]; then
    echo "  Téléchargement de jade.jar..."

    # Essai 1 : Maven Central via wget
    if command -v wget &> /dev/null; then
        wget -q --show-progress \
            "https://repo1.maven.org/maven2/com/tilab/jade/jade/4.6.0/jade-4.6.0.jar" \
            -O lib/jade.jar 2>/dev/null || true
    fi

    # Essai 2 : curl
    if [ ! -s "lib/jade.jar" ] && command -v curl &> /dev/null; then
        curl -L --silent --show-error \
            "https://repo1.maven.org/maven2/com/tilab/jade/jade/4.6.0/jade-4.6.0.jar" \
            -o lib/jade.jar 2>/dev/null || true
    fi

    # Si JADE n'est pas sur Maven Central, utiliser le JAR depuis le site officiel
    if [ ! -s "lib/jade.jar" ]; then
        echo -e "${YELLOW}  ⚠ Téléchargement auto échoué.${NC}"
        echo ""
        echo -e "${YELLOW}  ACTION REQUISE :${NC}"
        echo "  1. Va sur : https://jade.tilab.com/download/jade/license/"
        echo "  2. Accepte la licence et télécharge JADE 4.6.0 (ou 4.5.0)"
        echo "  3. Extrais le zip et copie 'jade.jar' dans le dossier : $(pwd)/lib/"
        echo "  4. Relance ce script"
        echo ""
        echo -e "${YELLOW}  OU installe avec Maven :${NC}"
        echo "  mvn install:install-file -Dfile=/chemin/vers/jade.jar \\"
        echo "    -DgroupId=com.tilab.jade -DartifactId=jade \\"
        echo "    -Dversion=4.6.0 -Dpackaging=jar"
        echo ""
        exit 1
    fi
    echo -e "${GREEN}  ✓ jade.jar téléchargé${NC}"
else
    echo -e "${GREEN}  ✓ jade.jar déjà présent${NC}"
fi

# Télécharger commons-codec (dépendance JADE)
if [ ! -f "lib/commons-codec-1.3.jar" ]; then
    echo "  Téléchargement de commons-codec..."
    wget -q "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.3/commons-codec-1.3.jar" \
        -O lib/commons-codec-1.3.jar 2>/dev/null || \
    curl -L --silent \
        "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.3/commons-codec-1.3.jar" \
        -o lib/commons-codec-1.3.jar 2>/dev/null || true
    echo -e "${GREEN}  ✓ commons-codec-1.3.jar téléchargé${NC}"
fi

# Jackson (pour JSON)
if [ ! -f "lib/jackson-databind.jar" ]; then
    echo "  Téléchargement de jackson..."
    BASE="https://repo1.maven.org/maven2/com/fasterxml/jackson"
    wget -q "${BASE}/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar" \
        -O lib/jackson-databind.jar 2>/dev/null || \
    curl -L --silent \
        "${BASE}/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar" \
        -o lib/jackson-databind.jar 2>/dev/null || true

    wget -q "${BASE}/core/jackson-core/2.16.1/jackson-core-2.16.1.jar" \
        -O lib/jackson-core.jar 2>/dev/null || \
    curl -L --silent \
        "${BASE}/core/jackson-core/2.16.1/jackson-core-2.16.1.jar" \
        -o lib/jackson-core.jar 2>/dev/null || true

    wget -q "${BASE}/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar" \
        -O lib/jackson-annotations.jar 2>/dev/null || \
    curl -L --silent \
        "${BASE}/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar" \
        -o lib/jackson-annotations.jar 2>/dev/null || true

    echo -e "${GREEN}  ✓ Jackson téléchargé${NC}"
fi

# ── Compilation Java ─────────────────────────────────────────
echo ""
echo -e "${YELLOW}▶ Compilation du backend Java...${NC}"

# Détection OS : Windows (Git Bash/MSYS) utilise ";" comme séparateur de classpath
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OS" == "Windows_NT" ]]; then
    SEP=";"
    echo "  (Windows détecté — séparateur classpath = ;)"
else
    SEP=":"
fi

CLASSPATH="lib/jade.jar${SEP}lib/commons-codec-1.3.jar${SEP}lib/jackson-databind.jar${SEP}lib/jackson-core.jar${SEP}lib/jackson-annotations.jar"

mkdir -p out
find src -name "*.java" > sources.txt
javac --add-exports jdk.httpserver/sun.net.httpserver=ALL-UNNAMED \
      -encoding UTF-8 \
      -cp "$CLASSPATH" -d out @sources.txt

if [ $? -eq 0 ]; then
    echo -e "${GREEN}  ✓ Compilation réussie${NC}"
    rm sources.txt
else
    echo -e "${RED}  ✗ Erreur de compilation${NC}"; exit 1
fi

# ── Installation frontend Node ───────────────────────────────
echo ""
echo -e "${YELLOW}▶ Installation des dépendances frontend...${NC}"
cd frontend
npm install --silent
cd ..
echo -e "${GREEN}  ✓ Dépendances frontend installées${NC}"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════╗"
echo "║            Installation terminée !                 ║"
echo "║                                                      ║"
echo "║  Pour lancer le projet :                             ║"
echo "║    Terminal 1 → ./run-backend.sh                     ║"
echo "║    Terminal 2 → ./run-frontend.sh                    ║"
echo "║                                                      ║"
echo "║  Interface : http://localhost:3000                   ║"
echo "║  API       : http://localhost:8080/api/wellness      ║"
echo -e "╚══════════════════════════════════════════════════════╝${NC}"
