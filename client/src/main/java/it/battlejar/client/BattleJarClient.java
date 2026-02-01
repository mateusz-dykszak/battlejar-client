package it.battlejar.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.battlejar.api.*;
import it.battlejar.client.http.HttpGameClient;
import it.battlejar.client.webscoket.EntityJacksonModule;
import it.battlejar.client.webscoket.WebSocketGameClient;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

/**
 * Client for communicating with the BattleJar Universe core server.
 * Designed for single game usage.
 * Usage:
 * <pre>
 * Commander commander = new MyCommander();
 * try (BattleJarClient client = new BattleJarClient(serverUrl, commander)) {
 *   RegistrationResponse response = client.register(player);
 *   client.process();
 * }
 * </pre>
 */
@Slf4j
public class BattleJarClient implements AutoCloseable {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new EntityJacksonModule());
    private final ExecutorService executorService;
    private final boolean externalExecutor;

    private final String baseUrl;
    private final HttpGameClient httpGameClient;

    private final Commander commander;

    private WebSocketGameClient webSocketGameClient;
    private Player player;
    private volatile UUID gameId;
    private volatile boolean closing = false;

    /**
     * Creates a new BattleJar client with a virtual thread executor.
     * The executor will be automatically managed and closed by the client.
     *
     * @param serverUrl the base URL of the BattleJar Universe server
     * @param commander the commander implementation that will process game state and send orders
     */
    @SuppressWarnings("unused")
    public BattleJarClient(String serverUrl, Commander commander) {
        this(serverUrl, commander, newVirtualThreadPerTaskExecutor(), false);
    }

    /**
     * Creates a new BattleJar client with a custom executor service.
     * The provided executor will not be shut down when the client is closed,
     * allowing it to be reused or managed externally.
     *
     * @param serverUrl       the base URL of the BattleJar Universe server
     * @param commander       the commander implementation that will process game state and send orders
     * @param executorService the executor service to use for asynchronous operations
     */
    public BattleJarClient(String serverUrl, Commander commander, ExecutorService executorService) {
        this(serverUrl, commander, executorService, true);
    }

    private BattleJarClient(String serverUrl, Commander commander, ExecutorService executorService, boolean externalExecutor) {
        this.baseUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.commander = commander;
        this.httpGameClient = new HttpGameClient(baseUrl);
        this.executorService = executorService;
        this.externalExecutor = externalExecutor;
    }

    /**
     * Registers a player with the server.
     *
     * @param player the player to register
     */
    public Player register(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        boolean registered = false;

        log.debug("[{}] Registering player {} with color: {}", gameId, player.id(), player.color() == null ? "AUTO" : player.color());
        do {
            try {
                RegistrationResponse response;
                String jsonBody = objectMapper.writeValueAsString(this.player != null ? this.player : player);
                log.debug("[{}] Registration request payload: {}", gameId, jsonBody);
                HttpGameClient.HttpResponse httpResponse = httpGameClient.post("/register", jsonBody);
                int code = httpResponse.code();
                String jsonBodyResponse = httpResponse.body();
                log.debug("[{}] Registration response payload: code={}, body={}", gameId, code, jsonBodyResponse);

                if (code == 200 || code == 202) {
                    response = objectMapper.readValue(jsonBodyResponse, RegistrationResponse.class);
                    log.debug("[{}] Registration response parsed: id={}, color={}", gameId, response.playerId(), response.color());

                    UUID playerId = response.playerId() != null ? response.playerId() : this.player.id();
                    this.player = new Player(playerId, response.color(), player.username());

                    if (code == 202) {
                        log.debug("[{}] Updated player: {}", gameId, this.player);
                        Thread.sleep(1000);
                    } else {
                        gameId = response.gameId();
                        httpGameClient.setGameId(gameId);
                        if (response.color() == null) {
                            throw new IllegalStateException("Server did not return a color in registration response");
                        }
                        log.info("[{}] Registration successful - assigned color: {}", gameId, response.color());

                        // Notify commander of registration and provide order sender
                        commander.process(response);
                        commander.setOrdersSender(this::order);
                        registered = true;
                    }
                } else {
                    log.error("[{}] Registration failed with code {}: {}", gameId, code, jsonBodyResponse);
                    throw new RuntimeException("Registration failed: " + (jsonBodyResponse != null ? jsonBodyResponse : "Unknown error"));
                }
            } catch (InterruptedException e) {
                currentThread().interrupt();
                throw new RuntimeException("Registration interrupted", e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to register player", e);
            }
        } while (!registered);
        return this.player;
    }

    /**
     * Starts processing entities and connects to WebSocket.
     * This method blocks until the WebSocket connection is closed.
     */
    public void process() {
        if (player == null) {
            throw new IllegalStateException("Must register before processing entities");
        }

        if (commander == null) {
            throw new IllegalStateException("Commander is required");
        }

        String wsUrl = baseUrl.replace("http", "ws") + "/ws?playerId=" + player.id();

        try (WebSocketGameClient webSocketGameClient = new WebSocketGameClient(gameId, objectMapper, executorService)) {
            webSocketGameClient.connect(wsUrl);
            this.webSocketGameClient = webSocketGameClient;
            log.info("[{}] Connected to WebSocket: {}", gameId, wsUrl);
            executorService.submit(webSocketGameClient::processMessages);
            log.info("[{}] Started processing WebSocket messages", gameId);
            executorService.submit(() -> webSocketGameClient.processEntities(this::entitiesProcessor));
            log.info("[{}] Started processing entities. Starting sending orders", gameId);
            webSocketGameClient.processOrders();
            log.info("[{}] Stopped sending orders. Stopping processing entities", gameId);
        }
    }

    private boolean entitiesProcessor(Entities entities) {
        if (entities.state() != null) {
            String state = entities.state();
            if ("ENDING".equals(state) || "CLEANING".equals(state) || "INITIALIZING".equals(state)) {
                log.info("[{}] Game state is {}, leaving gracefully", gameId, state);
                close();
                return false;
            }
        }
        log.debug("[{}] Processing {} entities for commander", gameId, entities.entities().size());
        return commander.process(entities);
    }

    /**
     * Sends an order to the game server.
     *
     * @param order the order to send
     */
    public void order(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.id() == null || order.id().isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (order.type() == null) {
            throw new IllegalArgumentException("Order type is required");
        }

        if (player == null) {
            throw new IllegalStateException("Must register before sending orders");
        }

        if (webSocketGameClient == null) {
            throw new IllegalStateException("WebSocket not connected - call process() first");
        }

        try {
            String json = objectMapper.writeValueAsString(order);
            webSocketGameClient.sendText(json);
        } catch (Exception e) {
            log.error("[{}] Failed to send order via WebSocket", gameId, e);
            throw new RuntimeException("Failed to send order via WebSocket", e);
        }
    }

    /**
     * Signals that the player is leaving the game.
     *
     * @param color the colour of the player leaving
     */
    public void leave(Color color) {
        String path = "/leave?color=" + color.name() + "&gameId=" + gameId;
        log.info("[{}] {} is leaving game", gameId, color.name());
        try {
            HttpGameClient.HttpResponse httpResponse = httpGameClient.get(path);
            log.debug("[{}] Leave response: code={}, body={}", gameId, httpResponse.code(), httpResponse.body());
        } catch (Exception e) {
            log.warn("[{}] Failed to call leave endpoint", gameId, e);
        }
    }

    @Override
    @Synchronized
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        try {
            if (player != null) {
                leave(player.color());
            }
            if (!externalExecutor) {
                executorService.shutdown();
            }
        } finally {
            log.info("[{}] Leaving - closing connection", gameId);
            if (webSocketGameClient != null) {
                webSocketGameClient.close();
            }
        }
    }
}
