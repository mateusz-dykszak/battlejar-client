package it.battlejar.client.webscoket;

import lombok.extern.slf4j.Slf4j;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

/**
 * A WebSocket listener decorator that records activity whenever any message is received.
 */
@Slf4j
public class PingPongListener implements WebSocket.Listener {

    private final WebSocket.Listener delegate;
    private final Runnable activityRecorder;

    /**
     * Creates a new PingPongListener.
     *
     * @param delegate         the actual listener to delegate events to
     * @param activityRecorder a runnable to be executed whenever activity is detected
     */
    public PingPongListener(WebSocket.Listener delegate, Runnable activityRecorder) {
        this.delegate = requireNonNull(delegate, "delegate");
        this.activityRecorder = requireNonNull(activityRecorder, "activityRecorder");
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        activityRecorder.run();
        return delegate.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
        activityRecorder.run();
        return delegate.onBinary(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, java.nio.ByteBuffer message) {
        activityRecorder.run();
        return delegate.onPing(webSocket, message);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, java.nio.ByteBuffer message) {
        activityRecorder.run();
        return delegate.onPong(webSocket, message);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        return delegate.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        delegate.onError(webSocket, error);
    }
}
