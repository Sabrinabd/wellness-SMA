package sma.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import sma.agents.AgentRaison;
import sma.api.RaisonConfig;
import sma.model.WellnessDecision;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 
    * Serveur HTTP pour exposer une API REST permettant d'interagir avec le SMA de wellness.

        * Ce serveur reçoit les requêtes du client (ex: application mobile), transmet la description à AgentRaison,
        * attend la décision finale, puis renvoie cette décision au client au format JSON.
 *
 * Endpoints :
 *   POST /api/wellness/analyze  → lance une analyse via le SMA
 *   GET  /api/wellness/health   → santé du service
 *   GET  /api/wellness/examples → exemples de descriptions
 *   OPTIONS *                   → CORS preflight
 * 
 */
public class WellnessHttpServer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer  server;
    private final AgentRaison agentRaison;
 // Constructeur qui initialise le serveur HTTP, configure les endpoints et les handlers, et prépare le thread pool pour gérer les requêtes concurrentes.
    public WellnessHttpServer(AgentRaison agentRaison) throws IOException {
        this.agentRaison = agentRaison;
          
        server = HttpServer.create(new InetSocketAddress(RaisonConfig.SERVER_PORT), 0);
        server.createContext("/api/wellness/analyze",  this::handleAnalyze);// Endpoint principal pour analyser une description et obtenir une recommandation
        server.createContext("/api/wellness/health",   this::handleHealth);// Endpoint de santé pour vérifier que le service est opérationnel
        server.createContext("/api/wellness/examples", this::handleExamples);// Endpoint pour fournir des exemples de descriptions que les utilisateurs peuvent tester, utile pour le développement et le debug
        server.setExecutor(Executors.newFixedThreadPool(4));
    }
     
    // Démarre le serveur HTTP et affiche les endpoints disponibles dans la console pour faciliter les tests.
    public void start() {
        server.start();
        System.out.println("[HTTP] Serveur démarré sur http://localhost:" + RaisonConfig.SERVER_PORT);
        System.out.println("[HTTP] Endpoints disponibles :");
        System.out.println("  POST http://localhost:" + RaisonConfig.SERVER_PORT + "/api/wellness/analyze");
        System.out.println("  GET  http://localhost:" + RaisonConfig.SERVER_PORT + "/api/wellness/health");
    }

    // ── POST /api/wellness/analyze ───────────────────────────────
    private void handleAnalyze(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(ex.getRequestMethod())) {
            sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
            return;
        }

        try {
            // Lire le body
            InputStream is  = ex.getRequestBody();
            String body     = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var requestJson = MAPPER.readTree(body);
            String description = requestJson.has("description")
                    ? requestJson.get("description").asText() : "";

            if (description.isBlank()) {
                sendJson(ex, 400, Map.of("success", false, "errorMessage", "Le champ 'description' est requis"));
                return;
            }

            System.out.printf("%n[HTTP] POST /analyze — description: \"%s\"%n", description.substring(0, Math.min(80, description.length())));

            // Générer un convId unique et soumettre à AgentRaison
            String convId = UUID.randomUUID().toString().substring(0, 8);
            agentRaison.submitRequest(description, convId);

            // Attendre la décision (30s max)
            WellnessDecision decision = agentRaison.waitForDecision(convId, RaisonConfig.AGENT_TIMEOUT_MS);
            sendJson(ex, 200, decision);

        } catch (Exception e) {
            System.err.println("[HTTP] Erreur analyse : " + e.getMessage());
            sendJson(ex, 500, WellnessDecision.error("Erreur serveur : " + e.getMessage(), null));
        }
    }

    // ── GET /api/wellness/health ─────────────────────────────────
    private void handleHealth(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        sendJson(ex, 200, Map.of(
            "status",  "UP",
            "service", "Wellness SMA",
            "agents",  new String[]{"AgentHydratation", "AgentFatigue", "AgentActivite", "AgentRaison"},
            "raisonAppId", RaisonConfig.APP_ID,
            "port",    RaisonConfig.SERVER_PORT
        ));
    }

    // ── GET /api/wellness/examples ───────────────────────────────
    private void handleExamples(HttpExchange ex) throws IOException {
        addCorsHeaders(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        sendJson(ex, 200, new String[]{
            "J'ai un peu mal à la tête, je suis fatigué, et j'ai travaillé 3h sans pause.",
            "Je me sens tendu et je suis assis depuis ce matin.",
            "Je n'arrive plus à me concentrer, je suis devant l'écran depuis longtemps.",
            "J'ai les yeux fatigués et les épaules raides après 4h de réunion.",
            "Je suis épuisé, stressé et je n'ai pas bu depuis des heures."
        });
    }

    // ── Utilitaires ──────────────────────────────────────────────
    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
