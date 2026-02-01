package it.battlejar.client;

import it.battlejar.api.Order;
import it.battlejar.api.OrderType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BattleJarClientTest {

    private final Commander commander = mock(Commander.class);

    @Test
    void registerShouldThrowWhenPlayerIsNull() {
        // given
        try(BattleJarClient client = new BattleJarClient("http://localhost:8080", commander)) {

            // when / then
            assertThatThrownBy(() -> client.register(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
        }
    }

    @Test
    void orderShouldThrowWhenOrderIsNull() {
        // given
        try(BattleJarClient client = new BattleJarClient("http://localhost:8080", commander)) {

            // when / then
            assertThatThrownBy(() -> client.order(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
        }
    }

    @Test
    void orderShouldThrowWhenOrderIdIsBlank() {
        // given
        try(BattleJarClient client = new BattleJarClient("http://localhost:8080", commander)) {

            // when / then
            assertThatThrownBy(() -> client.order(new Order(" ", OrderType.MOVE, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order ID");
        }
    }

    @Test
    void orderShouldThrowWhenOrderTypeIsNull() {
        // given
        try(BattleJarClient client = new BattleJarClient("http://localhost:8080", commander)) {

            // when / then
            assertThatThrownBy(() -> client.order(new Order("entity-1", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order type");
        }
    }

    @Test
    void processShouldThrowWhenNotRegistered() {
        // given
        try(BattleJarClient client = new BattleJarClient("http://localhost:8080", commander)) {

            // when / then
            assertThatThrownBy(client::process)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Must register");
        }
    }
}
