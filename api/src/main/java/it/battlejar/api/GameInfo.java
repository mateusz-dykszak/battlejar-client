package it.battlejar.api;

import java.time.Instant;

/**
 * Basic information about the game state.
 *
 * @param timeStamp The timestamp of the info.
 * @param cycle     The current game cycle.
 * @param state     The current game state.
 */
public record GameInfo(Instant timeStamp, long cycle, String state) {

    /**
     * Creates a new GameInfo with the current timestamp.
     *
     * @param cycle The current cycle.
     * @param state The game state.
     */
    public GameInfo(long cycle, String state) {
        this(Instant.now(), cycle, state);
    }
}
