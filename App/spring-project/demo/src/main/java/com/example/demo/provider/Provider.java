package com.example.demo.provider;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;

import com.example.demo.dto.UserAuthEvent;
import com.example.demo.service.UserAuthEventPublisher;

public class Provider implements EventListenerProvider {

    private UserAuthEventPublisher userAuthEventPublisher;

    @Override
    public void onEvent(Event event) {
        UserAuthEvent userAuthEvent = UserAuthEvent.builder()
            .eventId(event.getId())
            .userId(event.getUserId())
            .username(event.getDetails() != null ? event.getDetails().get("username") : null)
            .eventType(event.getType())
            .occurredAt(event.getTime())
            .build();
        userAuthEventPublisher.publish(userAuthEvent);
    }

    @Override
    public void onEvent(AdminEvent event, boolean b) {

    }

    @Override
    public void close() {

    }

    // private String toString(Event event) {

    //     StringBuilder sb = new StringBuilder();

    //     sb.append("type=");

    //     sb.append(event.getType());

    //     sb.append(", realmId=");

    //     sb.append(event.getRealmId());

    //     sb.append(", clientId=");

    //     sb.append(event.getClientId());

    //     sb.append(", userId=");

    //     sb.append(event.getUserId());

    //     sb.append(", ipAddress=");

    //     sb.append(event.getIpAddress());


    //     if (event.getError() != null) {

    //         sb.append(", error=");

    //         sb.append(event.getError());

    //     }


    //     if (event.getDetails() != null) {

    //         for (Map.Entry<String, String> e : event.getDetails().entrySet()) {

    //             sb.append(", ");

    //             sb.append(e.getKey());

    //             if (e.getValue() == null || e.getValue().indexOf(' ') == -1) {

    //                 sb.append("=");

    //                 sb.append(e.getValue());

    //             } else {

    //                 sb.append("='");

    //                 sb.append(e.getValue());

    //                 sb.append("'");

    //             }

    //         }

    //     }


    //     return sb.toString();

    // }
    // private String toString(AdminEvent adminEvent) {

    //     StringBuilder sb = new StringBuilder();


    //     sb.append("operationType=");

    //     sb.append(adminEvent.getOperationType());

    //     sb.append(", realmId=");

    //     sb.append(adminEvent.getAuthDetails().getRealmId());

    //     sb.append(", clientId=");

    //     sb.append(adminEvent.getAuthDetails().getClientId());

    //     sb.append(", userId=");

    //     sb.append(adminEvent.getAuthDetails().getUserId());

    //     sb.append(", ipAddress=");

    //     sb.append(adminEvent.getAuthDetails().getIpAddress());

    //     sb.append(", resourcePath=");

    //     sb.append(adminEvent.getResourcePath());


    //     if (adminEvent.getError() != null) {

    //         sb.append(", error=");

    //         sb.append(adminEvent.getError());

    //     }


    //     return sb.toString();

    // }

}
