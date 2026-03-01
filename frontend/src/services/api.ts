import type { WellnessDecision } from "../types";

const BASE = "/api/wellness"; // Proxy Vite → localhost:8080

export const api = {
  async analyze(description: string): Promise<WellnessDecision> {
    const res = await fetch(`${BASE}/analyze`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ description }),
    });
    if (!res.ok) {
      const txt = await res.text();
      throw new Error(`Serveur ${res.status} : ${txt}`);
    }
    const data: WellnessDecision = await res.json();
    if (!data.success) throw new Error(data.errorMessage || "Erreur SMA");
    return data;
  },

  async health(): Promise<boolean> {
    try {
      const res = await fetch(`${BASE}/health`);
      return res.ok;
    } catch { return false; }
  },

  async examples(): Promise<string[]> {
    try {
      const res = await fetch(`${BASE}/examples`);
      if (res.ok) return await res.json();
    } catch { /* fallback */ }
    return [
      "J'ai un peu mal à la tête, je suis fatigué, j'ai travaillé 3h sans pause.",
      "Je me sens tendu et je suis assis depuis ce matin.",
      "Je n'arrive plus à me concentrer, je suis devant l'écran depuis longtemps.",
      "J'ai les yeux fatigués et les épaules raides après 4h de réunion.",
    ];
  },
};
