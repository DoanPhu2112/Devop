package com.example.demo.controller;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.dto.AddToCartEventResponseDTO;
import com.example.demo.dto.EventDTO;
import com.example.demo.service.KurrentDbService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/kurrentdb")
public class KurrentdbEventsController {
    private final KurrentDbService kurrentDbService;

    @PostMapping(path = "/events/add-to-cart", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AddToCartEventResponseDTO> addItemToCart(@RequestBody AddToCartRequest request) {
        AddToCartEventResponseDTO resp = kurrentDbService.appendAddToCartEvent(request);
        return ResponseEntity.created(URI.create("/events/stream/" + request.getName())).body(resp);
    }

    @PostMapping(path = "/events/remove-from-cart/{eventId}", produces = "application/json")
    public ResponseEntity<String> removeItemFromCart(@PathVariable String eventId) {
        kurrentDbService.removeItemFromCart(eventId);
        return ResponseEntity.created(URI.create("/events/remove-from-cart/" + eventId)).body("Remove from cart event appended successfully.");
    }

    @GetMapping(path = "/events/stream/{itemId}", produces = "application/json")
    public ResponseEntity<List<EventDTO>> getEvent(@PathVariable String itemId) {
        List<EventDTO> events = kurrentDbService.readEventsByItem(itemId);
        return ResponseEntity.ok(events);
    }

    @PostMapping(path = "/events/buy", consumes = "application/json", produces = "application/json") 
    public ResponseEntity<String> buyItem(@RequestBody AddToCartRequest request) {
        kurrentDbService.buyItem(request);

        return ResponseEntity.created(URI.create("/events/buy")).body("Buy events appended successfully.");
    }

    @PostMapping(path = "/events/buy-all", consumes = "application/json", produces = "application/json") 
    public ResponseEntity<String> buyAllItems(@RequestBody List<AddToCartRequest> requests) {
        kurrentDbService.buyItems(requests);

        return ResponseEntity.created(URI.create("/events/buy-all")).body("All buy events appended successfully.");
    }
}

