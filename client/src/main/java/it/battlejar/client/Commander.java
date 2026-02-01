package it.battlejar.client;

import it.battlejar.api.Entities;
import it.battlejar.api.Order;
import it.battlejar.api.RegistrationResponse;

import java.util.function.Consumer;

/**
 * Interface for commanders that can participate in BattleJar Universe games.
 * Implementations of this interface define the strategy and logic for playing the game.
 */
public interface Commander {

    /**
     * Called when the player is registered with the server.
     *
     * @param registrationResponse the registration response containing game settings and assigned colour
     */
    void process(RegistrationResponse registrationResponse);

    /**
     * Processes entities received from the server.
     *
     * @param latestEntities the latest entities from the game
     * @return true if the commander should continue playing, false if the game should end
     */
    boolean process(Entities latestEntities);

    /**
     * Sets the consumer for sending orders to the server.
     * When the commander is ready to send orders, it should call the provided consumer with the order.
     *
     * @param orderSender consumer for sending orders to the server
     */
    void setOrdersSender(Consumer<Order> orderSender);
}
