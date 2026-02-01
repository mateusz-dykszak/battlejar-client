package it.battlejar.api;

/**
 * Represents an order sent to a game entity.
 *
 * @param id      The ID of the entity the order is for.
 * @param type    The type of the order (e.g. "MOVE", "ATTACK").
 * @param details Additional details for the order.
 */
public record Order(String id, OrderType type, String details) implements WebSocketMessage {
    /**
     * Creates an order without details.
     *
     * @param id   The ID of the entity.
     * @param type The type of the order.
     */
    @SuppressWarnings("unused")
    public Order(String id, OrderType type) {
        this(id, type, null);
    }
}

