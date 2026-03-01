# Wellness SMA — Guide de démarrage rapide

## Ce que fait ce projet

L'utilisateur décrit son état → 3 agents JADE analysent le texte → rAIson choisit la meilleure action.

```
Utilisateur  →  [AgentHydratation ]  →  score + arguments
             →  [AgentFatigue     ]  →  score + arguments   →  [rAIson ]  →  Décision
             →  [AgentActivite    ]  →  score + arguments
```

---

## Démarrage en 3 étapes

### Étape 1 — Obtenir JADE

JADE n'est pas sur Maven Central, il faut le télécharger manuellement.

1. Va sur https://jade.tilab.com/download/jade/license/
2. Accepte la licence → télécharge `JADE-4.6.0-bin.zip` (ou 4.5.0)
3. Extrais le zip → copie `jade.jar` dans le dossier `lib/`

### Étape 2 — Installation automatique

```bash
chmod +x setup.sh run-backend.sh run-frontend.sh
./setup.sh
```

### Étape 3 — Lancer le projet

**Terminal 1 — Backend (JADE + API REST) :**

```bash
./run-backend.sh       # Linux/Mac
run-backend.bat        # Windows
```

**Terminal 2 — Frontend React :**

```bash
./run-frontend.sh      # Linux/Mac
run-frontend.bat       # Windows
```

**Ouvre ton navigateur : http://localhost:3000**

---

## Structure du projet

```
wellness-sma/
│
├── lib/                      ← Mets jade.jar ici + JARs téléchargés auto
│   ├── jade.jar              ← À ajouter manuellement (voir Étape 1)
│   ├── commons-codec-1.3.jar ← Téléchargé par setup.sh
│   ├── jackson-databind.jar  ← Téléchargé par setup.sh
│   ├── jackson-core.jar      ← Téléchargé par setup.sh
│   └── jackson-annotations.jar
│
├── src/sma/
│   ├── MainLauncher.java     ← Point d'entrée Java (JADE + HTTP)
│   ├── agents/
│   │   ├── AgentHydratation.java  ← Détecte déshydratation
│   │   ├── AgentFatigue.java      ← Détecte fatigue / stress
│   │   ├── AgentActivite.java     ← Détecte sédentarité
│   │   └── AgentRaison.java       ← Orchestre + appelle rAIson
│   ├── api/
│   │   ├── RaisonConfig.java      ← Clé API + config
│   │   ├── RaisonAPIClient.java   ← Client HTTP rAIson
│   │   └── RaisonPayloadBuilder.java
│   ├── model/
│   │   ├── AgentProposal.java     ← Modèle proposition agent
│   │   └── WellnessDecision.java  ← Modèle décision finale
│   └── server/
│       └── WellnessHttpServer.java ← API REST sans Spring Boot
│
├── frontend/                 ← React + Vite + Tailwind
│   └── src/
│       ├── App.tsx           ← Application principale
│       ├── components/
│       │   ├── UserInput.tsx
│       │   ├── AgentCard.tsx
│       │   ├── RaisonResult.tsx
│       │   └── AnalyzingLoader.tsx
│       └── services/api.ts   ← Connexion backend
│
├── setup.sh / setup.bat      ← Installation automatique
├── run-backend.sh / .bat     ← Lance le backend Java
└── run-frontend.sh / .bat    ← Lance React
```

---

## Configuration rAIson

La clé API doit etre configurée dans `src/sma/api/RaisonConfig.java` :

```java
public static final String API_KEY = "API_key";
public static final String APP_ID  = "PRJ....";
```

## Comment ça marche techniquement

### Communication JADE (ACLMessage)

```
MainLauncher
  └─ Crée le container JADE
  └─ Lance AgentRaison, AgentHydratation, AgentFatigue, AgentActivite

HTTP POST /analyze
  └─ WellnessHttpServer lit le body JSON
  └─ Appelle agentRaison.submitRequest(texte, convId)
       └─ AgentRaison broadcast → ACLMessage REQUEST (ontologie "wellness-request")
            └─ AgentHydratation reçoit → analyse → ACLMessage INFORM (ontologie "wellness-proposal") → AgentRaison
            └─ AgentFatigue reçoit    → analyse → ACLMessage INFORM → AgentRaison
            └─ AgentActivite reçoit   → analyse → ACLMessage INFORM → AgentRaison
       └─ AgentRaison collecte 3 propositions JSON
       └─ Build payload rAIson → POST https://api.ai-raison.com/...
       └─ Stocke WellnessDecision
  └─ WellnessHttpServer récupère la décision → répond JSON
```
