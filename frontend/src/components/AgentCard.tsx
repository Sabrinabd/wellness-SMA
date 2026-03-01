import type { AgentProposal, AgentType } from "../types";
import { AGENT_STYLES } from "../types";

interface Props {
  proposal: AgentProposal;
  rank: number;
  delay?: number;
}

function importanceBadge(score: number) {
  if (score >= 8) return { label: "Critique", cls: "bg-red-50 text-red-600 border-red-200" };
  if (score >= 6) return { label: "Élevé",    cls: "bg-orange-50 text-orange-600 border-orange-200" };
  if (score >= 4) return { label: "Modéré",   cls: "bg-yellow-50 text-yellow-600 border-yellow-200" };
  return            { label: "Faible",    cls: "bg-gray-50 text-gray-500 border-gray-200" };
}

export default function AgentCard({ proposal, rank, delay = 0 }: Props) {
  const s    = AGENT_STYLES[proposal.agentType as AgentType];
  const badge = importanceBadge(proposal.importance);
  const isWinner = rank === 1;

  return (
    <div
      className="anim-slide-up relative rounded-2xl border-2 p-5 transition-all duration-300"
      style={{
        borderColor: isWinner ? s.color : "#e5e7eb",
        backgroundColor: isWinner ? s.light : "#fafafa",
        animationDelay: `${delay}ms`,
        boxShadow: isWinner ? `0 4px 24px ${s.color}25` : undefined,
      }}
    >
      {/* Badge winner */}
      {isWinner && (
        <div
          className="absolute -top-3 -right-3 w-8 h-8 rounded-full flex items-center justify-center text-white text-sm font-bold shadow-lg"
          style={{ backgroundColor: s.color }}
        >★</div>
      )}

      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-3">
          <div
            className="w-11 h-11 rounded-xl flex items-center justify-center text-2xl border-2"
            style={{ backgroundColor: s.light, borderColor: s.border }}
          >
            {proposal.emoji}
          </div>
          <div>
            <h3 className="font-bold text-gray-800 text-sm leading-none">{proposal.agentName}</h3>
            <p className="text-xs text-gray-400 mt-0.5">Agent spécialiste JADE</p>
          </div>
        </div>

        <div className="text-right">
          <div className="text-2xl font-black" style={{ color: s.color }}>
            {proposal.importance}
            <span className="text-sm font-normal text-gray-400">/10</span>
          </div>
          <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${badge.cls}`}>
            {badge.label}
          </span>
        </div>
      </div>

      {/* Barre de score */}
      <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden mb-4">
        <div
          className="h-full rounded-full"
          style={{
            width: `${proposal.importance * 10}%`,
            background: `linear-gradient(to right, ${s.color}80, ${s.color})`,
            transition: "width 1s ease-out",
          }}
        />
      </div>

      {/* Action proposée */}
      <div
        className="rounded-xl p-3 mb-4 border"
        style={{ backgroundColor: `${s.color}10`, borderColor: `${s.color}30` }}
      >
        <p className="text-xs font-semibold uppercase tracking-wider mb-1" style={{ color: s.color }}>
          Action proposée
        </p>
        <p className="font-semibold text-gray-800 text-sm leading-relaxed">{proposal.action}</p>
      </div>

      {/* Arguments */}
      <div className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-wider text-gray-400">Arguments</p>
        {(proposal.arguments || []).map((arg, i) => (
          <div key={i} className="flex gap-2.5">
            <span
              className="flex-shrink-0 w-5 h-5 rounded-full flex items-center justify-center text-white text-xs font-bold mt-0.5"
              style={{ backgroundColor: s.color }}
            >{i + 1}</span>
            <p className="text-sm text-gray-600 leading-relaxed">{arg}</p>
          </div>
        ))}
      </div>

      {/* Signaux */}
      {(proposal.detectedSignals || []).length > 0 && (
        <div className="mt-4 pt-3 border-t border-gray-100">
          <p className="text-xs text-gray-400 mb-1.5">Signaux détectés :</p>
          <div className="flex flex-wrap gap-1.5">
            {proposal.detectedSignals.slice(0, 4).map((sig, i) => (
              <span
                key={i}
                className="text-xs px-2.5 py-0.5 rounded-full font-medium"
                style={{ backgroundColor: `${s.color}15`, color: s.color }}
              >{sig}</span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
