package sma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Proposition structurée émise par chaque agent spécialiste.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentProposal {

    public enum AgentType { HYDRATATION, FATIGUE, ACTIVITE }

    private AgentType agentType;
    private String    agentName;
    private String    action;
    private String    actionId;
    private int       importance;       // 0-10
    private List<String> arguments;
    private List<String> detectedSignals;
    private String    emoji;

    public AgentProposal() {}
     // Constructeur complet pour faciliter la création d'instances avec tous les champs
    public AgentProposal(AgentType agentType, String agentName, String action,
                         String actionId, int importance,
                         List<String> arguments, List<String> detectedSignals,
                         String emoji) {
        this.agentType       = agentType;
        this.agentName       = agentName;
        this.action          = action;
        this.actionId        = actionId;
        this.importance      = importance;
        this.arguments       = arguments;
        this.detectedSignals = detectedSignals;
        this.emoji           = emoji;
    }

    // ── Getters / Setters ────────────────────────────────────
    public AgentType    getAgentType()                  { return agentType; }
    public void         setAgentType(AgentType t)       { this.agentType = t; }
    public String       getAgentName()                  { return agentName; }
    public void         setAgentName(String n)          { this.agentName = n; }
    public String       getAction()                     { return action; }
    public void         setAction(String a)             { this.action = a; }
    public String       getActionId()                   { return actionId; }
    public void         setActionId(String id)          { this.actionId = id; }
    public int          getImportance()                 { return importance; }
    public void         setImportance(int i)            { this.importance = i; }
    public List<String> getArguments()                  { return arguments; }
    public void         setArguments(List<String> args) { this.arguments = args; }
    public List<String> getDetectedSignals()            { return detectedSignals; }
    public void         setDetectedSignals(List<String> s){ this.detectedSignals = s; }
    public String       getEmoji()                      { return emoji; }
    public void         setEmoji(String e)              { this.emoji = e; }
}
