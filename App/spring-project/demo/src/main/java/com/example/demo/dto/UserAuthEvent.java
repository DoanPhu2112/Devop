package com.example.demo.dto;

import org.keycloak.events.EventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthEvent {
    private String eventId;
    private String userId;
    private String username;
    private EventType eventType;
    private Long occurredAt;
}
