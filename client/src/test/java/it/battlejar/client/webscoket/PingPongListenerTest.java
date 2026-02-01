package it.battlejar.client.webscoket;

import org.junit.jupiter.api.Test;

import java.net.http.WebSocket;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PingPongListenerTest {

    @Test
    void constructorShouldThrowWhenDelegateIsNull() {
        assertThatThrownBy(() -> new PingPongListener(null, () -> {}))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("delegate");
    }

    @Test
    void constructorShouldThrowWhenActivityRecorderIsNull() {
        WebSocket.Listener delegate = mock(WebSocket.Listener.class);

        assertThatThrownBy(() -> new PingPongListener(delegate, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("activityRecorder");
    }
}
