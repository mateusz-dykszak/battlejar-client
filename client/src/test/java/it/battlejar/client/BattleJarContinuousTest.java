package it.battlejar.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BattleJarContinuousTest {


    @ParameterizedTest
    @CsvSource({
        "0, 0, false",
        "0, 3, false",
        "1, 0, false",
        "1, 3, false",
        "2, 2, true",
        "3, 3, true",
        "4, 3, true"
    })
    void isGameLimitReachedBehavesAsExpected(int gamesCompleted, int maxGames, boolean expected) {
        assertThat(BattleJarContinuous.isGameLimitReached(gamesCompleted, maxGames)).isEqualTo(expected);
    }

}
