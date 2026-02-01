package it.battlejar.api;

/**
 * Represents a pong message sent in response to a {@link Ping}.
 *
 * @param id        The unique identifier from the corresponding Ping.
 * @param timestamp The timestamp from the corresponding Ping.
 */
public record Pong(String id, long timestamp) implements WebSocketMessage {
}
