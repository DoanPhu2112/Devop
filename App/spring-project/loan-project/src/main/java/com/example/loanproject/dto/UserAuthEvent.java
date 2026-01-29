package com.example.loanproject.dto;

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
    private UserAuthEventType eventType;
    private Long occurredAt;

    public UserAuthEventType safeEventType() {
        return eventType != null ? eventType : UserAuthEventType.UNKNOWN;
    }
}
