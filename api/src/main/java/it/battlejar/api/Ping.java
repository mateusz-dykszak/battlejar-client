package it.battlejar.api;

import java.util.UUID;

/**
 * Represents a ping message used for heartbeat and latency measurement.
 *
 * @param id        A unique identifier for the ping.
 * @param timestamp The timestamp when the ping was created.
 */
public record Ping(String id, long timestamp) implements WebSocketMessage {
    /**
     * Creates a new Ping with a random ID and specified timestamp.
     *
     * @param timestamp The timestamp.
     */
    public Ping(long timestamp) {
        this(UUID.randomUUID().toString(), timestamp);
    }

    /**
     * Creates a new Ping with a random ID and the current system time.
     */
    public Ping() {
        this(System.currentTimeMillis());
    }
}
