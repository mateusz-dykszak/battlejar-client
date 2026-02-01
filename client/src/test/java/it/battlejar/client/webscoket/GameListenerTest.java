package it.battlejar.client.webscoket;

import org.junit.jupiter.api.Test;

import java.net.http.WebSocket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GameListenerTest {

    @Test
    void onTextWithNullDataShouldNotThrow() throws InterruptedException {
        // given
        BlockingDeque<String> deque = new LinkedBlockingDeque<>();
        GameListener listener = new GameListener(deque);
        WebSocket webSocket = mock(WebSocket.class);

        // when
        listener.onText(webSocket, null, true);

        // then
        String message = deque.poll(1, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).isEmpty();
    }
}
