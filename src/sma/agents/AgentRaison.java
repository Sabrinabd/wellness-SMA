package sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import sma.api.RaisonAPIClient;
import sma.api.RaisonPayloadBuilder;
import sma.model.AgentProposal;
import sma.model.WellnessDecision;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Agent Raison - Chef d'orchestre du SMA

 */
public class AgentRaison extends Agent {

    private static final ObjectMapper MAPPER    = new ObjectMapper(); // Pour construire les payloads JSON
    private static final int          NB_AGENTS = 3; 

    // Queue HTTP → JADE (thread-safe)
    private final LinkedBlockingQueue<String[]> requestQueue = new LinkedBlockingQueue<>();

    // convId → propositions reçues
    private final Map<String, List<AgentProposal>> pending    = new ConcurrentHashMap<>();

    // convId → décision terminée (JAMAIS null — ConcurrentHashMap interdit null!)
    private final Map<String, WellnessDecision>    decisions  = new ConcurrentHashMap<>(); 

    // convId des analyses en cours (remplace decisions.put(id, null))
    private final Set<String> inProgress = ConcurrentHashMap.newKeySet();

    private RaisonAPIClient apiClient;

    @Override
    protected void setup() {
        apiClient = new RaisonAPIClient();
        System.out.println("[AgentRaison] Pret - Orchestrateur SMA actif");

        // ── Behaviour 1 : poll requestQueue toutes les 100ms ──
        // Le seul endroit qui appelle send() → thread JADE garanti
        addBehaviour(new TickerBehaviour(this, 100) { 
            @Override
            protected void onTick() {
                String[] req;
                while ((req = requestQueue.poll()) != null) {
                    String userText = req[0];
                    String convId   = req[1];
                    System.out.printf("[AgentRaison] Traitement [%s] : \"%s\"%n",
                        convId, userText.substring(0, Math.min(60, userText.length())));
                    broadcast(userText, convId); 
                }
            }
        });

        // ── Behaviour 2 : collecte les propositions des agents ──
        addBehaviour(new CyclicBehaviour() {
            final MessageTemplate MT = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchOntology("wellness-proposal")
            );

            @Override
            public void action() {
                ACLMessage msg = receive(MT);
                if (msg != null) {
                    String convId = msg.getConversationId();
                    try {
                        AgentProposal p = MAPPER.readValue(msg.getContent(), AgentProposal.class);
                        List<AgentProposal> list = pending.get(convId);
                        if (list != null) {
                            list.add(p);
                            System.out.printf("[AgentRaison] <- %s  score=%d%n",
                                p.getAgentName(), p.getImportance());

                            if (list.size() >= NB_AGENTS) {
                                processDecision(convId, new ArrayList<>(list));
                                pending.remove(convId);
                                inProgress.remove(convId);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[AgentRaison] Erreur parsing : " + e.getMessage());
                    }
                } else {
                    block();
                }
            }
        });
    }
    // ── Méthodes utilitaires pour le traitement des propositions et la prise de décision ──
    private void broadcast(String userText, String convId) {// Envoie le texte de l'utilisateur à tous les agents spécialisés pour analyse
        for (String name : new String[]{"AgentHydratation", "AgentFatigue", "AgentActivite"}) {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID(name, AID.ISLOCALNAME));
            msg.setContent(userText);
            msg.setConversationId(convId);
            msg.setOntology("wellness-request");
            send(msg);
        }
        System.out.printf("[AgentRaison] -> Broadcast envoye a %d agents%n", NB_AGENTS);
    }
    
    // Processus de décision basé sur les propositions reçues et interaction avec rAIson
    private void processDecision(String convId, List<AgentProposal> proposals) {
        System.out.println("[AgentRaison] === DECISION EN COURS ===");
        proposals.forEach(p -> System.out.printf("  * %-22s %d/10  %s%n",
            p.getAgentName(), p.getImportance(), p.getAction()));

        proposals.sort(Comparator.comparingInt(AgentProposal::getImportance).reversed());

        WellnessDecision decision;
        try {// Construction du payload pour rAIson à partir des propositions reçues
            ObjectNode payload = RaisonPayloadBuilder.buildPayload(proposals);
            String raw         = apiClient.runExecution(payload);
            decision           = buildDecisionFromRaison(proposals, raw);
            System.out.println("[AgentRaison] Recommandation : " + decision.getRecommendation());
        } catch (Exception e) {
            System.err.println("[AgentRaison] Erreur rAIson : " + e.getMessage()
                + " -> fallback local");
            decision = buildLocalDecision(proposals, "Fallback local - " + e.getMessage());
        }

        // (ConcurrentHashMap interdit null)
        decisions.put(convId, decision);
    }

    // Méthode pour construire la décision finale à partir de la recommandation brute de rAIson et des propositions reçues
    private WellnessDecision buildDecisionFromRaison(List<AgentProposal> proposals, String raw) {
        AgentProposal best = proposals.get(0);

        // raw = null si rAIson retourne tout isSolution=false (conflits non résolus)
        if (raw == null || raw.isBlank()) {
            System.out.println("[AgentRaison] rAIson sans décision → fallback score JADE");
            return buildLocalDecision(proposals,
                "rAIson n\'a pas pu décider (conflits non résolus) - fallback JADE score");
        }

        // Mappe le label rAIson vers une action complète avec emoji
        // Prend le premier si plusieurs solutions séparées par "|"
        String winner = raw.contains("|") ? raw.split("\\|")[0] : raw;
        String recommendation = mapRaisonLabel(winner, best);
        String emoji = getEmoji(winner, best.getEmoji());

        System.out.println("[AgentRaison] DECISION rAIson : \"" + winner + "\" -> " + recommendation);

        List<String> reasoning = new ArrayList<>();
        reasoning.add("rAIson PRJ29975 a selectionne : \"" + winner + "\"");
        reasoning.addAll(buildReasoning(proposals, best));

        return WellnessDecision.ok(recommendation, emoji,
            reasoning, buildSecondary(proposals), proposals,
            "rAIson winner: " + winner);
    }
    // Méthodes pour construire une décision locale basée sur le score des propositions reçues (fallback en cas d'erreur rAIson)
    private String mapRaisonLabel(String label, AgentProposal fallback) {// Mappe le label rAIson vers une action complète avec emoji
        if (label == null || label.isBlank()) return fallback.getEmoji() + " " + fallback.getAction();
        String l = label.toLowerCase();
        // 
        if (l.contains("boire") || l.contains("eau"))          return "Boire un grand verre d'eau (300 ml)";
        if (l.contains("pause"))                                return "Faire une pause de 5 a 10 minutes";
        if (l.contains("march") || l.contains("marcher"))      return "Marcher 5 minutes";
        if (l.contains("respir"))                               return "Exercice de respiration 4-7-8 (1 minute)";
        if (l.contains("organ"))                                return "Organiser ses taches et priorites";
        return label;
    }

    // Méthode pour mapper un label rAIson vers un emoji, avec fallback sur l'emoji de la proposition gagnante
    private String getEmoji(String label, String fallback) {
        if (label == null) return fallback;
        String l = label.toLowerCase();
        if (l.contains("boire") || l.contains("eau"))     return "💧";
        if (l.contains("pause"))                          return "☕";
        if (l.contains("march"))                          return "🚶";
        if (l.contains("respir"))                         return "🧘";
        if (l.contains("organ"))                          return "📋";
        return fallback;
    }

    // Méthode pour construire une décision locale basée sur le score des propositions reçues (fallback en cas d'erreur rAIson)
    private WellnessDecision buildLocalDecision(List<AgentProposal> proposals, String note) {
        AgentProposal best = proposals.get(0);
        List<String> reasoning = new ArrayList<>();
        reasoning.add("Score le plus eleve : " + best.getImportance() + "/10 -> " + best.getAgentName());
        reasoning.addAll(best.getArguments());
        return WellnessDecision.ok(
            best.getEmoji() + " " + best.getAction(),
            best.getEmoji(), reasoning, buildSecondary(proposals), proposals, note);
    }
    // Méthode pour construire la partie "reasoning" de la décision finale à partir des propositions reçues et de la proposition gagnante
    private List<String> buildReasoning(List<AgentProposal> proposals, AgentProposal best) {
        List<String> r = new ArrayList<>();
        r.add(best.getAgentName() + " attribue le score maximum : " + best.getImportance() + "/10");
        r.addAll(best.getArguments());
        if (best.getDetectedSignals() != null && !best.getDetectedSignals().isEmpty()) {
            r.add("Signaux detectes : " + String.join(", ",
                best.getDetectedSignals().subList(0,
                    Math.min(3, best.getDetectedSignals().size()))));
        }
        return r;
    }

    private List<String> buildSecondary(List<AgentProposal> proposals) {
        List<String> sec = new ArrayList<>();
        for (int i = 1; i < proposals.size(); i++) {
            AgentProposal p = proposals.get(i);
            sec.add(p.getEmoji() + " " + p.getAction());
        }
        return sec;
    }

    // ── API publique appelée par WellnessHttpServer ─────────────

    public void submitRequest(String userText, String convId) {
        inProgress.add(convId);                                     // marque comme "en cours"
        pending.put(convId, new CopyOnWriteArrayList<>());          // prépare la liste
        requestQueue.offer(new String[]{userText, convId});          // queue → TickerBehaviour
        System.out.printf("[AgentRaison] Requete en queue [%s]%n", convId);
    }

    /**
     * Attend la décision (appelé depuis le thread HTTP).
     * Retourne quand decisions.containsKey(convId) est vrai.
     */
    public WellnessDecision waitForDecision(String convId, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            // On utilise containsKey car on ne met JAMAIS null dans decisions
            if (decisions.containsKey(convId)) {
                return decisions.remove(convId);
            }
            Thread.sleep(200);
        }
        // Nettoyage en cas de timeout
        inProgress.remove(convId);
        pending.remove(convId);
        throw new RuntimeException("Timeout " + timeoutMs + "ms pour convId=" + convId);
    }
}
