package it.battlejar.api;

import java.util.UUID;

/**
 * Response returned when a player registers.
 * Contains the registration message and game settings.
 */
public record RegistrationResponse(UUID gameId, UUID playerId, Color color, GameSettings gameSettings) {
}

