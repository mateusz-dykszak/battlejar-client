package it.battlejar.client.webscoket;

import lombok.extern.slf4j.Slf4j;

import java.net.http.WebSocket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletionStage;

/**
 * Simplified WebSocket listener that only works with JSON strings.
 * Puts received messages into a blocking deque for processing by a separate job.
 */
@Slf4j
public class GameListener implements WebSocket.Listener {

    private final BlockingDeque<String> entitiesDeque;
    private final StringBuilder messageAccumulator = new StringBuilder();

    /**
     * Creates a new GameListener.
     *
     * @param entitiesDeque the deque where received messages will be added
     */
    public GameListener(BlockingDeque<String> entitiesDeque) {
        this.entitiesDeque = entitiesDeque;
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        if (data != null) {
            messageAccumulator.append(data);
        }
        if (last) {
            String fullMessage = messageAccumulator.toString();
            log.debug("Received message: {}", fullMessage);
            messageAccumulator.setLength(0);
            entitiesDeque.add(fullMessage);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("WebSocket error", error);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.info("WebSocket closed: {} {}", statusCode, reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
}
