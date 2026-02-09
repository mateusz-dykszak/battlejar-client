package it.battlejar.client;

import it.battlejar.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AbstractCommanderTest {

    private TestCommander commander;
    private Consumer<Order> orderSender;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        commander = new TestCommander();
        orderSender = mock(Consumer.class);
        commander.setOrdersSender(orderSender);
    }

    @Test
    void processShouldStoreColorAndSettings() {
        // given
        GameSettings settings = new GameSettings(800, 600, 100, 10, 500, 30, 50, 5);
        RegistrationResponse response = new RegistrationResponse(UUID.randomUUID(), UUID.randomUUID(), Color.RED, settings);

        // when
        commander.process(response);

        // then
        assertThat(commander.myColor).isEqualTo(Color.RED);
        assertThat(commander.settings).isEqualTo(settings);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STARTING", "ENDING", "CLEANING", "INITIALIZING", "REGISTRATION", "PRE_REGISTRATION"})
    void processShouldNotDelegateWhenStateIsNotRunning(String state) {
        // given
        Entities entities = new Entities(List.of(), state);

        // when
        boolean result = commander.process(entities);

        // then
        assertThat(result).isTrue();
        assertThat(commander.processCallCount).isZero();
        verifyNoInteractions(orderSender);
    }

    @Test
    void processShouldNotDelegateWhenEntitiesIsNull() {
        // when
        boolean result = commander.process((Entities) null);

        // then
        assertThat(result).isTrue();
        assertThat(commander.processCallCount).isZero();
        verifyNoInteractions(orderSender);
    }

    @Test
    void processShouldDelegateWhenRunning() {
        // given
        Entity entity = new Entity("e1", Entity.Type.FIGHTER, "RED", 0, 0, 0, 0, null, 0, 0, 0, "OK");
        List<Entity> entityList = List.of(entity);
        Entities entities = Entities.running(entityList);

        // when
        boolean result = commander.process(entities);

        // then
        assertThat(result).isTrue();
        assertThat(commander.processCallCount).isEqualTo(1);
        assertThat(commander.lastProcessedEntities).isSameAs(entityList);
    }

    @Test
    void processShouldReturnFalseWhenSubclassReturnsFalse() {
        // given
        TestCommander quitCommander = new TestCommander() {
            @Override
            protected boolean process(Collection<Entity> entities) {
                super.process(entities);
                return false;
            }
        };
        quitCommander.setOrdersSender(orderSender);
        Entities entities = Entities.running(List.of());

        // when
        boolean result = quitCommander.process(entities);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void processShouldThrowWhenOrderSenderNotSet() {
        // given
        TestCommander noSender = new TestCommander();
        Entities entities = Entities.running(List.of());

        // when / then
        assertThatThrownBy(() -> noSender.process(entities))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Order sender not set");
    }

    @Test
    void orderShouldSendOrder() {
        // given
        Order order = new Order("RED-01", OrderType.ATTACK);

        // when
        commander.order(order);

        // then
        verify(orderSender).accept(order);
    }

    @Test
    void orderShouldThrowWhenOrderIsNull() {
        assertThatThrownBy(() -> commander.order(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("order cannot be null");
    }

    @Test
    void processShouldThrowAfterSetOrdersSenderNull() {
        // given
        commander.setOrdersSender(null);
        Entities entities = Entities.running(List.of());

        // when / then
        assertThatThrownBy(() -> commander.process(entities))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Order sender not set");
    }

    private static class TestCommander extends AbstractCommander {
        int processCallCount = 0;
        Collection<Entity> lastProcessedEntities;

        @Override
        protected boolean process(Collection<Entity> entities) {
            processCallCount++;
            lastProcessedEntities = entities;
            return true;
        }
    }
}
