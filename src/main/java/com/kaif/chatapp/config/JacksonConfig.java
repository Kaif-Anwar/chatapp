package com.kaif.chatapp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * API timestamps are stored as UTC in MySQL but modeled as LocalDateTime.
 * Serialize/deserialize them as ISO-8601 UTC so browsers parse times correctly.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcLocalDateTimeCustomizer() {
        return builder -> {
            builder.serializerByType(LocalDateTime.class, new UtcLocalDateTimeSerializer());
            builder.deserializerByType(LocalDateTime.class, new UtcLocalDateTimeDeserializer());
        };
    }

    static final class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(value.atZone(ZoneOffset.UTC).toInstant().toString());
        }
    }

    static final class UtcLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            String text = parser.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            if (text.endsWith("Z") || text.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return Instant.parse(text).atZone(ZoneOffset.UTC).toLocalDateTime();
            }
            return LocalDateTime.parse(text);
        }
    }
}
