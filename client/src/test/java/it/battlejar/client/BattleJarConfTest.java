package it.battlejar.client;

import it.battlejar.api.Color;
import it.battlejar.api.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BattleJarConfTest {

    @Test
    void mergeWithFileFillsOnlyEmptyFields(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("battlejar.conf");
        UUID fileId = UUID.randomUUID();
        Files.writeString(
            conf,
            """
                player.id=%s
                player.username=configUser
                player.color=BLUE
                """.formatted(fileId)
        );

        Player partial = new Player(null, null, "cliName");
        Player merged = BattleJarConf.mergeWithFile(partial, conf);

        assertThat(merged.id()).isEqualTo(fileId);
        assertThat(merged.username()).isEqualTo("cliName");
        assertThat(merged.color()).isEqualTo(Color.BLUE);
    }

    @Test
    void mergeWithFileUsesCliWhenSet(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("battlejar.conf");
        UUID cliId = UUID.randomUUID();
        Files.writeString(
            conf,
            """
                player.id=00000000-0000-0000-0000-000000000001
                player.username=fileOnly
                player.color=GREEN
                """
        );

        Player cli = new Player(cliId, Color.RED, "me");
        Player merged = BattleJarConf.mergeWithFile(cli, conf);

        assertThat(merged.id()).isEqualTo(cliId);
        assertThat(merged.username()).isEqualTo("me");
        assertThat(merged.color()).isEqualTo(Color.RED);
    }

    @Test
    void mergeLoadsDisplayNameFromPlayerNameWhenUsernameAbsent(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("battlejar.conf");
        Files.writeString(
            conf,
            """
                player.name=NickOnly
                player.color=ORANGE
                """
        );

        Player merged = BattleJarConf.mergeWithFile(new Player(null, null, null), conf);

        assertThat(merged.username()).isEqualTo("NickOnly");
        assertThat(merged.color()).isEqualTo(Color.ORANGE);
    }

    @Test
    void mergePrefersUsernameWhenNameAndUsernameDiffer(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("battlejar.conf");
        Files.writeString(
            conf,
            """
                player.name=DisplayName
                player.username=loginName
                """
        );

        Player merged = BattleJarConf.mergeWithFile(new Player(null, null, null), conf);

        assertThat(merged.username()).isEqualTo("loginName");
    }

    @Test
    void mergeAcceptsMatchingNameAndUsername(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("battlejar.conf");
        Files.writeString(
            conf,
            """
                player.name=same
                player.username=same
                """
        );

        Player merged = BattleJarConf.mergeWithFile(new Player(null, null, null), conf);

        assertThat(merged.username()).isEqualTo("same");
    }

    @Test
    void mergeWithNullBaseLoadsFromFile(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("battlejar.conf");
        UUID fileId = UUID.randomUUID();
        Files.writeString(
            conf,
            """
                player.id=%s
                player.username=ghost
                player.color=VIOLET
                """.formatted(fileId)
        );

        Player merged = BattleJarConf.mergeWithFile(null, conf);

        assertThat(merged.id()).isEqualTo(fileId);
        assertThat(merged.username()).isEqualTo("ghost");
        assertThat(merged.color()).isEqualTo(Color.VIOLET);
    }

    @Test
    void savePlayerIdCreatesFileWithId(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("nested").resolve("battlejar.conf");
        UUID id = UUID.randomUUID();

        BattleJarConf.savePlayerId(conf, id);

        assertThat(Files.isRegularFile(conf)).isTrue();
        Player merged = BattleJarConf.mergeWithFile(new Player(null, null, null), conf);
        assertThat(merged.id()).isEqualTo(id);
    }
}
