package it.battlejar.api;

/**
 * Represents the possible colours for players and entities in the game.
 */
public enum Color {
    RED, BLUE, GREEN, VIOLET, ORANGE, WHITE, NONE;

    /**
     * Returns the number of valid colors (excluding NONE).
     *
     * @return The count of valid colors.
     */
    public static int count() {
        return values().length - 1;
    }
}

