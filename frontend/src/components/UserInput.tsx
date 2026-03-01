import { useState, useEffect } from "react";
import { Sparkles, AlertCircle } from "lucide-react";
import { api } from "../services/api";

interface Props {
  onSubmit: (desc: string) => void;
  isAnalyzing: boolean;
}

export default function UserInput({ onSubmit, isAnalyzing }: Props) {
  const [text, setExamples] = useState<string[]>([]);
  const [value, setValue]   = useState("");

  useEffect(() => { api.examples().then(setExamples); }, []);

  const valid = value.trim().length >= 10;

  const submit = () => { if (valid && !isAnalyzing) onSubmit(value.trim()); };

  return (
    <div className="space-y-5">
      {/* Titre */}
      <div>
        <h2 className="text-2xl font-bold text-gray-800 mb-1">Comment vous sentez-vous ?</h2>
        <p className="text-sm text-gray-500">
          Décrivez votre état. Nos 3 agents JADE vont analyser vos symptômes,
          puis <span className="font-semibold text-blue-600">rAIson</span> choisira la meilleure action.
        </p>
      </div>

      {/* Textarea */}
      <textarea
        rows={5}
        value={value}
        onChange={e => setValue(e.target.value.slice(0, 500))}
        onKeyDown={e => { if ((e.ctrlKey || e.metaKey) && e.key === "Enter") submit(); }}
        disabled={isAnalyzing}
        placeholder="Ex : J'ai mal à la tête, je suis fatigué et j'ai travaillé 3h sans pause..."
        className="
          w-full px-4 py-3 rounded-xl border-2 resize-none text-gray-800 text-sm
          placeholder-gray-300 border-gray-200
          focus:border-blue-400 focus:outline-none
          disabled:bg-gray-50 disabled:opacity-60
          transition-colors duration-200
        "
      />

      {/* Validation */}
      {value.length > 0 && !valid && (
        <div className="flex items-center gap-2 text-orange-500 text-xs">
          <AlertCircle className="w-3.5 h-3.5" />
          Minimum 10 caractères pour une bonne analyse.
        </div>
      )}

      {/* Exemples */}
      {text.length > 0 && (
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
            Essayez un exemple :
          </p>
          <div className="flex flex-col gap-2">
            {text.map((ex, i) => (
              <button
                key={i}
                onClick={() => setValue(ex)}
                disabled={isAnalyzing}
                className="
                  text-left text-sm text-gray-600 px-4 py-2.5 rounded-full
                  bg-white border border-gray-200 shadow-sm
                  hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700
                  transition-all duration-150 disabled:opacity-50
                "
              >
                {ex}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Bouton */}
      <button
        onClick={submit}
        disabled={!valid || isAnalyzing}
        className="
          w-full py-3.5 rounded-xl font-semibold text-white text-base
          flex items-center justify-center gap-2
          bg-gradient-to-r from-blue-500 to-blue-600
          hover:from-blue-600 hover:to-indigo-600
          disabled:opacity-50 disabled:cursor-not-allowed
          shadow-md hover:shadow-lg transition-all duration-200
        "
      >
        {isAnalyzing ? (
          <>
            <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            Analyse en cours...
          </>
        ) : (
          <>
            <Sparkles className="w-5 h-5" />
            Analyser mon état
          </>
        )}
      </button>

      <p className="text-center text-xs text-gray-300">Ctrl+Enter pour envoyer</p>
    </div>
  );
}
