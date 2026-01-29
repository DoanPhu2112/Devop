package com.example.demo.service;

import com.eventstore.dbclient.CreatePersistentSubscriptionToStreamOptions;
import com.eventstore.dbclient.EventStoreDBPersistentSubscriptionsClient;
import com.eventstore.dbclient.NamedConsumerStrategy;
import com.eventstore.dbclient.NackAction;
import com.eventstore.dbclient.PersistentSubscription;
import com.eventstore.dbclient.PersistentSubscriptionListener;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.SubscribePersistentSubscriptionOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.dto.UserAuthEvent;
import com.example.demo.dto.UserAuthEventType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAuthPersistentSubscription {
    private static final String STREAM_NAME = "My_Stream_Diy";
    private static final String GROUP_NAME = "My_Group_Diy";

    private final EventStoreDBPersistentSubscriptionsClient persistentSubscriptionsClient;
    private final LoanTrackingService loanTrackingService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void start() {
        ensurePersistentSubscription();
        subscribe();
    }

    private void ensurePersistentSubscription() {
        CreatePersistentSubscriptionToStreamOptions options = CreatePersistentSubscriptionToStreamOptions.get()
                .fromStart()
                .resolveLinkTos()
                .namedConsumerStrategy(NamedConsumerStrategy.ROUND_ROBIN);

        try {
            persistentSubscriptionsClient.createToStream(STREAM_NAME, GROUP_NAME, options).get();
            log.info("Created persistent subscription {} for group {}", STREAM_NAME, GROUP_NAME);
        } catch (ExecutionException ex) {
            if (isAlreadyExists(ex.getCause())) {
                log.info("Persistent subscription {} / {} already exists", STREAM_NAME, GROUP_NAME);
                return;
            }
            throw new RuntimeException("Failed to create persistent subscription", ex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating persistent subscription", e);
        }
    }

    private void subscribe() {
        SubscribePersistentSubscriptionOptions options = SubscribePersistentSubscriptionOptions.get()
                .bufferSize(32);

        persistentSubscriptionsClient.subscribeToStream(STREAM_NAME, GROUP_NAME, options, new PersistentSubscriptionListener() {
            @Override
            public void onEvent(PersistentSubscription subscription, int retryCount, ResolvedEvent event) {
                handleEvent(subscription, event, retryCount);
            }

            @Override
            public void onCancelled(PersistentSubscription subscription, Throwable throwable) {
                log.error("Persistent subscription {} / {} cancelled", STREAM_NAME, GROUP_NAME, throwable);
            }

            @Override
            public void onConfirmation(PersistentSubscription subscription) {
                log.info("Persistent subscription {} / {} confirmed", STREAM_NAME, GROUP_NAME);
            }
        }).whenComplete((subscription, throwable) -> {
            if (throwable != null) {
                log.error("Failed to attach persistent subscription {} / {}", STREAM_NAME, GROUP_NAME, throwable);
            } else if (subscription != null) {
                log.info("Attached to persistent subscription {} / {} with id {}", STREAM_NAME, GROUP_NAME, subscription.getSubscriptionId());
            }
        });
    }

    private void handleEvent(PersistentSubscription subscription, ResolvedEvent event, int retryCount) {
        try {
            UserAuthEvent authEvent = toUserAuthEvent(event);
            loanTrackingService.handleUserEvent(authEvent);
            subscription.ack(event);
        } catch (Exception e) {
            log.error("Failed to process auth event, retry {}", retryCount, e);
            subscription.nack(NackAction.Park, e.getMessage(), event);
        }
    }

    private UserAuthEvent toUserAuthEvent(ResolvedEvent event) throws IOException {
        UserAuthEvent parsed = objectMapper.readValue(event.getEvent().getEventData(), UserAuthEvent.class);

        UserAuthEventType type = parsed.safeEventType();
        if (type == UserAuthEventType.UNKNOWN) {
            type = inferTypeFromName(event.getEvent().getEventType());
        }

        long occurredAt = parsed.getOccurredAt() != null
                ? parsed.getOccurredAt()
                : event.getEvent().getCreated().toEpochMilli();

        if (parsed.getEventId() == null) {
            parsed.setEventId(event.getEvent().getEventId().toString());
        }

        parsed.setEventType(type);
        parsed.setOccurredAt(occurredAt);

        return parsed;
    }

    private UserAuthEventType inferTypeFromName(String eventTypeName) {
        if (eventTypeName == null) {
            return UserAuthEventType.UNKNOWN;
        }
        String normalized = eventTypeName.toUpperCase(Locale.ROOT);
        if (normalized.contains("LOGIN")) return UserAuthEventType.LOGIN;
        if (normalized.contains("LOGOUT")) return UserAuthEventType.LOGOUT;
        if (normalized.contains("SIGNUP") || normalized.contains("REGISTER")) return UserAuthEventType.SIGNUP;
        if (normalized.contains("PASSWORD")) return UserAuthEventType.PASSWORD_UPDATE;
        return UserAuthEventType.UNKNOWN;
    }

    private boolean isAlreadyExists(Throwable cause) {
        if (cause == null) {
            return false;
        }
        String message = cause.getMessage();
        return message != null && message.toUpperCase(Locale.ROOT).contains("ALREADY_EXISTS");
    }
}
