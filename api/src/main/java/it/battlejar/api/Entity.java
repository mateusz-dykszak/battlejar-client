package it.battlejar.api;

/**
 * Represents a game entity (e.g. Fighter, Carrier, Missile) in the BattleJar Universe.
 *
 * @param id       The unique identifier of the entity.
 * @param type     The type of the entity (FIGHTER, CARRIER, MISSILE).
 * @param color    The colour of the entity.
 * @param px       The x-coordinate of the entity's position.
 * @param py       The y-coordinate of the entity's position.
 * @param vx       The x-component of the entity's velocity.
 * @param vy       The y-component of the entity's velocity.
 * @param shot     The result of the shot ("true"/"false"), or null if the entity was not shooting.
 * @param sx       The x-coordinate of the shot target.
 * @param sy       The y-coordinate of the shot target.
 * @param missiles The number of missiles the entity has.
 * @param status   The current status of the entity.
 */
public record Entity(String id, Type type, String color, float px, float py, float vx, float vy, String shot, float sx,
                     float sy, int missiles, String status) {

    /**
     * Enumeration of entity types.
     */
    public enum Type {
        FIGHTER,
        CARRIER,
        MISSILE
    }
}

