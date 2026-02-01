package it.battlejar.api;

/**
 * Game configuration settings.
 * This record holds config values that are sent to clients during registration.
 *
 * @param worldWidth      The width of the game world.
 * @param worldHeight     The height of the game world.
 * @param fighterBody     The body type ID for Fighter entities.
 * @param fighterSize     The size (radius) of Fighter entities.
 * @param carrierBody     The body type ID for Carrier entities.
 * @param carrierSize     The size (radius) of Carrier entities.
 * @param missileBaseBody The base body type ID for Missile entities.
 * @param missileSize     The size (radius) of Missile entities.
 */
public record GameSettings(
    float worldWidth,
    float worldHeight,
    int fighterBody,
    float fighterSize,
    int carrierBody,
    float carrierSize,
    int missileBaseBody,
    float missileSize
) {
}

