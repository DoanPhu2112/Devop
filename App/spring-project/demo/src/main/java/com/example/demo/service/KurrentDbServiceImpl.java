package com.example.demo.service;

import com.eventstore.dbclient.AppendToStreamOptions;
import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.WriteResult;
import com.example.demo.dto.BuyEventRequest;
import com.example.demo.dto.BuyEventResponseDTO;
import com.example.demo.dto.EventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class KurrentDbServiceImpl implements KurrentDbService {
    private final Map<String, List<EventDTO>> store = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventStoreDBClient client;

    @Override
    public BuyEventResponseDTO appendBuyEvent(BuyEventRequest request) {
        try {
            String streamName = "events-" + request.getItemId();
            String eventType = "BuyEvent";
            String eventId = UUID.randomUUID().toString();
            long createdEpoch = Instant.now().toEpochMilli();

            EventData eventData = EventData.builderAsJson(eventType, request)
                    .eventId(UUID.randomUUID())
                    .build();

            client.appendToStream("orders", eventData)
                    .get();

            return new BuyEventResponseDTO(eventId,streamName, createdEpoch);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<EventDTO> readEventsByItem(String itemId) {
        String streamName = "events-" + itemId;
        List<EventDTO> events = store.get(streamName);
        if (events == null) return new ArrayList<>();
        return new ArrayList<>(events);
    }
}
