import { useState } from "react";
import { CheckCircle2, Brain, TrendingUp, ChevronDown, ChevronUp } from "lucide-react";
import type { WellnessDecision } from "../types";

interface Props { decision: WellnessDecision; }

export default function RaisonResult({ decision }: Props) {
  const [showRaw, setShowRaw] = useState(false);

  return (
    <div className="space-y-4 anim-fade-in">
      {/* Carte principale */}
      <div className="relative rounded-2xl border-2 border-blue-400 bg-gradient-to-br from-blue-50 via-indigo-50 to-blue-50 p-6 shadow-lg overflow-hidden">
        {/* Déco */}
        <div className="absolute -top-10 -right-10 w-40 h-40 rounded-full bg-blue-100 opacity-40" />
        <div className="absolute -bottom-8 -left-8 w-28 h-28 rounded-full bg-indigo-100 opacity-40" />

        <div className="relative">
          {/* Badge */}
          <div className="flex items-center gap-2 mb-5">
            <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-600 text-white text-xs font-bold shadow-sm">
              <Brain className="w-3.5 h-3.5" />
              Décision rAIson · PRJ28725
            </div>
            <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-green-100 text-green-700 text-xs font-medium border border-green-200">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
              Analyse terminée
            </div>
          </div>

          {/* Recommandation */}
          <div className="mb-5">
            <p className="text-xs font-semibold uppercase tracking-wider text-blue-500 mb-2">
              Recommandation finale
            </p>
            <div className="flex items-start gap-3">
              <CheckCircle2 className="w-6 h-6 text-blue-500 flex-shrink-0 mt-0.5" />
              <p className="text-xl font-bold text-gray-900 leading-snug">{decision.recommendation}</p>
            </div>
          </div>

          {/* Raisonnement */}
          <div>
            <div className="flex items-center gap-2 mb-3">
              <TrendingUp className="w-4 h-4 text-indigo-500" />
              <h3 className="font-semibold text-gray-700 text-sm">Pourquoi cette décision ?</h3>
            </div>
            <div className="space-y-2 pl-6">
              {(decision.reasoning || []).map((r, i) => (
                <div
                  key={i}
                  className="flex gap-2.5 anim-fade-in"
                  style={{ animationDelay: `${i * 80 + 200}ms` }}
                >
                  <span className="text-blue-500 font-bold text-sm flex-shrink-0">✓</span>
                  <p className="text-sm text-gray-700 leading-relaxed">{r}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Actions secondaires */}
      {(decision.secondaryActions || []).length > 0 && (
        <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-3">
            Actions complémentaires
          </p>
          <div className="flex flex-wrap gap-2">
            {decision.secondaryActions.map((a, i) => (
              <span
                key={i}
                className="text-sm px-3 py-1.5 rounded-full border border-gray-200 bg-gray-50 text-gray-600 font-medium"
              >{a}</span>
            ))}
          </div>
        </div>
      )}

      {/* Debug rAIson */}
      {decision.raisonRawResponse && (
        <div className="rounded-xl border border-gray-100 overflow-hidden">
          <button
            onClick={() => setShowRaw(v => !v)}
            className="w-full flex items-center justify-between px-4 py-3 text-xs text-gray-400 hover:text-gray-600 hover:bg-gray-50 transition-colors"
          >
            <span>🔍 Réponse brute rAIson (debug)</span>
            {showRaw ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>
          {showRaw && (
            <pre className="bg-gray-50 px-4 pb-4 text-xs text-gray-500 overflow-x-auto whitespace-pre-wrap break-all">
              {decision.raisonRawResponse}
            </pre>
          )}
        </div>
      )}
    </div>
  );
}
