package it.battlejar.api.serialization;

import it.battlejar.api.Entity;
import it.battlejar.api.Frame;
import it.battlejar.api.GameRecord;
import it.battlejar.api.Order;
import it.battlejar.api.OrderType;
import it.battlejar.api.Player;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameRecordJsonlCodecTest {

    @Test
    void shouldSerializeAndDeserializeJsonlAndIgnoreUnknownLines() {
        // given
        List<Player> players = List.of(
            new Player(UUID.randomUUID(), null, "alice"),
            new Player(UUID.randomUUID(), null, "bob")
        );
        Frame frame = new Frame(
            Instant.parse("2026-04-26T19:20:00Z"),
            List.of(new Entity("e1", Entity.Type.FIGHTER, "RED", 1f, 2f, 3f, 4f, "false", 0f, 0f, 0, "ALIVE")),
            List.of(new Order("e1", OrderType.MOVE, "to=1,2"))
        );
        GameRecord record = new GameRecord(players, List.of(frame));

        // when
        String jsonl = GameRecordJsonlCodec.serialize(record);
        String jsonlWithNoise = jsonl + "{\"unknown\":true}\n" + "[{\"not\":\"players\"}]\n";
        GameRecord back = GameRecordJsonlCodec.deserialize(jsonlWithNoise);

        // then
        assertThat(back.players()).isEqualTo(players);
        assertThat(back.frames()).hasSize(1);
        assertThat(back.frames().getFirst().timeStamp()).isEqualTo(frame.timeStamp());
        assertThat(back.frames().getFirst().entities()).isEqualTo(frame.entities());
        assertThat(back.frames().getFirst().orders()).isEqualTo(frame.orders());
    }
}

