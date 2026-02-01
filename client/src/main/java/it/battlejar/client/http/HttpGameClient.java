package it.battlejar.client.http;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * HTTP client for communicating with the BattleJar Universe server.
 * Works with JSON strings only - no knowledge of domain models.
 */
@Slf4j
public class HttpGameClient {

    private final String baseUrl;
    private UUID gameId;

    /**
     * Creates a new HttpGameClient.
     *
     * @param baseUrl the base URL of the BattleJar Universe server
     */
    public HttpGameClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * HTTP response containing status code and body.
     *
     * @param code the HTTP status code
     * @param body the response body as a string
     */
    public record HttpResponse(int code, String body) {
    }

    /**
     * Sets the game ID for logging purposes.
     *
     * @param gameId the unique identifier of the game
     * @throws IllegalStateException if the game ID has already been set
     */
    public void setGameId(UUID gameId) {
        if (this.gameId != null) {
            throw new IllegalStateException("Game ID already set");
        }
        this.gameId = gameId;
    }

    /**
     * Sends a POST request with JSON body.
     *
     * @param path the path to send the request to
     * @param jsonBody the JSON body as a string
     * @return the HTTP response containing status code and body
     * @throws HttpError on I/O failure or on HTTP error status (4xx/5xx); response body is in HttpResponse when available
     */
    public HttpResponse post(String path, String jsonBody) {
        try {
            URL url = URI.create(baseUrl + path).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            return readResponse(path, connection);
        } catch (IOException e) {
            log.error("[{}] Failed to send POST request to {}", gameId, path, e);
            throw new HttpError("Failed to communicate with server: " + e.getMessage(), 0);
        }
    }

    /**
     * Sends a GET request.
     *
     * @param path the path to send the request to
     * @return the HTTP response containing status code and body
     * @throws HttpError on I/O failure or on HTTP error status (4xx/5xx); response body is in HttpResponse when available
     */
    public HttpResponse get(String path) {
        try {
            URL url = URI.create(baseUrl + path).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            return readResponse(path, connection);
        } catch (IOException e) {
            log.error("[{}] Failed to send GET request to {}", gameId, path, e);
            throw new HttpError("Failed to communicate with server: " + e.getMessage(), 0);
        }
    }

    private HttpResponse readResponse(String path, HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (InputStream is = connection.getInputStream()) {
                String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                log.debug("[{}] Response from {}: {}", gameId, path, response);
                return new HttpResponse(responseCode, response.isEmpty() ? null : response);
            }
        } else {
            try {
                return readErrorResponse(connection);
            } catch (RuntimeException e) {
                throw new HttpError("Failed to send request to " + path + ": " + e.getMessage(), responseCode);
            }
        }
    }

    /**
     * Reads error response from the connection.
     */
    private HttpResponse readErrorResponse(HttpURLConnection connection) {
        try (InputStream is = connection.getErrorStream()) {
            if (is != null) {
                return new HttpResponse(connection.getResponseCode(), new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            log.error("[{}] Failed to read error response", gameId, e);
            throw new RuntimeException(e.getMessage());
        }
        throw new RuntimeException("Unknown error");
    }

    /**
     * Custom exception for HTTP communication errors.
     */
    @Getter
    public static class HttpError extends RuntimeException {
        private final int code;

        /**
         * Creates a new HttpError.
         *
         * @param message the error message
         * @param code    the HTTP status code (0 if unknown or I/O error)
         */
        public HttpError(String message, int code) {
            super(message);
            this.code = code;
        }
    }
}
