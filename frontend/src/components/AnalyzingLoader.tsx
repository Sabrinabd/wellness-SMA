import { useEffect, useState } from "react";

const STEPS = [
  { icon: "🔄", text: "Envoi aux agents spécialistes...",              pct: 10 },
  { icon: "💧", text: "Agent Hydratation analyse vos symptômes...",    pct: 30 },
  { icon: "🧠", text: "Agent Fatigue évalue votre état mental...",     pct: 52 },
  { icon: "🚶", text: "Agent Activité détecte les tensions...",        pct: 72 },
  { icon: "⚖️", text: "rAIson compare les propositions des agents...", pct: 88 },
  { icon: "✅", text: "Finalisation de la recommandation...",           pct: 97 },
];

export default function AnalyzingLoader() {
  const [step, setStep] = useState(0);

  useEffect(() => {
    const t = setInterval(() => setStep(s => Math.min(s + 1, STEPS.length - 1)), 1400);
    return () => clearInterval(t);
  }, []);

  const cur = STEPS[step];

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-8 text-center shadow-sm space-y-6">
      {/* Icone */}
      <div className="relative w-20 h-20 mx-auto">
        <div className="absolute inset-0 rounded-full border-4 border-blue-200 pulse-ring" />
        <div className="absolute inset-2 rounded-full border-4 border-blue-300 pulse-ring" style={{ animationDelay: "0.4s" }} />
        <div className="absolute inset-4 rounded-full bg-blue-50 border-2 border-blue-200 flex items-center justify-center text-3xl">
          {cur.icon}
        </div>
      </div>

      {/* Texte étape */}
      <div>
        <p className="font-semibold text-gray-800 text-base">{cur.text}</p>
        <p className="text-xs text-gray-400 mt-1">Étape {step + 1} / {STEPS.length}</p>
      </div>

      {/* Barre de progression */}
      <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full bg-gradient-to-r from-blue-400 to-indigo-500 transition-all duration-1000 ease-out"
          style={{ width: `${cur.pct}%` }}
        />
      </div>

      {/* Agents actifs */}
      <div className="flex justify-center gap-3">
        {[
          { label: "💧 Hydratation", active: step >= 1, done: step >= 2 },
          { label: "🧠 Fatigue",     active: step >= 2, done: step >= 3 },
          { label: "🚶 Activité",    active: step >= 3, done: step >= 4 },
        ].map((a, i) => (
          <span
            key={i}
            className={`text-xs px-3 py-1 rounded-full font-medium border transition-all duration-500 ${
              a.done   ? "bg-green-50 text-green-700 border-green-200" :
              a.active ? "bg-blue-50 text-blue-700 border-blue-200 animate-pulse" :
                         "bg-gray-50 text-gray-400 border-gray-200"
            }`}
          >{a.label}</span>
        ))}
      </div>
    </div>
  );
}
