package it.battlejar.client;

import it.battlejar.api.Color;
import it.battlejar.api.Player;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Loads and updates local {@code battlejar.conf} (Java {@link Properties} format).
 */
@Slf4j
final class BattleJarConf {

    static final String KEY_PLAYER_ID = "player.id";
    static final String KEY_PLAYER_NAME = "player.name";
    static final String KEY_PLAYER_USERNAME = "player.username";
    static final String KEY_PLAYER_COLOR = "player.color";

    private BattleJarConf() {
    }

    static Path defaultPath() {
        return Path.of("battlejar.conf");
    }

    /**
     * Fills null id, blank username, or null color from the config file when those fields are empty on {@code base}.
     * Display name may come from {@code player.username} or {@code player.name}; if both are set in the file to
     * different values, {@code player.username} is used and a warning is logged.
     */
    static Player mergeWithFile(Player base, Path confPath) {
        Player b = base == null ? new Player(null, null, null) : base;
        if (!Files.isRegularFile(confPath)) {
            return b;
        }
        try {
            Properties props = loadProperties(confPath);
            UUID id = b.id() != null ? b.id() : parseUuid(props.getProperty(KEY_PLAYER_ID)).orElse(null);
            String username = nonBlank(b.username()) ? b.username() : resolveNameOrUsernameFromConfig(props);
            Color color = b.color() != null ? b.color() : parseColor(props.getProperty(KEY_PLAYER_COLOR)).orElse(null);
            return new Player(id, color, username);
        } catch (IOException e) {
            log.warn("Could not read {}: {}", confPath.toAbsolutePath(), e.getMessage());
            return b;
        }
    }

    static void savePlayerId(Path confPath, UUID id) {
        try {
            Properties props = new Properties();
            if (Files.isRegularFile(confPath)) {
                try (Reader r = Files.newBufferedReader(confPath, StandardCharsets.UTF_8)) {
                    props.load(r);
                }
            }
            props.setProperty(KEY_PLAYER_ID, id.toString());
            Path parent = confPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer w = Files.newBufferedWriter(confPath, StandardCharsets.UTF_8)) {
                props.store(w, "BattleJar client local configuration");
            }
            log.info("Saved player id to {}", confPath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not write player id to {}: {}", confPath.toAbsolutePath(), e.getMessage());
        }
    }

    private static Properties loadProperties(Path confPath) throws IOException {
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(confPath, StandardCharsets.UTF_8)) {
            props.load(r);
        }
        return props;
    }

    private static Optional<UUID> parseUuid(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring invalid player.id in config: {}", raw);
            return Optional.empty();
        }
    }

    private static Optional<Color> parseColor(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Color.valueOf(s.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring invalid player.color in config: {}", raw);
            return Optional.empty();
        }
    }

    private static String normalizeUsername(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Resolves the display name from {@code player.name} and/or {@code player.username} in the config file.
     * When both are present and differ, {@code player.username} wins.
     */
    private static String resolveNameOrUsernameFromConfig(Properties props) {
        String fromName = normalizeUsername(props.getProperty(KEY_PLAYER_NAME));
        String fromUsername = normalizeUsername(props.getProperty(KEY_PLAYER_USERNAME));
        if (fromName != null && fromUsername != null && !fromName.equals(fromUsername)) {
            log.warn(
                "battlejar.conf: player.name ({}) and player.username ({}) differ; using player.username",
                fromName,
                fromUsername
            );
            return fromUsername;
        }
        if (fromUsername != null) {
            return fromUsername;
        }
        return fromName;
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.isBlank();
    }
}
