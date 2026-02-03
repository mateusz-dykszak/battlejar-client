package it.battlejar.client.webscoket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.battlejar.api.Entities;
import it.battlejar.api.Ping;
import it.battlejar.api.Pong;
import it.battlejar.api.WebSocketMessage;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.Thread.currentThread;
import static java.net.http.HttpClient.newHttpClient;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Handles WebSocket communication for a game session.
 */
@Slf4j
public class WebSocketGameClient implements AutoCloseable {

    private final ObjectMapper objectMapper;
    private final BlockingDeque<String> outQueue = new LinkedBlockingDeque<>();
    private final BlockingDeque<String> inQueue = new LinkedBlockingDeque<>();

    private final UUID gameId;
    private final ExecutorService executorService;
    private final PingPong pingPong = new PingPong();

    private WebSocket webSocket;
    private HttpClient httpClient;

    private final Semaphore lastEntitiesUpdated = new Semaphore(1);
    private Entities lastEntities;
    private volatile boolean running = true;
    private volatile boolean closing = false;

    /**
     * Creates a new WebSocketGameClient.
     *
     * @param gameId          the unique identifier of the game
     * @param objectMapper    the object mapper for JSON serialization/deserialization
     * @param executorService the executor service to use for asynchronous tasks
     */
    public WebSocketGameClient(UUID gameId, ObjectMapper objectMapper, ExecutorService executorService) {
        this.gameId = gameId;
        this.executorService = executorService;
        this.objectMapper = objectMapper;
    }

    /**
     * Queues text to be sent over the WebSocket.
     *
     * @param text the text to send
     */
    public void sendText(String text) {
        if (closing) {
            throw new IllegalStateException("WebSocket is closing - cannot send text");
        }
        outQueue.add(text);
    }

    /**
     * Queues text to be sent with high priority.
     *
     * @param text the text to send
     */
    public void sendPriorityText(String text) {
        if (closing) {
            throw new IllegalStateException("WebSocket is closing - cannot send text");
        }
        outQueue.addFirst(text);
    }

    @Override
    @Synchronized
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        running = false;
        try {
            if (webSocket != null) {
                log.info("[{}] Closing WebSocket", gameId);
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing")
                    .thenRun(webSocket::abort)
                    .orTimeout(10, TimeUnit.SECONDS);
            }
            if (httpClient != null) {
                log.info("[{}] Closing HTTP client", gameId);
                runAsync(httpClient::close, executorService).orTimeout(10, TimeUnit.SECONDS);
                if (!httpClient.awaitTermination(Duration.of(10, ChronoUnit.SECONDS))) {
                    httpClient.shutdownNow();
                }
            }
        } catch (Exception e) {
            log.error("[{}] Failed to close WebSocket or HTTP client", gameId, e);
        } finally {
            log.info("[{}] Exiting close()", gameId);
            this.webSocket = null;
            this.httpClient = null;
        }
    }

    /**
     * Starts the loop for processing and sending queued orders.
     */
    public void processOrders() {
        long lastCheck = System.currentTimeMillis();
        List<String> orders = new ArrayList<>(60);
        while (running) {
            try {
                orders.clear();
                outQueue.drainTo(orders, 60);
                if (orders.isEmpty()) {
                    if (System.currentTimeMillis() - lastCheck > 1000) {
                        lastCheck = System.currentTimeMillis();
                        pingPong.monitorConnection();
                    }
                    String text = outQueue.poll(1, TimeUnit.SECONDS);
                    if (text != null) {
                        doSendText(text);
                    }
                } else {
                    for (String order : orders) {
                        doSendText(order);
                    }
                }
            } catch (InterruptedException e) {
                log.error("[{}] Orders processing interrupted - closing connection", gameId, e);
                close();
                currentThread().interrupt();
            } catch (Throwable e) {
                log.error("[{}]Failed to process orders", gameId, e);
            }
        }
    }

    /**
     * Starts the loop for processing incoming entities.
     *
     * @param entitiesProcessor the function to process the received entities
     */
    public void processEntities(Function<Entities, Boolean> entitiesProcessor) {
        Entities toProcess = null;
        try {
            while (running) {
                if (lastEntitiesUpdated.tryAcquire(1, TimeUnit.SECONDS)) { // we use semaphore only to block waiting for an update
                    synchronized (lastEntitiesUpdated) { // we use synchronisation to enforce atomicity
                        if (lastEntities != null) {
                            toProcess = lastEntities;
                            lastEntities = null;
                        }
                    }
                    if (toProcess != null) {
                        running = entitiesProcessor.apply(toProcess);
                        toProcess = null;
                    }
                }
            }
        } catch (InterruptedException e) {
            currentThread().interrupt();
        }
    }

    /**
     * Starts the loop for processing incoming WebSocket messages.
     */
    public void processMessages() {
        List<String> messages = new ArrayList<>(30);
        while (running) {
            try {
                messages.clear();
                inQueue.drainTo(messages, 30);
                if (messages.isEmpty()) {
                    String polled = inQueue.poll(1, TimeUnit.SECONDS);
                    if (polled != null) {
                        messages.add(polled);
                    }
                }
                if (!messages.isEmpty()) {
                    Entities toProcess = null;
                    int size = messages.size();
                    for (String json : messages) {
                        try {
                            log.debug("[{}] Received JSON from WebSocket: {}", gameId, json);
                            WebSocketMessage wsMessage = objectMapper.readValue(json, WebSocketMessage.class);
                            if (wsMessage == null) {
                                log.warn("[{}] Parsed WebSocketMessage is null from JSON: {}", gameId, json);
                                continue;
                            }
                            switch (wsMessage) {
                                case Entities entities:
                                    toProcess = entities;
                                    break;
                                case Ping ping:
                                    pingPong.handlePing(ping);
                                    size--;
                                    break;
                                case Pong pong:
                                    pingPong.handlePong(pong);
                                    size--;
                                    break;
                                default:
                                    log.warn("[{}] Unhandled WebSocketMessage type: {}", gameId, wsMessage.getClass().getSimpleName());
                            }
                        } catch (Exception e) {
                            log.error("[{}] Failed to parse WebSocket message JSON: {}", gameId, json, e);
                        }
                    }
                    if (toProcess != null) {
                        log.debug("[{}] Processing entities: {}, {} skipped", gameId, toProcess, size - 1);
                        synchronized (lastEntitiesUpdated) {
                            lastEntities = toProcess;
                        }
                        lastEntitiesUpdated.release(); // we use semaphore only to block waiting for an update
                    }
                }
            } catch (InterruptedException e) {
                close();
                currentThread().interrupt();
            } catch (Throwable e) {
                log.error("[{}] Failed to process entities", gameId, e);
            }
        }
    }

    private void doSendText(String text) {
        log.debug("[{}] Send text attempt: {}", gameId, text);
        if (webSocket == null) {
            log.error("[{}] WebSocket is not connected - connect before sending orders...", gameId);
        } else if (webSocket.isOutputClosed()) {
            log.warn("[{}] WebSocket - output is closed", gameId);
            close();
        } else {
            try {
                log.debug("[{}] Sending: {}", gameId, text);
                webSocket.sendText(text, true);
            } catch (Exception e) {
                log.error("[{}] Failed to send text via WebSocket", gameId, e);
                throw new RuntimeException("Failed to send text via WebSocket", e);
            }
        }
    }

    /**
     * Connects to the game server's WebSocket.
     *
     * @param wsUrl the WebSocket URL to connect to
     */
    @Synchronized
    public void connect(String wsUrl) {
        log.info("[{}] Connecting to WebSocket: {}", gameId, wsUrl);
        httpClient = newHttpClient();
        WebSocket.Listener listener = new GameListener(inQueue);
        listener = new PingPongListener(listener, pingPong::recordActivity);
        webSocket = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), listener)
            .join();
    }

    private class PingPong {
        private final List<Ping> sentPings = new CopyOnWriteArrayList<>();
        private volatile long lastReceivedTime = System.currentTimeMillis();
        private static final int PING_WARN_THRESHOLD = 100;
        private static final int PING_ERROR_THRESHOLD = 3000;
        private static final int PING_MAX_ATTEMPTS = 5;

        private void handlePing(Ping ping) {
            recordActivity();
            Pong pong = new Pong(ping.id(), ping.timestamp());
            long duration = System.currentTimeMillis() - ping.timestamp();

            if (duration < PING_WARN_THRESHOLD) {
                log.info("[{}] Received Ping, responding with Pong: id={}, duration={}ms", gameId, ping.id(), duration);
            } else {
                log.warn("[{}] Received Ping, responding with Pong: id={}, duration={}ms (above threshold {}ms)", gameId, ping.id(), duration, PING_WARN_THRESHOLD);
            }
            send(pong);
        }

        private void handlePong(Pong pong) {
            recordActivity();
            List<Ping> pings = getPingsTillLastMatching(ping -> ping.id().equals(pong.id()));
            if (pings.isEmpty()) {
                log.debug("[{}] Received unexpected or late Pong: id={}", gameId, pong.id());
                return;
            }
            var ping = pings.getLast();
            long duration = System.currentTimeMillis() - ping.timestamp();

            if (duration < PING_WARN_THRESHOLD) {
                log.info("[{}] Received Pong from server: id={}, duration={}ms", gameId, pong.id(), duration);
            } else {
                log.warn("[{}] Received Pong from server: id={}, duration={}ms (above threshold {}ms)", gameId, pong.id(), duration, PING_WARN_THRESHOLD);
            }
        }

        private List<Ping> getPingsTillLastMatching(Predicate<Ping> predicate) {
            ArrayList<Ping> filtered = new ArrayList<>(sentPings.size());
            boolean found = false;
            for (int i = 0; i < sentPings.size(); i++) {
                if (!found || predicate.test(sentPings.get(i))) {
                    filtered.add(sentPings.get(i));
                    found = true;
                } else {
                    break;
                }
            }
            if (found) {
                sentPings.removeAll(filtered);
                return filtered;
            }
            return List.of();
        }

        void recordActivity() {
            lastReceivedTime = System.currentTimeMillis();
        }

        void monitorConnection() {
            if (running) {
                try {
                    long currentTime = System.currentTimeMillis();
                    checkPing(currentTime);
                    checkTimeout(currentTime);
                } catch (Exception e) {
                    log.error("[{}] Error in connection monitor", gameId, e);
                }
            }
        }

        private void checkPing(long currentTime) {
            long gapStart;
            if (!sentPings.isEmpty()) {
                Ping lastPing = sentPings.getLast();
                long lastPingTimeStamp = lastPing == null ? 0 : lastPing.timestamp();
                gapStart = Math.max(lastReceivedTime, lastPingTimeStamp);
            } else {
                gapStart = lastReceivedTime;
            }
            long gap = currentTime - gapStart;
            if (gap > PING_ERROR_THRESHOLD && sentPings.isEmpty()) {
                Ping ping = new Ping();
                sentPings.add(ping);
                log.warn("[{}] Inactive for {}ms. Sending Ping: id={}", gameId, gap, ping.id());
                send(ping);
            }
        }

        private void checkTimeout(long currentTime) {
            if (currentTime - lastReceivedTime > PING_ERROR_THRESHOLD * PING_MAX_ATTEMPTS) {
                log.error("[{}] Max ping attempts reached. Closing connection.", gameId);
                close();
            }
        }

        private void send(Object message) {
            try {
                if (message instanceof WebSocketMessage) {
                    sendPriorityText(objectMapper.writerFor(WebSocketMessage.class).writeValueAsString(message));
                } else {
                    sendPriorityText(objectMapper.writeValueAsString(message));
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize WebSocket message", e);
            }
        }
    }
}
