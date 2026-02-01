package it.battlejar.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColorTest {

    @Test
    void countShouldExcludeNone() {
        // given / when
        int count = Color.count();

        // then
        assertThat(count).isEqualTo(Color.values().length - 1);
        assertThat(Color.values()).contains(Color.NONE);
    }

}
