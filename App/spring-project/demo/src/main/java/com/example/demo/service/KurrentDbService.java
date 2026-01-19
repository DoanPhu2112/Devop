package com.example.demo.service;

import com.example.demo.dto.BuyEventRequest;
import com.example.demo.dto.BuyEventResponseDTO;
import com.example.demo.dto.EventDTO;

import java.util.List;

public interface KurrentDbService {
    BuyEventResponseDTO appendBuyEvent(BuyEventRequest request);

    List<EventDTO> readEventsByItem(String itemId);
}
