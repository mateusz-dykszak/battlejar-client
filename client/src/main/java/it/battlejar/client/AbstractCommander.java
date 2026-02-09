package it.battlejar.client;

import it.battlejar.api.*;

import java.util.Collection;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Abstract implementation of the {@link Commander} interface.
 * Provides basic functionality for storing game settings and assigned color,
 * and handles the boilerplate code for sending orders.
 */
public abstract class AbstractCommander implements Commander {

    /**
     * Consumer for sending {@link Order}s to the server.
     */
    protected Consumer<Order> orderSender;

    /**
     * The color assigned to this commander.
     */
    protected Color myColor;

    /**
     * The current {@link GameSettings}.
     */
    protected GameSettings settings;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void process(RegistrationResponse response) {
        this.myColor = response.color();
        this.settings = response.gameSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setOrdersSender(Consumer<Order> orderSender) {
        this.orderSender = orderSender;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if {@link #orderSender} has not been set
     */
    @Override
    public final boolean process(Entities entities) {
        if (orderSender == null) {
            throw new IllegalStateException("Order sender not set. Register and connect websocket first.");
        }
        if (entities == null || !"RUNNING".equals(entities.state())) {
            return true; // wait for the game to start
        }
        return process(entities.entities());
    }

    /**
     * Sends an order to the server.
     *
     * @param order the order to send
     */
    protected void order(Order order) {
        requireNonNull(order, "order cannot be null");
        orderSender.accept(order);
    }

    /**
     * Processes the collection of entities and returns whether to continue.
     * This method is called by {@link #process(Entities)} when the game is running.
     *
     * @param entities the collection of entities to process
     * @return true if the commander should continue playing, false otherwise
     */
    protected abstract boolean process(Collection<Entity> entities);

}
