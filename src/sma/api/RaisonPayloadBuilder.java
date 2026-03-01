package sma.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import sma.model.AgentProposal;
import sma.model.AgentProposal.AgentType;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class RaisonPayloadBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Mapping AgentType → label exact dans rAIson PRJ29975
    private static final Map<AgentType, String> AGENT_TO_LABEL = Map.of(
        AgentType.HYDRATATION, "signes de deshydratation",
        AgentType.FATIGUE,     "fatigue mentale detectee",
        AgentType.ACTIVITE,    "position assise prolongee"
    );

    public static ObjectNode buildPayload(List<AgentProposal> proposals) {

        // Les 2 agents avec les meilleurs scores
        // On utilise un Set pour éviter les doublons si jamais 2 propositions du même agent dépassent le seuil
        Set<AgentType> actifs = new HashSet<>();
        proposals.stream()
            .sorted((a, b) -> Integer.compare(b.getImportance(), a.getImportance()))
            .limit(2)
            .forEach(p -> actifs.add(p.getAgentType()));      

        System.out.println("[PayloadBuilder] Agents actifs (score>=5) : " + actifs);

        // Construction des éléments avec les labels exacts rAIson
        // RaisonAPIClient les convertira en vrais OPT IDs via GET
        // Seuls les agents avec un score >= 5 sont inclus pour éviter d'envoyer des signaux faibles à rAIson
        ArrayNode elements = MAPPER.createArrayNode();
        for (AgentType type : actifs) {
            String label = AGENT_TO_LABEL.get(type);
            if (label == null) continue;

            ObjectNode el = MAPPER.createObjectNode();
            el.put("id", label);  
            el.set("parameters", MAPPER.createArrayNode());
            elements.add(el);
            System.out.println("[PayloadBuilder] + \"" + label + "\"");
        }
        // si aucun agent n'atteint le seuil de 5, le payload contiendra une liste d'éléments vide, ce qui est acceptable pour rAIson et
       
        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("elements", elements);
        return payload;
    }
}
