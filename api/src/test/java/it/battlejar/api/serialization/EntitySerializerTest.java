package it.battlejar.api.serialization;

import it.battlejar.api.Entity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntitySerializerTest {

    @Test
    void shouldRoundTripSerializeAndDeserializeEntity() {
        // given
        Entity entity = new Entity(
            "entity-1", Entity.Type.FIGHTER, "RED",
            10.5f, 20.5f, 1.0f, 0.0f, "false", 0f, 0f, 2, "ALIVE"
        );

        // when
        String serialized = EntitySerializer.serialize(entity);
        Entity deserialized = EntitySerializer.deserialize(serialized);

        // then
        assertThat(deserialized).isEqualTo(entity);
    }

    @Test
    void shouldSerializeAndDeserializeCarrierAndMissileTypes() {
        // given - use non-null shot so round-trip preserves equality
        Entity carrier = new Entity("c-1", Entity.Type.CARRIER, "BLUE", 0f, 0f, 0f, 0f, "false", 0f, 0f, 0, "ALIVE");
        Entity missile = new Entity("m-1", Entity.Type.MISSILE, "GREEN", 1f, 1f, 1f, 1f, "false", 0f, 0f, 0, "FLYING");

        // when
        Entity carrierBack = EntitySerializer.deserialize(EntitySerializer.serialize(carrier));
        Entity missileBack = EntitySerializer.deserialize(EntitySerializer.serialize(missile));

        // then
        assertThat(carrierBack).isEqualTo(carrier);
        assertThat(missileBack).isEqualTo(missile);
    }

    @Test
    void shouldThrowWhenPartsCountIsWrong() {
        // given - 11 parts (missing one field)
        String serialized = "id|F|RED|0|0|0|0||0|0|ALIVE";

        // when / then
        assertThatThrownBy(() -> EntitySerializer.deserialize(serialized))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Expected 12 parts");
    }

    @Test
    void shouldThrowWhenEntityTypeIsUnknown() {
        // given
        String serialized = "id|X|RED|0|0|0|0||0|0|0|ALIVE";

        // when / then
        assertThatThrownBy(() -> EntitySerializer.deserialize(serialized))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown entity type");
    }

    @Test
    void shouldThrowWhenNumericFieldIsInvalid() {
        // given
        String serialized = "id|F|RED|not-a-number|0|0|0||0|0|0|ALIVE";

        // when / then
        assertThatThrownBy(() -> EntitySerializer.deserialize(serialized))
            .isInstanceOf(NumberFormatException.class);
    }
}
