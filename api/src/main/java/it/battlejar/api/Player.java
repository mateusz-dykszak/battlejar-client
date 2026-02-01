package it.battlejar.api;

import java.util.UUID;

/**
 * Represents a player in the BattleJar Universe.
 *
 * @param id       The unique identifier of the player.
 * @param color    The colour assigned to the player.
 * @param username The username of the player.
 */
public record Player(UUID id, Color color, String username) {
}

