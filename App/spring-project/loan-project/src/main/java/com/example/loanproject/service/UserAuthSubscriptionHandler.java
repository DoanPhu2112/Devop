package com.example.loanproject.service;

import com.eventstore.dbclient.CreatePersistentSubscriptionToStreamOptions;
import com.eventstore.dbclient.EventStoreDBPersistentSubscriptionsClient;
import com.eventstore.dbclient.NamedConsumerStrategy;
import com.eventstore.dbclient.NackAction;
import com.eventstore.dbclient.PersistentSubscription;
import com.eventstore.dbclient.PersistentSubscriptionListener;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.SubscribePersistentSubscriptionOptions;
import com.example.loanproject.config.KurrentDbConfig;
import com.example.loanproject.dto.UserAuthEvent;
import com.example.loanproject.dto.UserAuthEventType;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAuthSubscriptionHandler {
    private final EventStoreDBPersistentSubscriptionsClient persistentSubscriptionsClient;
    private final KurrentDbConfig config;
    private final LoanTrackingService loanTrackingService;
    private final CreditApprovalService creditApprovalService;
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
            persistentSubscriptionsClient.createToStream(config.getStreamName(), config.getGroupName(), options).get();
            log.info("Created persistent subscription {} for group {}", config.getStreamName(), config.getGroupName());
        } catch (ExecutionException ex) {
            if (isAlreadyExists(ex.getCause())) {
                log.info("Persistent subscription {} / {} already exists", config.getStreamName(), config.getGroupName());
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

        persistentSubscriptionsClient.subscribeToStream(
                config.getStreamName(), 
                config.getGroupName(), 
                options, 
                new PersistentSubscriptionListener() {
                    @Override
                    public void onEvent(PersistentSubscription subscription, int retryCount, ResolvedEvent event) {
                        handleEvent(subscription, event, retryCount);
                    }

                    @Override
                    public void onCancelled(PersistentSubscription subscription, Throwable throwable) {
                        log.error("Persistent subscription {} / {} cancelled", 
                                config.getStreamName(), config.getGroupName(), throwable);
                    }

                    @Override
                    public void onConfirmation(PersistentSubscription subscription) {
                        log.info("Persistent subscription {} / {} confirmed", 
                                config.getStreamName(), config.getGroupName());
                    }
                }
        ).whenComplete((subscription, throwable) -> {
            if (throwable != null) {
                log.error("Failed to attach persistent subscription {} / {}", 
                        config.getStreamName(), config.getGroupName(), throwable);
            } else if (subscription != null) {
                log.info("Attached to persistent subscription {} / {} with id {}", 
                        config.getStreamName(), config.getGroupName(), subscription.getSubscriptionId());
            }
        });
    }

    private void handleEvent(PersistentSubscription subscription, ResolvedEvent event, int retryCount) {
        try {
            UserAuthEvent authEvent = toUserAuthEvent(event);
            
            // Process by loan tracking service
            loanTrackingService.handleUserEvent(authEvent);
            
            // Process by credit approval service
            creditApprovalService.evaluate(authEvent);
            
            // Acknowledge successful processing
            subscription.ack(event);
            
            log.debug("Processed auth event {} for user {}", authEvent.getEventId(), authEvent.getUserId());
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
