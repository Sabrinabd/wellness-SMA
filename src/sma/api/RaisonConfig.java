package sma.api;

/**
 * Configuration de l'API rAIson.
 */
public final class RaisonConfig {

    // ── Paramètres API ───────────────────────────────────────
    public static final String BASE_URL = "https://api.ai-raison.com";

    
    public static final String API_KEY = System.getenv("RAISON_API_KEY") != null
            ? System.getenv("RAISON_API_KEY")
            : "RkBvGKGueu7qbXQ6Ye53c2XkDOC7RZeE46UWReMl";

    public static final String APP_ID  = "PRJ30175";
    public static final String VERSION = "latest";

    // ── Timeouts ────────────────────────────────────────────
    public static final int HTTP_TIMEOUT_MS  = 15_000;   // 15s pour l'appel rAIson
    public static final int AGENT_TIMEOUT_MS = 30_000;   // 30s attente agents JADE

    // ── Serveur HTTP interne ─────────────────────────────────
    public static final int SERVER_PORT = 8080;

    private RaisonConfig() {}

    public static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║           Wellness SMA — Backend démarré         ║");
        System.out.println("║                                                  ║");
        System.out.println("║  APP_ID  : " + APP_ID + "                          ║");
        System.out.println("║  API KEY : " + API_KEY.substring(0, 8) + "...              ║");
        System.out.println("║  Port    : " + SERVER_PORT + "                              ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
    }
}
