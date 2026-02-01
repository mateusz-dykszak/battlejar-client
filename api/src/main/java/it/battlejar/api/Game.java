package it.battlejar.api;

import java.time.Instant;
import java.util.Collection;

/**
 * Represents the state of the game at a specific point in time.
 *
 * @param timeStamp The timestamp of the game state.
 * @param cycle     The current game cycle (tick).
 * @param entities  The collection of entities in the game.
 * @param state     The overall game state (e.g. "RUNNING", "PAUSED").
 * @param players   The collection of players in the game.
 */
public record Game(Instant timeStamp, long cycle, Collection<Entity> entities, String state, Collection<Player> players) {

    /**
     * Creates a new Game instance with the current timestamp.
     *
     * @param cycle    The current cycle.
     * @param entities The collection of entities.
     * @param state    The game state.
     * @param players  The collection of players.
     */
    public Game(long cycle, Collection<Entity> entities, String state, Collection<Player> players) {
        this(Instant.now(), cycle, entities, state, players);
    }

    /**
     * Creates a new Game instance with the current timestamp and a default cycle value.
     *
     * @param entities The collection of entities.
     * @param state    The game state.
     * @param players  The collection of players.
     */
    public Game(Collection<Entity> entities, String state, Collection<Player> players) {
        this(Instant.now(), Integer.MAX_VALUE, entities, state, players);
    }
}
