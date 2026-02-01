package it.battlejar.api;

/**
 * Order types that can be sent to entities.
 * These correspond to Command.Type values in the core module.
 */
public enum OrderType {
    /**
     * Move the entity to a specific location.
     */
    MOVE,
    /**
     * Turn the entity to a specific orientation.
     */
    TURN_XY,
    /**
     * Dock the entity with another entity (e.g. Fighter to Carrier).
     */
    DOCK,
    /**
     * Set the entity to patrol an area.
     */
    PATROL,
    /**
     * Set a target for the entity.
     */
    TARGET,
    /**
     * Order the entity to attack a target.
     */
    ATTACK,
    /**
     * Order the entity to fire a missile.
     */
    FIRE_MISSILE
}

