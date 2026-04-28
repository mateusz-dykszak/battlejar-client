package it.battlejar.api;

import java.util.List;

/**
 * Represents a whole game recording, containing participating players and a list of frames.
 *
 * @param players The list of players.
 * @param frames  The list of frames.
 */
public record GameRecord(List<Player> players, List<Frame> frames) {
}

