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
 * Agent Fatigue 
 *
 * Rôle : détecter fatigue mentale, stress, surcharge cognitive.
 *        Propose une pause ou exercice de respiration selon l'intensité.
 */
public class AgentFatigue extends Agent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String[] KEYWORDS_FATIGUE = { // Mots-clés liés à la fatigue mentale et physique
        "fatigué", "fatigue", "épuisé", "épuisement", "crevé",
        "sans énergie", "vidé", "à plat", "exhausted"
    };
    private static final String[] KEYWORDS_MENTAL = { // Mots-clés liés à la difficulté de concentration et brouillard mental
        "concentrer", "concentration", "focus", "distrait",
        "tête vide", "brouillard", "brain fog", "difficile de penser",
        "plus me concentrer", "perdu le fil"
    };
    private static final String[] KEYWORDS_STRESS = { // Mots-clés liés au stress, anxiété et surcharge émotionnelle
        "stressé", "stress", "anxieux", "anxiété", "nerveux",
        "tendu", "pression", "débordé", "surchargé", "overwhelmed",
        "angoissé", "oppressé"
    };
    private static final String[] KEYWORDS_PAUSE = { // Mots-clés liés au besoin de pause et durée d'activité prolongée
        "sans pause", "sans arrêt", "3h", "4h", "5h", "6h",
        "toute la journée", "depuis ce matin", "longtemps", "des heures"
    };

    @Override
    protected void setup() {
        System.out.println("[AgentFatigue]  Prêt — En écoute...");
       // Filtre pour recevoir uniquement les messages de type REQUEST avec l'ontologie "wellness-request"
        MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("wellness-request")
        );
        
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String text = msg.getContent().toLowerCase();
                    System.out.println("[AgentFatigue] Analyse : \"" + text.substring(0, Math.min(60, text.length())) + "...\"");

                    List<String> signals = detectSignals(text);
                    boolean highStress   = containsAny(text, KEYWORDS_STRESS); 
                    int score            = computeScore(text, signals);
                    AgentProposal proposal = buildProposal(signals, score, highStress);

                    sendProposal(msg.getConversationId(), proposal);
                } else {
                    block();
                }
            }
        });
    }

    private List<String> detectSignals(String text) {
        List<String> found = new ArrayList<>();
        for (String kw : KEYWORDS_FATIGUE) if (text.contains(kw)) found.add(kw);
        for (String kw : KEYWORDS_MENTAL)  if (text.contains(kw)) found.add(kw);
        for (String kw : KEYWORDS_STRESS)  if (text.contains(kw)) found.add(kw);
        for (String kw : KEYWORDS_PAUSE)   if (text.contains(kw)) found.add(kw);
        return found;
    }

    private int computeScore(String text, List<String> signals) {
        int score = 3;
        if (containsAny(text, KEYWORDS_FATIGUE)) score += 2;
        if (containsAny(text, KEYWORDS_MENTAL))  score += 2;
        if (containsAny(text, KEYWORDS_STRESS))  score += 2;
        if (containsAny(text, KEYWORDS_PAUSE))   score += 1;
        return Math.min(score, 10);
    }

    private boolean containsAny(String text, String[] kws) {
        for (String kw : kws) if (text.contains(kw)) return true;
        return false;
    }

    private AgentProposal buildProposal(List<String> signals, int score, boolean highStress) {
        String action, actionId, emoji;
        List<String> args = new ArrayList<>();

        if (highStress || score >= 7) {
            action   = "Exercice de respiration 4-7-8 (1 minute)";
            actionId = "OPT2004";
            emoji    = "🧘";
            args.add("La technique 4-7-8 active le système nerveux parasympathique en 60 secondes et réduit le cortisol");
            args.add("Cliniquement prouvée pour réduire l'anxiété et restaurer la clarté mentale immédiatement");
        } else {
            action   = "Pause de 5 à 10 minutes (méthode Pomodoro)";
            actionId = "OPT2002";
            emoji    = "☕";
            args.add("La méthode Pomodoro recommande une pause toutes les 90 min pour maintenir la productivité optimale");
            args.add("Le repos cognitif permet la consolidation mémorielle et restaure la capacité d'attention");
        }

        if (!signals.isEmpty()) {
            args.add("Signaux détectés : " + String.join(", ", signals.subList(0, Math.min(2, signals.size()))));
        }

        return new AgentProposal(AgentType.FATIGUE, "Agent Fatigue",
            action, actionId, score, args, signals, emoji);
    }
    // Méthode pour envoyer la proposition d'activité à AgentRaison avec le format attendu
    private void sendProposal(String convId, AgentProposal proposal) {
        try {
            ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
            reply.addReceiver(getAID("AgentRaison"));// Envoi à AgentRaison
            reply.setContent(MAPPER.writeValueAsString(proposal)); 
            reply.setConversationId(convId); 
            reply.setOntology("wellness-proposal"); 
            send(reply); // Envoi du message
            System.out.printf("[AgentFatigue] ✓ Proposition envoyée → score=%d/10 action=%s%n",
                proposal.getImportance(), proposal.getAction());
        } catch (Exception e) {
            System.err.println("[AgentFatigue] Erreur envoi : " + e.getMessage());
        }
    }
}
