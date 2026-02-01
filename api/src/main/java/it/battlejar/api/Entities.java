package it.battlejar.api;

import java.time.Instant;
import java.util.Collection;

/**
 * Represents a collection of entities at a specific point in time, along with the game state.
 *
 * @param timeStamp The timestamp when the entities were captured.
 * @param entities  The collection of entities.
 * @param state     The current game state.
 */
public record Entities(Instant timeStamp, Collection<Entity> entities, String state) implements WebSocketMessage {

    /**
     * Creates a new Entities instance with the current timestamp.
     *
     * @param entities The collection of entities.
     * @param state    The game state.
     */
    public Entities(Collection<Entity> entities, String state) {
        this(Instant.now(), entities, state);
    }

    /**
     * Creates a new Entities instance representing a running game.
     *
     * @param entities The collection of entities.
     * @return A new Entities instance with "RUNNING" state.
     */
    public static Entities running(Collection<Entity> entities) {
        return new Entities(entities, "RUNNING");
    }
}

