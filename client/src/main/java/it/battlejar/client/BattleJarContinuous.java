package it.battlejar.client;

import it.battlejar.api.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;

/**
 * Continuous client for running multiple games in a loop.
 * When one game ends, it automatically registers for a new game.
 * Usage:
 * <pre>
 * Player initialPlayer = new Player(...);
 * try(ExecutorService executorService = ...) {
 *   BattleJarContinuous continuousClient = new BattleJarContinuous(serverUrl, initialPlayer, Commander::new, executorService);
 *   continuousClient.run();
 * };
 * </pre>
 */
@Slf4j
public class BattleJarContinuous {

    private final String serverUrl;
    private final ExecutorService executorService;
    private final Supplier<Commander> commanderFactory;
    private Player player;

    /**
     * Creates a new continuous client with a callback for when a new game starts.
     *
     * @param serverUrl        the base URL of the server
     * @param player           the initial player configuration
     * @param executorService  the executor service for running background tasks (currently unused, reserved for future use)
     * @param commanderFactory supplier that creates a new commander instance for each game
     */
    public BattleJarContinuous(String serverUrl, Player player, Supplier<Commander> commanderFactory, ExecutorService executorService) {
        this.serverUrl = serverUrl;
        this.player = requireNonNull(player, "Initial player cannot be null");
        this.executorService = executorService;
        this.commanderFactory = requireNonNull(commanderFactory, "Commander supplier cannot be null");
    }

    /**
     * Runs the continuous game loop.
     * This method blocks until the thread is interrupted.
     * When a game ends or registration fails, it retries after a short delay.
     */
    public void run() {
        log.info("Starting continuous game loop");
        log.info("Server URL: {}", serverUrl);
        log.info("Player Color: {}", player.color());

        boolean running = true;

        while (running) {
            log.info("Starting new game session...");
            try {
                Commander commander = commanderFactory.get();
                try (BattleJarClient client = new BattleJarClient(serverUrl, commander, executorService)) {
                    log.debug("Registering player with data: {}", player);
                    player = client.register(player);
                    log.debug("Registration completed with player data: {}", player);
                    client.process();
                    log.info("Game session ended. Re-registering...");
                }
            } catch (Throwable e) {
                log.error("Error in game session: {}. Retrying in 5 seconds...", e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    currentThread().interrupt();
                    log.info("Thread interrupted during retry delay - stopping");
                }
            } finally {
                if (currentThread().isInterrupted()) {
                    running = false;
                    log.info("Thread interrupted - stopping continuous game loop");
                }
            }
        }
        log.info("Continuous game loop stopped");
    }
}
