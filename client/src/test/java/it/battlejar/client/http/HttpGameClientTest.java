package it.battlejar.client.http;

import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpGameClientTest {

    @Test
    void setGameIdShouldThrowWhenAlreadySet() {
        // given
        HttpGameClient client = new HttpGameClient("http://localhost:8080");
        client.setGameId(randomUUID());

        // when / then
        assertThatThrownBy(() -> client.setGameId(randomUUID()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already set");
    }

    @Test
    void httpErrorShouldExposeCode() {
        HttpGameClient.HttpError error = new HttpGameClient.HttpError("test message", 404);
        assertThat(error.getCode()).isEqualTo(404);
        assertThat(error.getMessage()).isEqualTo("test message");
    }

    @Test
    void httpResponseShouldHoldCodeAndBody() {
        HttpGameClient.HttpResponse response = new HttpGameClient.HttpResponse(200, "{\"ok\":true}");
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"ok\":true}");
    }
}
