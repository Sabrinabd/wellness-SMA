package sma;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import sma.agents.AgentActivite;
import sma.agents.AgentFatigue;
import sma.agents.AgentHydratation;
import sma.agents.AgentRaison;
import sma.api.RaisonConfig;
import sma.server.WellnessHttpServer;

/**
 * Point d'entrée de l'application Wellness SMA.
 *
 * Lance :
 *   1. Le container JADE avec 4 agents
 *   2. Le serveur HTTP REST sur le port 8080
 *
 * Usage :
 *   java -cp "out:lib/*" sma.MainLauncher
 */
public class MainLauncher {

    public static void main(String[] args) throws Exception {
        RaisonConfig.printBanner();

        // ── 1. Démarrer JADE ─────────────────────────────────
        Runtime  rt      = Runtime.instance();
        Profile  profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "false");   // Mettre "true" pour la GUI JADE (debug)

        AgentContainer container = rt.createMainContainer(profile);
        System.out.println("[Launcher] Container JADE démarré");

        // ── 2. Instancier et lancer AgentRaison en premier ───
        AgentRaison     agentRaison    = new AgentRaison();
        AgentController raisonCtrl     = container.acceptNewAgent("AgentRaison", agentRaison);
        raisonCtrl.start();
        System.out.println("[Launcher] ✓ AgentRaison démarré");

        // ── 3. Lancer les agents spécialistes ────────────────
        launchAgent(container, "AgentHydratation", new AgentHydratation());
        launchAgent(container, "AgentFatigue",      new AgentFatigue());
        launchAgent(container, "AgentActivite",     new AgentActivite());

        // ── 4. Démarrer le serveur HTTP ───────────────────────
        // Petit délai pour que JADE soit bien initialisé
        Thread.sleep(1500);

        WellnessHttpServer httpServer = new WellnessHttpServer(agentRaison);
        httpServer.start();

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  ✅ Wellness SMA — Tout est prêt !              ║");
        System.out.println("║                                                  ║");
        System.out.println("║  Backend  : http://localhost:8080/api/wellness   ║");
        System.out.println("║  Frontend : http://localhost:3000                ║");
        System.out.println("║                                                  ║");
        System.out.println("║  Agents actifs : Hydratation · Fatigue · Activité║");
        System.out.println("║  Moteur        : rAIson PRJ28725                 ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Ctrl+C pour arrêter");

        // Hook d'arrêt propre
        // NOTE : on utilise java.lang.Runtime explicitement car jade.core.Runtime est importé
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Launcher] Arrêt du SMA...");
            try { container.kill(); } catch (Exception e) { /* ignore */ }
            System.out.println("[Launcher] Bye 👋");
        }));
    }

    private static void launchAgent(AgentContainer container, String name, jade.core.Agent agent) throws Exception {
        AgentController ctrl = container.acceptNewAgent(name, agent);
        ctrl.start();
        System.out.println("[Launcher] ✓ " + name + " démarré");
    }
}
