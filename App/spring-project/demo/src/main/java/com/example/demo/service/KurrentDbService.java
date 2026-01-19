package com.example.demo.service;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.dto.AddToCartEventResponseDTO;
import com.example.demo.dto.EventDTO;
import java.util.List;

public interface KurrentDbService {
    AddToCartEventResponseDTO appendAddToCartEvent(AddToCartRequest request);

    List<EventDTO> readEventsByItem(String itemId);

    void removeItemFromCart(String eventId);

    void buyItem(AddToCartRequest request);

    void buyItems(List<AddToCartRequest> requests);
}
