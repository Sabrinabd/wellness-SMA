export type AgentType = "HYDRATATION" | "FATIGUE" | "ACTIVITE";

export interface AgentProposal {
  agentType: AgentType;
  agentName: string;
  action: string;
  actionId: string;
  importance: number;
  arguments: string[];
  detectedSignals: string[];
  emoji: string;
}

export interface WellnessDecision {
  success: boolean;
  recommendation: string;
  recommendationEmoji: string;
  reasoning: string[];
  secondaryActions: string[];
  agentProposals: AgentProposal[];
  raisonRawResponse?: string;
  errorMessage?: string;
}

export const AGENT_STYLES: Record<AgentType, { color: string; light: string; border: string; gradient: string }> = {
  HYDRATATION: { color: "#0ea5e9", light: "#e0f2fe", border: "#bae6fd", gradient: "from-sky-400 to-blue-500" },
  FATIGUE:     { color: "#8b5cf6", light: "#ede9fe", border: "#ddd6fe", gradient: "from-violet-400 to-purple-500" },
  ACTIVITE:    { color: "#10b981", light: "#d1fae5", border: "#a7f3d0", gradient: "from-emerald-400 to-green-500" },
};
