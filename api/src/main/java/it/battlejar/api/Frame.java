package it.battlejar.api;

import java.time.Instant;
import java.util.Collection;

/**
 * Represents a RUNNING snapshot of the game at a specific point in time, including entities and orders.
 *
 * @param timeStamp The timestamp when the frame was captured.
 * @param entities  The collection of entities.
 * @param orders    The collection of orders issued around this frame.
 */
public record Frame(Instant timeStamp, Collection<Entity> entities, Collection<Order> orders) {
}

