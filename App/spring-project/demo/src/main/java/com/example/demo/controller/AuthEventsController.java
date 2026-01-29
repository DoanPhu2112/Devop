package com.example.demo.controller;

import com.example.demo.dto.UserAuthEvent;
import com.example.demo.dto.UserAuthEventResponse;
import com.example.demo.service.UserAuthEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/kurrentdb/auth-events")
public class AuthEventsController {
    private final UserAuthEventPublisher publisher;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserAuthEventResponse> publishUserEvent(@RequestBody UserAuthEvent event) {
        UserAuthEventResponse response = publisher.publish(event);
        return ResponseEntity.created(URI.create("/kurrentdb/auth-events/" + response.getEventId()))
                .body(response);
    }
}
