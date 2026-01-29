package com.example.loanproject.service;

import com.example.loanproject.dto.LoanStatus;
import com.example.loanproject.dto.UserAuthEvent;
import com.example.loanproject.dto.UserAuthEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class LoanTrackingService {
    private final ConcurrentMap<String, LoanStatus> loanStatuses = new ConcurrentHashMap<>();

    public void handleUserEvent(UserAuthEvent event) {
        if (event == null || event.getUserId() == null) {
            log.warn("Skip loan tracking because event or user id is null");
            return;
        }

        UserAuthEventType type = event.safeEventType();
        long occurredAt = event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now().toEpochMilli();

        loanStatuses.compute(event.getUserId(), (userId, existing) -> LoanStatus.builder()
                .userId(userId)
                .username(event.getUsername())
                .lastEventType(type)
                .lastEventAt(occurredAt)
                .status(resolveStatus(type, existing))
                .notes(buildNotes(type, event))
                .build());

        log.info("Loan tracker updated for user {} with event {}", event.getUserId(), type);
    }

    public Collection<LoanStatus> getAllStatuses() {
        return loanStatuses.values();
    }

    public Optional<LoanStatus> getStatus(String userId) {
        return Optional.ofNullable(loanStatuses.get(userId));
    }

    private String resolveStatus(UserAuthEventType type, LoanStatus existing) {
        switch (type) {
            case SIGNUP:
                return "PROFILE_CREATED";
            case LOGIN:
                return "ACTIVE_SESSION";
            case LOGOUT:
                return "SESSION_ENDED";
            case PASSWORD_UPDATE:
                return "SECURITY_REVIEW";
            default:
                return existing != null ? existing.getStatus() : "UNKNOWN";
        }
    }

    private String buildNotes(UserAuthEventType type, UserAuthEvent event) {
        String base = String.format("Event %s at %d", type.name(),
                event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now().toEpochMilli());
        if (type == UserAuthEventType.SIGNUP) {
            return base + ", onboarding from ";
        }
        return base;
    }
}
