package com.example.demo.service;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.example.demo.dto.UserAuthEvent;
import com.example.demo.dto.UserAuthEventResponse;
import com.example.demo.dto.UserAuthEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAuthEventPublisher {
    public static final String STREAM_NAME = "My_Stream_Diy";

    private final EventStoreDBClient client;

    public UserAuthEventResponse publish(UserAuthEvent event) {
        UUID eventId = UUID.randomUUID();
        UserAuthEvent normalized = normalize(event, eventId.toString());
        String eventType = buildEventType(normalized.safeEventType());

        try {
            EventData eventData = EventData.builderAsJson(eventType, normalized)
                    .eventId(eventId)
                    .build();

            client.appendToStream(STREAM_NAME, eventData).get();

            return UserAuthEventResponse.builder()
                    .eventId(eventId.toString())
                    .streamName(STREAM_NAME)
                    .createdEpochMillis(normalized.getOccurredAt())
                    .build();
        } catch (Exception e) {
            log.error("Failed to publish user auth event", e);
            throw new RuntimeException("Unable to publish user auth event", e);
        }
    }

    private UserAuthEvent normalize(UserAuthEvent event, String eventId) {
        if (event == null) {
            return UserAuthEvent.builder()
                    .eventId(eventId)
                    .eventType(UserAuthEventType.UNKNOWN)
                    .occurredAt(Instant.now().toEpochMilli())
                    .build();
        }

        UserAuthEventType type = event.safeEventType();
        Long occurredAt = event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now().toEpochMilli();

        return UserAuthEvent.builder()
            .eventId(eventId)
                .userId(event.getUserId())
                .username(event.getUsername())
                .eventType(type)
                .occurredAt(occurredAt)
                .build();
    }

    private String buildEventType(UserAuthEventType eventType) {
        return "KeycloakAuthEvent-" + eventType.name();
    }
}
