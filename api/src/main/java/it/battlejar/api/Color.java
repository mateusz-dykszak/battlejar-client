package it.battlejar.api;

import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * Selects a random color from the available color options, excluding {@code NONE}.
     *
     * @return A randomly chosen {@code Color} from the enum, excluding {@code NONE}.
     */
    public static Color random() {
        Color[] colors = Color.values();
        return colors[ThreadLocalRandom.current().nextInt(count())];
    }
}

