package it.battlejar.client.webscoket;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import it.battlejar.api.Entity;
import it.battlejar.api.serialization.EntitySerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Jackson module for Entity serialization and deserialization.
 */
@Slf4j
public class EntityJacksonModule extends SimpleModule {
    /**
     * Creates a new EntityJacksonModule and registers custom serializer and deserializer for {@link Entity}.
     */
    public EntityJacksonModule() {
        addSerializer(Entity.class, new EntitySerializerJackson());
        addDeserializer(Entity.class, new EntityDeserializerJackson());
    }

    private static class EntitySerializerJackson extends JsonSerializer<Entity> {
        @Override
        public void serialize(Entity value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            String serialized;
            try {
                serialized = EntitySerializer.serialize(value);
            } catch (Exception e) {
                log.error("Error serializing entity", e);
                serialized = "";
            }
            gen.writeString(serialized);
        }
    }

    private static class EntityDeserializerJackson extends JsonDeserializer<Entity> {
        @Override
        public Entity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getValueAsString();
            try {
                return EntitySerializer.deserialize(text);
            } catch (Throwable e) {
                log.error("Error deserializing entity: {}", text, e);
                return null;
            }
        }
    }
}
