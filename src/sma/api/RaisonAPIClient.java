package sma.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Client rAIson 
 *
 * Étape 1 : GET /executions/PRJ29975/latest → récupère les vrais OPT IDs (mis en cache)
 * Étape 2 : POST /executions/PRJ29975/latest → envoie les facts, lit isSolution=true
 
 */
public class RaisonAPIClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Cache OPT IDs : label → id  (évite un GET à chaque appel)
    private Map<String, String> elementLabelToId = null;
    private Map<String, String> optionLabelToId  = null;

    private final HttpClient client;

    public RaisonAPIClient() { 
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(RaisonConfig.HTTP_TIMEOUT_MS))
            .build();
    }

    /**
     * Exécute une décision rAIson à partir d'un payload construit par RaisonPayloadBuilder.
     * @return label de l'option gagnante (isSolution=true), ou null si aucune solution
     */
    public String runExecution(ObjectNode payload) throws Exception {

        // Étape 1 : récupère les IDs si pas encore en cache
        if (elementLabelToId == null) {
            fetchStructure();
        }

        // Étape 2 : remplace les labels par les vrais OPT IDs dans le payload
        ObjectNode realPayload = buildRealPayload(payload);

        String url  = RaisonConfig.BASE_URL + "/executions/" + RaisonConfig.APP_ID + "/latest";
        String body = MAPPER.writeValueAsString(realPayload);

        System.out.println("[rAIson] POST " + url);
        System.out.println("[rAIson] Payload : " + body);
        
        // Envoi de la requête POST avec le payload et les headers nécessaires, puis lecture de la réponse et extraction de la solution gagnante
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Accept",       "application/json")
            .header("x-api-key",    RaisonConfig.API_KEY)
            .timeout(Duration.ofMillis(RaisonConfig.HTTP_TIMEOUT_MS))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
         // Envoi de la requête et gestion de la réponse 
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[rAIson] Status : " + response.statusCode());
        System.out.println("[rAIson] Body   : " + response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("rAIson HTTP " + response.statusCode());
        }

        return extractWinner(response.body());
    }

    /**
     * GET /executions/PRJ29975/latest → construit les maps label→id pour éléments et options.
     * Ces maps sont utilisées pour convertir les labels du payload en vrais OPT IDs avant d'appeler rAIson.
     */
    private void fetchStructure() throws Exception {
        String url = RaisonConfig.BASE_URL + "/executions/" + RaisonConfig.APP_ID + "/latest";// URL pour récupérer la structure du projet rAIson, incluant les éléments et options disponibles avec leurs labels et IDs respectifs
        System.out.println("[rAIson] GET " + url + " (récupération structure)");
        
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept",    "application/json")
            .header("x-api-key", RaisonConfig.API_KEY)
            .timeout(Duration.ofMillis(RaisonConfig.HTTP_TIMEOUT_MS))
            .GET()
            .build();
        // Envoi de la requête GET pour récupérer la structure du projet rAIson, puis parsing de la réponse pour construire les maps de mappage entre les labels et les IDs des éléments et options du projet
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("[rAIson] Structure status : " + resp.statusCode());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("GET structure failed : " + resp.statusCode());
        }

        JsonNode root = MAPPER.readTree(resp.body());

        elementLabelToId = new HashMap<>();
        optionLabelToId  = new HashMap<>();

        for (JsonNode el : root.path("elements")) {
            elementLabelToId.put(el.path("label").asText(), el.path("id").asText());
        }
        for (JsonNode opt : root.path("options")) {
            optionLabelToId.put(opt.path("label").asText(), opt.path("id").asText());
        }

        System.out.println("[rAIson] Éléments disponibles : " + elementLabelToId.keySet());
        System.out.println("[rAIson] Options disponibles  : " + optionLabelToId.keySet());
    }

    /**
     * Remplace les labels provisoires du payload par les vrais OPT IDs.
     * Les agents construisent le payload avec des labels génériques pour rester découplés de la configuration exacte du projet rAIson, qui peut changer. Cette méthode fait le lien entre les deux en utilisant les maps de mappage construites à partir de la structure du projet récupérée via l'API.
     */
    private ObjectNode buildRealPayload(ObjectNode agentPayload) {
        ObjectNode payload = MAPPER.createObjectNode();

        // ── Éléments : label → vrai OPT ID ────────────────────────
        ArrayNode elements = MAPPER.createArrayNode();
        for (JsonNode el : agentPayload.path("elements")) {
            String label = el.path("id").asText();  // PayloadBuilder met le label dans "id"
            String realId = elementLabelToId.get(label);

            if (realId != null) {
                ObjectNode elem = MAPPER.createObjectNode();
                elem.put("id", realId);
                elem.set("parameters", MAPPER.createArrayNode());
                elements.add(elem);
                System.out.println("[rAIson] Mappage : \"" + label + "\" → " + realId);
            } else {
                System.out.println("[rAIson] ⚠ Élément inconnu : \"" + label + "\"");
            }
        }
        payload.set("elements", elements);

        // ── Options : toutes les options du projet  ─────────────────
        
        ArrayNode options = MAPPER.createArrayNode();
        for (Map.Entry<String, String> entry : optionLabelToId.entrySet()) { 
            ObjectNode opt = MAPPER.createObjectNode();
            opt.put("id",    entry.getValue());
            opt.put("label", entry.getKey());
            options.add(opt);
        }
        payload.set("options", options);

        return payload;
    }

 
    private String extractWinner(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isArray()) {
                for (JsonNode item : root) {
                    if (item.path("isSolution").asBoolean(false)) {
                        String label = item.path("option").path("label").asText();
                        System.out.println("[rAIson] ✓ SOLUTION : " + label);
                        return label;
                    }
                }
                System.out.println("[rAIson] ✗ Aucune solution (tout isSolution=false)");
                System.out.println("[rAIson] → Vérifie que les conflits sont résolus et le projet publié");
                return null;
            }
        } catch (Exception e) {
            System.err.println("[rAIson] Erreur parsing : " + e.getMessage());
        }
        return null;
    }
}
