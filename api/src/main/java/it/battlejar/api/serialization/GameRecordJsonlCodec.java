package it.battlejar.api.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.battlejar.api.Entity;
import it.battlejar.api.Frame;
import it.battlejar.api.GameRecord;
import it.battlejar.api.Order;
import it.battlejar.api.OrderType;
import it.battlejar.api.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Serializes / deserializes a {@link GameRecord} in JSONL format.
 *
 * <p>Each line is either:
 * <ul>
 *     <li>a JSON array of players (first line)</li>
 *     <li>a JSON object representing a frame</li>
 * </ul>
 *
 * <p>Frames store entities as strings produced by {@link EntitySerializer}.
 * <p>Deserializer ignores unknown line objects.
 */
public final class GameRecordJsonlCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final TypeReference<List<Player>> PLAYERS_LIST = new TypeReference<>() {
    };

    private GameRecordJsonlCodec() {
    }

    public static String serialize(GameRecord record) {
        requireNonNull(record, "record cannot be null");
        StringBuilder sb = new StringBuilder();
        sb.append(serializePlayers(record.players())).append('\n');
        List<Frame> frames = record.frames() == null ? List.of() : record.frames();
        for (Frame frame : frames) {
            sb.append(serializeFrame(frame)).append('\n');
        }
        return sb.toString();
    }

    public static String serializePlayers(List<Player> players) {
        try {
            return MAPPER.writeValueAsString(players == null ? emptyList() : players);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize players list", e);
        }
    }

    public static String serializeFrame(Frame frame) {
        requireNonNull(frame, "frame cannot be null");
        try {
            List<String> entities = new ArrayList<>();
            if (frame.entities() != null) {
                for (Entity e : frame.entities()) {
                    entities.add(EntitySerializer.serialize(e));
                }
            }
            List<OrderLine> orders = new ArrayList<>();
            if (frame.orders() != null) {
                for (Order o : frame.orders()) {
                    orders.add(new OrderLine(o.id(), o.type(), o.details()));
                }
            }
            FrameLine line = new FrameLine(
                frame.timeStamp() == null ? null : frame.timeStamp().toString(),
                entities,
                orders
            );
            return MAPPER.writeValueAsString(line);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize frame", e);
        }
    }

    public static GameRecord deserialize(String jsonl) {
        requireNonNull(jsonl, "jsonl cannot be null");

        List<Player> players = null;
        List<Frame> frames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(jsonl))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    JsonNode node = MAPPER.readTree(trimmed);
                    if (node == null) {
                        continue;
                    }
                    if (node.isArray()) {
                        // players list (or unknown array); only accept first one
                        if (players == null) {
                            players = MAPPER.readValue(trimmed, PLAYERS_LIST);
                        }
                        continue;
                    }
                    if (node.isObject() && node.has("entities")) {
                        Frame frame = tryParseFrame(node);
                        if (frame != null) {
                            frames.add(frame);
                        }
                    }
                } catch (Exception ignored) {
                    // unknown or malformed line - ignore
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read JSONL", e);
        }

        return new GameRecord(players == null ? emptyList() : players, frames);
    }

    private static Frame tryParseFrame(JsonNode node) {
        try {
            String timeStampString = node.hasNonNull("timeStamp") ? node.get("timeStamp").asText(null) : null;
            Instant timeStamp = timeStampString == null || timeStampString.isBlank() ? null : Instant.parse(timeStampString);

            List<Entity> entities = new ArrayList<>();
            JsonNode entitiesNode = node.get("entities");
            if (entitiesNode != null && entitiesNode.isArray()) {
                for (JsonNode e : entitiesNode) {
                    if (e != null && e.isTextual()) {
                        try {
                            entities.add(EntitySerializer.deserialize(e.asText()));
                        } catch (Exception ignored) {
                            // ignore bad entity rows
                        }
                    }
                }
            }

            List<Order> orders = new ArrayList<>();
            JsonNode ordersNode = node.get("orders");
            if (ordersNode != null && ordersNode.isArray()) {
                try {
                    List<OrderLine> parsed = MAPPER.convertValue(ordersNode, new TypeReference<List<OrderLine>>() {
                    });
                    for (OrderLine o : parsed) {
                        if (o != null && o.id() != null && o.type() != null) {
                            orders.add(new Order(o.id(), o.type(), o.details()));
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // ignore bad orders list
                }
            }

            return new Frame(timeStamp, entities, orders);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record FrameLine(String timeStamp, Collection<String> entities, Collection<OrderLine> orders) {
    }

    private record OrderLine(String id, OrderType type, String details) {
    }
}

