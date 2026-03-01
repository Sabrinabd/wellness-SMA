import { useState, useCallback, useEffect } from "react";
import UserInput from "./components/UserInput";
import AgentCard from "./components/AgentCard";
import RaisonResult from "./components/RaisonResult";
import AnalyzingLoader from "./components/AnalyzingLoader";
import { api } from "./services/api";
import { AlertTriangle, RefreshCw, Info, Wifi, WifiOff } from "lucide-react";
import type { WellnessDecision } from "./types";

type Phase = "input" | "analyzing" | "result";

export default function App() {
  const [phase, setPhase] = useState<Phase>("input");
  const [input, setInput] = useState("");
  const [decision, setDecision] = useState<WellnessDecision | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [online, setOnline] = useState<boolean | null>(null);

  // Vérifier que le backend est up
  useEffect(() => {
    api.health().then((ok) => setOnline(ok));
  }, []);

  const handleAnalyze = useCallback(async (desc: string) => {
    setInput(desc);
    setPhase("analyzing");
    setError(null);
    setDecision(null);
    try {
      const d = await api.analyze(desc);
      setDecision(d);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setPhase("result");
    }
  }, []);

  const reset = () => {
    setPhase("input");
    setDecision(null);
    setError(null);
  };

  const sorted = decision?.agentProposals
    ? [...decision.agentProposals].sort((a, b) => b.importance - a.importance)
    : [];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      {/* ── HEADER ──────────────────────────────────────── */}
      <header className="sticky top-0 z-20 border-b border-white/60 bg-white/80 backdrop-blur-sm shadow-sm">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-xl shadow-md">
              🌿
            </div>
            <div>
              <h1 className="font-bold text-gray-900 text-base leading-none">
                Wellness SMA
              </h1>
              <p className="text-xs text-gray-400 leading-none mt-0.5">
                AI-Powered Multi-Agent Wellness System
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Status backend */}
            {online !== null && (
              <div
                className={`flex items-center gap-1.5 text-xs px-3 py-1 rounded-full border font-medium ${
                  online
                    ? "bg-green-50 text-green-700 border-green-200"
                    : "bg-red-50 text-red-600 border-red-200"
                }`}
              >
                {online ? (
                  <Wifi className="w-3.5 h-3.5" />
                ) : (
                  <WifiOff className="w-3.5 h-3.5" />
                )}
                {online ? "Backend connecté" : "Backend hors ligne"}
              </div>
            )}

            {phase !== "input" && (
              <button
                onClick={reset}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white border border-gray-200 text-sm text-gray-600 hover:bg-gray-50 transition-colors shadow-sm"
              >
                <RefreshCw className="w-3.5 h-3.5" /> Nouvelle analyse
              </button>
            )}
          </div>
        </div>
      </header>

      {/* ── MAIN ────────────────────────────────────────── */}
      <main className="max-w-5xl mx-auto px-4 py-8">
        {/* PHASE : INPUT */}
        {phase === "input" && (
          <div className="max-w-xl mx-auto space-y-4">
            {/* Avertissement backend offline */}
            {online === false && (
              <div className="rounded-xl border border-orange-200 bg-orange-50 p-4 flex items-start gap-3">
                <AlertTriangle className="w-4 h-4 text-orange-500 flex-shrink-0 mt-0.5" />
                <div className="text-sm">
                  <p className="font-semibold text-orange-700">
                    Backend non détecté
                  </p>
                  <p className="text-orange-600 mt-0.5">
                    Lancez d'abord{" "}
                    <code className="bg-orange-100 px-1 rounded">
                      ./run-backend.sh
                    </code>{" "}
                    puis rafraîchissez.
                  </p>
                </div>
              </div>
            )}

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
              <UserInput onSubmit={handleAnalyze} isAnalyzing={false} />
            </div>
          </div>
        )}

        {/* PHASE : ANALYZING */}
        {phase === "analyzing" && (
          <div className="max-w-xl mx-auto space-y-4">
            <div className="bg-white rounded-xl border border-gray-100 px-4 py-3 shadow-sm">
              <p className="text-xs text-gray-400 mb-1">Votre description :</p>
              <p className="text-sm text-gray-700 italic">"{input}"</p>
            </div>
            <AnalyzingLoader />
          </div>
        )}

        {/* PHASE : RESULT */}
        {phase === "result" && (
          <div className="space-y-6">
            {/* Description rappel */}
            <div className="max-w-2xl mx-auto bg-white rounded-xl border border-gray-100 px-4 py-2.5 shadow-sm">
              <span className="text-xs text-gray-400">Analyse : </span>
              <span className="text-sm text-gray-700 italic">"{input}"</span>
            </div>

            {/* Erreur */}
            {error && (
              <div className="max-w-2xl mx-auto rounded-xl border border-red-200 bg-red-50 p-4 flex items-start gap-3">
                <AlertTriangle className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-semibold text-red-700">
                    Erreur lors de l'analyse
                  </p>
                  <p className="text-sm text-red-600 mt-1">{error}</p>
                  <p className="text-xs text-red-400 mt-2">
                    Vérifiez que le backend Java tourne sur{" "}
                    <code>localhost:8080</code>.
                  </p>
                </div>
              </div>
            )}

            {/* Résultats layout 2 colonnes */}
            {decision && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
                {/* Propositions agents */}
                <div className="space-y-4">
                  <h2 className="font-bold text-gray-700 flex items-center gap-2 text-sm">
                    <span className="w-6 h-6 rounded-lg bg-gray-100 flex items-center justify-center font-bold text-gray-500">
                      1
                    </span>
                    Propositions des agents JADE
                  </h2>
                  {sorted.map((p, i) => (
                    <AgentCard
                      key={p.agentType}
                      proposal={p}
                      rank={i + 1}
                      delay={i * 120}
                    />
                  ))}
                </div>

                {/* Décision rAIson */}
                <div className="space-y-4 lg:sticky lg:top-20">
                  <h2 className="font-bold text-gray-700 flex items-center gap-2 text-sm">
                    <span className="w-6 h-6 rounded-lg bg-blue-100 flex items-center justify-center font-bold text-blue-600">
                      2
                    </span>
                    Décision finale — rAIson
                  </h2>
                  <RaisonResult decision={decision} />
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* ── FOOTER ──────────────────────────────────────── */}
      <footer className="text-center py-6 text-xs text-gray-300 border-t border-gray-100 mt-12">
        Wellness SMA · JADE 4.6 · rAIson · React + Vite · Non médical
      </footer>
    </div>
  );
}
