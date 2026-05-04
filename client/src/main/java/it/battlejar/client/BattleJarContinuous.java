package it.battlejar.client;

import it.battlejar.api.Player;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
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
 * try(ExecutorService executor = ...) {
 *   var continuous = new BattleJarContinuous(serverUrl, initialPlayer, MyCommander::new, executor);       // no limit
 *   // or: new BattleJarContinuous(..., executor, 10) to stop after 10 completed games
 *   continuous.run();
 * };
 * </pre>
 * <p>
 * If {@code player.id()}, {@code player.username()}, or {@code player.color()} are empty, {@link #run()} fills them
 * from {@code battlejar.conf} in the current working directory when that file exists. A display name may also be read
 * from {@code player.name}; if {@code player.name} and {@code player.username} both appear in the file with different
 * values, {@code player.username} is used and a warning is logged. If registration is sent without
 * a player id, the id returned by the server is written back to {@code battlejar.conf}.
 * </p>
 */
@Slf4j
public class BattleJarContinuous {

    private final String serverUrl;
    private final ExecutorService executorService;
    private final Supplier<Commander> commanderFactory;
    private final Player initialPlayer;
    private final Path battleJarConfPath;

    /**
     * Maximum number of games to play; 0 or less means no limit.
     */
    private final int maxGames;

    /**
     * Creates a new continuous client with a callback for when a new game starts.
     * There is no limit on the number of games.
     *
     * @param serverUrl        the base URL of the server
     * @param player           the initial player configuration (may be null: resolved from {@code battlejar.conf} in {@link #run()})
     * @param executorService  the executor service for running background tasks (currently unused, reserved for future use)
     * @param commanderFactory supplier that creates a new commander instance for each game
     */
    public BattleJarContinuous(String serverUrl, Player player, Supplier<Commander> commanderFactory, ExecutorService executorService) {
        this(serverUrl, player, commanderFactory, executorService, BattleJarConf.defaultPath(), 0);
    }

    /**
     * Creates a new continuous client with a limit on how many games to play.
     *
     * @param serverUrl        the base URL of the server
     * @param player           the initial player configuration (may be null: resolved from {@code battlejar.conf} in {@link #run()})
     * @param executorService  the executor service for running background tasks (currently unused, reserved for future use)
     * @param commanderFactory supplier that creates a new commander instance for each game
     * @param maxGames         stop after these many completed games; 0 or less for no limit
     */
    public BattleJarContinuous(
        String serverUrl,
        Player player,
        Supplier<Commander> commanderFactory,
        ExecutorService executorService,
        int maxGames
    ) {
        this(serverUrl, player, commanderFactory, executorService, BattleJarConf.defaultPath(), maxGames);
    }

    /**
     * Same as {@link #BattleJarContinuous(String, Player, Supplier, ExecutorService)} with no initial player;
     * identity is taken from {@code battlejar.conf} when present.
     */
    public BattleJarContinuous(String serverUrl, Supplier<Commander> commanderFactory, ExecutorService executorService) {
        this(serverUrl, null, commanderFactory, executorService, BattleJarConf.defaultPath(), 0);
    }

    /**
     * Same as {@link #BattleJarContinuous(String, Player, Supplier, ExecutorService, int)} with no initial player.
     */
    public BattleJarContinuous(
        String serverUrl,
        Supplier<Commander> commanderFactory,
        ExecutorService executorService,
        int maxGames
    ) {
        this(serverUrl, null, commanderFactory, executorService, BattleJarConf.defaultPath(), maxGames);
    }

    BattleJarContinuous(
        String serverUrl,
        Player player,
        Supplier<Commander> commanderFactory,
        ExecutorService executorService,
        Path battleJarConfPath,
        int maxGames
    ) {
        this.serverUrl = requireNonNull(serverUrl, "Server URL cannot be null");
        this.initialPlayer = player;
        this.executorService = executorService;
        this.commanderFactory = requireNonNull(commanderFactory, "Commander supplier cannot be null");
        this.battleJarConfPath = requireNonNull(battleJarConfPath, "Config path cannot be null");
        this.maxGames = maxGames;
    }

    static boolean isGameLimitReached(int gamesCompleted, int maxGames) {
        return maxGames > 0 && gamesCompleted >= maxGames;
    }

    /**
     * Runs the continuous game loop.
     * This method blocks until the thread is interrupted.
     * When a game ends or registration fails, it retries after a short delay.
     */
    public void run() {
        log.info("Starting continuous game loop");
        log.info("Server URL: {}", this.serverUrl);
        Player player = BattleJarConf.mergeWithFile(initialPlayer, battleJarConfPath);
        log.info("Player id: {}", player.id() != null ? player.id() : "(none)");
        log.info("Player name: {}", player.username() != null && !player.username().isBlank() ? player.username() : "(none)");
        log.info("Player color: {}", player.color() != null ? player.color() : "(none)");
        if (maxGames > 0) {
            log.info("Game limit: {} (will stop after that many completed games)", maxGames);
        } else {
            log.info("No game limit (runs until interrupted)");
        }

        boolean running = true;
        int gamesCompleted = 0;

        while (running) {
            log.info("Starting new game session...");
            try {
                Commander commander = commanderFactory.get();
                try (BattleJarClient client = new BattleJarClient(serverUrl, commander, executorService)) {
                    boolean registrationSentWithoutId = player.id() == null;
                    log.debug("Registering player with data: {}", player);
                    var registeredPlayer = client.register(player);
                    log.debug("Registration completed with player data: {}", registeredPlayer);
                    if (registrationSentWithoutId && registeredPlayer.id() != null) {
                        BattleJarConf.savePlayerId(battleJarConfPath, registeredPlayer.id());
                    }
                    client.process();
                    player = new Player(registeredPlayer.id(), registeredPlayer.color(), player.username());
                    gamesCompleted++;
                    if (isGameLimitReached(gamesCompleted, maxGames)) {
                        log.info("Completed {} game(s) (limit: {}). Stopping — client closed, commander finished for this session.", gamesCompleted, maxGames);
                        running = false;
                    } else {
                        log.info("Game session ended. Re-registering... ({} completed so far)", gamesCompleted);
                    }
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
