package it.battlejar.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for all messages exchanged over the WebSocket connection.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Order.class, name = "order"),
    @JsonSubTypes.Type(value = Entities.class, name = "entities"),
    @JsonSubTypes.Type(value = Ping.class, name = "ping"),
    @JsonSubTypes.Type(value = Pong.class, name = "pong"),
})
public sealed interface WebSocketMessage permits Entities, Order, Ping, Pong {
}
