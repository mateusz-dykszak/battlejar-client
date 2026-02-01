package it.battlejar.api.serialization;

import it.battlejar.api.Entity;
import it.battlejar.api.Entity.Type;

import java.util.EnumMap;
import java.util.Map;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toMap;

/**
 * Utility class for serialization and deserialization of entities.
 */
public class EntitySerializer {

    private static final Map<Type, String> TYPE_SERIALIZERS = new EnumMap<>(Map.of(Type.FIGHTER, "F", Type.CARRIER, "C", Type.MISSILE, "M"));
    private static final Map<String, Type> TYPE_DESERIALIZERS = TYPE_SERIALIZERS.entrySet().stream().collect(toMap(Map.Entry::getValue, Map.Entry::getKey));


    /**
     * Serializes the entity into a string format.
     *
     * @param entity The entity to serialize.
     * @return The serialized string representation of the entity.
     */
    public static String serialize(Entity entity) {
        return entity.id() + "|" + TYPE_SERIALIZERS.get(entity.type()) + "|" + entity.color() + "|" + entity.px() + "|" + entity.py() + "|" + entity.vx() + "|" + entity.vy() + "|" + entity.shot() + "|" + entity.sx() + "|" + entity.sy() + "|" + entity.missiles() + "|" + entity.status();
    }

    /**
     * Deserializes an entity from its string representation.
     *
     * @param serialized The serialized string representation.
     * @return The deserialized Entity object.
     * @throws IllegalArgumentException if the format is invalid (wrong part count or unknown type).
     * @throws NumberFormatException   if a numeric field cannot be parsed.
     */
    public static Entity deserialize(String serialized) {
        String[] split = serialized.split("\\|", -1);
        if (split.length != 12) {
            throw new IllegalArgumentException("Expected 12 parts, got " + split.length + ": " + serialized);
        }
        Type type = TYPE_DESERIALIZERS.get(split[1]);
        if (type == null) {
            throw new IllegalArgumentException("Unknown entity type: " + split[1]);
        }
        return new Entity(split[0], type, split[2], parseFloat(split[3]), parseFloat(split[4]), parseFloat(split[5]), parseFloat(split[6]), split[7], parseFloat(split[8]), parseFloat(split[9]), parseInt(split[10]), split[11]);
    }
}
