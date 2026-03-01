package sma.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import sma.model.AgentProposal;
import sma.model.AgentProposal.AgentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent Hydratation 
 *
 * Rôle : détecter les signes de déshydratation dans le texte utilisateur
 *        et produire une proposition structurée.
 *
 * Communication JADE :
 *   - Reçoit  : ACLMessage REQUEST (ontologie "wellness-request")
 *   - Envoie  : ACLMessage INFORM  (ontologie "wellness-proposal") → AgentRaison
 */
public class AgentHydratation extends Agent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Mots-clés de détection ───────────────────────────────
    private static final String[] KEYWORDS_TETE = { // Mots-clés liés aux maux de tête et vertiges, symptômes classiques de la déshydratation
        "mal à la tête", "maux de tête", "migraine", "céphalée",
        "tête qui tourne", "headache", "vertige", "vertiges"
    };
    private static final String[] KEYWORDS_SOIF = { // Mots-clés liés à la sensation de soif et autres signes de déshydratation
        "soif", "thirsty", "bouche sèche", "lèvres sèches",
        "gorge sèche", "urine foncée", "peu uriné"
    };
    private static final String[] KEYWORDS_DUREE = { // Mots-clés liés à la durée prolongée sans boire, un facteur de risque important
        "3h", "4h", "5h", "plusieurs heures", "toute la journée",
        "longtemps", "depuis ce matin", "sans boire", "sans eau"
    };

    @Override
    protected void setup() {
        System.out.println("[AgentHydratation]  Prêt — En écoute...");
        // Filtre pour recevoir uniquement les messages de type REQUEST avec l'ontologie "wellness-request"
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("wellness-request")
        );

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive(mt);

                if (msg != null) {// Message reçu → analyse et proposition
                    String text = msg.getContent().toLowerCase();
                    System.out.println("[AgentHydratation] Analyse : \"" + text.substring(0, Math.min(60, text.length())) + "...\"");

                    List<String> signals  = detectSignals(text);
                    int          score    = computeScore(text, signals); 
                    AgentProposal proposal = buildProposal(signals, score);
                    sendProposal(msg.getConversationId(), proposal);
                } else {
                    block();
                }
            }
        });
    }

    private List<String> detectSignals(String text) {
        List<String> found = new ArrayList<>();
        for (String kw : KEYWORDS_TETE)  if (text.contains(kw)) found.add(kw);
        for (String kw : KEYWORDS_SOIF)  if (text.contains(kw)) found.add(kw);
        for (String kw : KEYWORDS_DUREE) if (text.contains(kw)) found.add(kw);
        return found;
    }

    private int computeScore(String text, List<String> signals) {
        int score = 3; // base
        if (containsAny(text, KEYWORDS_TETE))  score += 3;
        if (containsAny(text, KEYWORDS_SOIF))  score += 2;
        if (containsAny(text, KEYWORDS_DUREE)) score += 2;
        // Bonus si combinaison tête + durée (cas classique de déshydratation)
        if (containsAny(text, KEYWORDS_TETE) && containsAny(text, KEYWORDS_DUREE)) score += 1;
        return Math.min(score, 10); // Score max 10 pour éviter les propositions trop extrêmes
    }
    // Méthode utilitaire pour vérifier si le texte contient au moins un mot-clé d'une liste
    private boolean containsAny(String text, String[] kws) {
        for (String kw : kws) if (text.contains(kw)) return true;
        return false;
    }

    private AgentProposal buildProposal(List<String> signals, int score) {
        List<String> args = new ArrayList<>();
        args.add("La déshydratation est responsable de 60% des maux de tête de mi-journée selon l'OMS");

        if (!signals.isEmpty() && signals.stream().anyMatch(s -> s.contains("h") || s.contains("longtemps") || s.contains("matin"))) {
            args.add("Travailler plusieurs heures sans boire réduit les capacités cognitives de 15-20%");
        } else {
            args.add("Une bonne hydratation améliore la concentration et réduit la fatigue mentale");
        }
        if (!signals.isEmpty()) {
            args.add("Signaux détectés : " + String.join(", ", signals.subList(0, Math.min(2, signals.size()))));
        }

        return new AgentProposal(
            AgentType.HYDRATATION,
            "Agent Hydratation",
            "Boire un grand verre d'eau (300 ml)",
            "OPT2001",
            score,
            args,
            signals,
            "💧"
        );
    }
    // Méthode pour envoyer la proposition d'activité à AgentRaison avec le format attendu
    private void sendProposal(String convId, AgentProposal proposal) {
        try {
            ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
            reply.addReceiver(getAID("AgentRaison"));
            reply.setContent(MAPPER.writeValueAsString(proposal));
            reply.setConversationId(convId);
            reply.setOntology("wellness-proposal");
            send(reply);
            System.out.printf("[AgentHydratation] ✓ Proposition envoyée → score=%d/10 action=%s%n",
                proposal.getImportance(), proposal.getAction());
        } catch (Exception e) {
            System.err.println("[AgentHydratation] Erreur envoi : " + e.getMessage());
        }
    }
}
