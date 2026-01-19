package com.example.demo.controller;

import com.example.demo.dto.BuyEventRequest;
import com.example.demo.dto.BuyEventResponseDTO;
import com.example.demo.dto.EventDTO;
import com.example.demo.service.KurrentDbService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@AllArgsConstructor
public class EventsController {
    private final KurrentDbService kurrentDbService;

    @PostMapping(path = "/events/buy", consumes = "application/json", produces = "application/json")
    public ResponseEntity<BuyEventResponseDTO> buyEvent(@RequestBody BuyEventRequest request) {
        BuyEventResponseDTO resp = kurrentDbService.appendBuyEvent(request);
        return ResponseEntity.created(URI.create("/events/stream/" + request.getItemId())).body(resp);
    }

    @GetMapping(path = "/events/stream/{itemId}", produces = "application/json")
    public ResponseEntity<List<EventDTO>> getEvents(@PathVariable String itemId) {
        List<EventDTO> events = kurrentDbService.readEventsByItem(itemId);
        return ResponseEntity.ok(events);
    }
}

