package sma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Décision finale produite par AgentRaison après appel à l'API rAIson.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WellnessDecision {

    private boolean      success;
    private String       recommendation;
    private String       recommendationEmoji;
    private List<String> reasoning;
    private List<String> secondaryActions;
    private List<AgentProposal> agentProposals;
    private String       raisonRawResponse;
    private String       errorMessage;

    // ── Factories ────────────────────────────────────────────
    public static WellnessDecision ok(String recommendation, String emoji,
                                       List<String> reasoning, List<String> secondary,
                                       List<AgentProposal> proposals, String raw) {
        WellnessDecision d = new WellnessDecision();
        d.success             = true;
        d.recommendation      = recommendation;
        d.recommendationEmoji = emoji;
        d.reasoning           = reasoning;
        d.secondaryActions    = secondary;
        d.agentProposals      = proposals;
        d.raisonRawResponse   = raw;
        return d;
    }
      // Factory pour créer une décision d'erreur, avec un message et les propositions des agents (pour le debug)
    public static WellnessDecision error(String msg, List<AgentProposal> proposals) {
        WellnessDecision d = new WellnessDecision();
        d.success        = false;
        d.errorMessage   = msg;
        d.agentProposals = proposals;
        return d;
    }

    // ── Getters / Setters ────────────────────────────────────
    public boolean      isSuccess()                         { return success; }
    public void         setSuccess(boolean s)               { this.success = s; }
    public String       getRecommendation()                 { return recommendation; }
    public void         setRecommendation(String r)         { this.recommendation = r; }
    public String       getRecommendationEmoji()            { return recommendationEmoji; }
    public void         setRecommendationEmoji(String e)    { this.recommendationEmoji = e; }
    public List<String> getReasoning()                      { return reasoning; }
    public void         setReasoning(List<String> r)        { this.reasoning = r; }
    public List<String> getSecondaryActions()               { return secondaryActions; }
    public void         setSecondaryActions(List<String> s) { this.secondaryActions = s; }
    public List<AgentProposal> getAgentProposals()          { return agentProposals; }
    public void         setAgentProposals(List<AgentProposal> p){ this.agentProposals = p; }
    public String       getRaisonRawResponse()              { return raisonRawResponse; }
    public void         setRaisonRawResponse(String r)      { this.raisonRawResponse = r; }
    public String       getErrorMessage()                   { return errorMessage; }
    public void         setErrorMessage(String e)           { this.errorMessage = e; }
}
