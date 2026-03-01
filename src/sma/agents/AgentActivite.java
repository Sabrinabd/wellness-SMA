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
 * Agent Activité 
 *
 * Rôle : détecter sédentarité, tensions physiques, surcharge écran.
 *        Propose une activité physique légère adaptée.
 */
public class AgentActivite extends Agent {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private static final String[] KEYWORDS_SEDENTAIRE = {  // Mots-clés liés à la sédentarité et posture
        "assis", "bureau", "chaise", "posture", "raide", "dos",
        "nuque", "épaules", "mal au dos", "courbatures", "sitting"
    };
    private static final String[] KEYWORDS_ECRAN = {   // Mots-clés liés à l'utilisation d'écrans et fatigue oculaire
        "écran", "ordinateur", "pc", "laptop", "télé", "téléphone",
        "yeux fatigués", "yeux secs", "vision floue", "screen", "moniteur"
    };
    private static final String[] KEYWORDS_TENSION = {  // Mots-clés liés à la tension musculaire et fatigue physique
        "tendu", "crispé", "tension", "raideur", "contracté",
        "douleur", "muscles", "courbé", "tight", "bloqué"
    };
    private static final String[] KEYWORDS_ENERGIE = {  // Mots-clés liés à la fatigue générale et manque d'énergie
        "manque d'énergie", "pas d'énergie", "mou", "apathique",
        "léthargique", "lourd", "lent", "sluggish", "sans motivation"
    };

    @Override
    protected void setup() {    
        System.out.println("[AgentActivite]  Prêt — En écoute...");

        MessageTemplate mt = MessageTemplate.and(  
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
            MessageTemplate.MatchOntology("wellness-request")
        );
        // Comportement principal : analyse des messages et proposition d'activités
        addBehaviour(new CyclicBehaviour() {   
            @Override
            public void action() {
                ACLMessage msg = receive(mt); 

                if (msg != null) { // Message reçu → analyse et proposition
                    String text = msg.getContent().toLowerCase();   
                    System.out.println("[AgentActivite] Analyse : \"" + text.substring(0, Math.min(60, text.length())) + "...\"");
                    // Détection de signaux clés dans le texte
                    List<String> signals = detectSignals(text);
                    boolean hasTension   = containsAny(text, KEYWORDS_TENSION);
                    boolean hasEcran     = containsAny(text, KEYWORDS_ECRAN);
                    int score            = computeScore(text, signals);
                    AgentProposal proposal = buildProposal(signals, score, hasTension, hasEcran); 

                    sendProposal(msg.getConversationId(), proposal);  // Envoi de la proposition à AgentRaison
                } else {
                    block();
                }
            }
        });
    }
    // Méthodes d'analyse du texte pour détecter les signaux et construire la proposition d'activité
    private List<String> detectSignals(String text) {
        List<String> found = new ArrayList<>();
        for (String kw : KEYWORDS_SEDENTAIRE) if (text.contains(kw)) found.add(kw);  
        for (String kw : KEYWORDS_ECRAN)      if (text.contains(kw)) found.add(kw);
        for (String kw : KEYWORDS_TENSION)    if (text.contains(kw)) found.add(kw);
        for (String kw : KEYWORDS_ENERGIE)    if (text.contains(kw)) found.add(kw);
        return found;
    }

    private int computeScore(String text, List<String> signals) {
        int score = 3;
        if (containsAny(text, KEYWORDS_SEDENTAIRE)) score += 2; // Sédentarité détectée → score plus élevé
        if (containsAny(text, KEYWORDS_TENSION))    score += 3;// Tension musculaire → score très élevé
        if (containsAny(text, KEYWORDS_ECRAN))      score += 1;// Utilisation d'écrans → score modéré
        if (containsAny(text, KEYWORDS_ENERGIE))    score += 1; // Fatigue générale → score modéré
        return Math.min(score, 10);
    }
    // Méthode utilitaire pour vérifier si le texte contient au moins un mot-clé d'une liste
    private boolean containsAny(String text, String[] kws) {
        for (String kw : kws) if (text.contains(kw)) return true;
        return false;
    }
    // Méthode pour construire la proposition d'activité basée sur les signaux détectés et le score d'importance
    private AgentProposal buildProposal(List<String> signals, int score,
                                         boolean tension, boolean ecran) {
        String action, actionId, emoji;
        List<String> args = new ArrayList<>();

        if (tension && score >= 6) {
            action   = "Étirements cou + épaules (5 minutes)";
            actionId = "OPT2003";
            emoji    = "🤸";
            args.add("Les étirements ciblés libèrent les points de tension musculaire et réduisent les céphalées de tension de 30%");
            args.add("Un auto-massage des épaules de 5 minutes améliore la circulation sanguine et la posture");
        } else if (ecran && score >= 5) {
            action   = "Règle 20-20-20 : 20 sec au loin + marche courte";
            actionId = "OPT2003";
            emoji    = "👁️";
            args.add("La règle 20-20-20 recommandée par les ophtalmologues réduit la fatigue oculaire numérique");
            args.add("Associer repos visuel et mouvement optimise la récupération globale");
        } else {
            action   = "Marcher 5 minutes (escaliers ou extérieur)";
            actionId = "OPT2003";
            emoji    = "🚶";
            args.add("5 minutes de marche augmentent le flux sanguin cérébral de 15% et améliorent l'humeur");
            args.add("Le mouvement libère des endorphines et brise le cycle sédentarité-fatigue");
        }
         
        if (!signals.isEmpty()) {
            args.add("Signaux détectés : " + String.join(", ", signals.subList(0, Math.min(2, signals.size()))));
        }

        return new AgentProposal(AgentType.ACTIVITE, "Agent Activité",
            action, actionId, score, args, signals, emoji);
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
            System.out.printf("[AgentActivite] ✓ Proposition envoyée → score=%d/10 action=%s%n", 
                proposal.getImportance(), proposal.getAction());
        } catch (Exception e) {
            System.err.println("[AgentActivite] Erreur envoi : " + e.getMessage()); 
        }
    }
}
