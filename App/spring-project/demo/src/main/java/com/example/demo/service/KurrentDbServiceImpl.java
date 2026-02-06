package com.example.demo.service;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.WriteResult;
import com.example.demo.dto.AddToCartRequest;
import com.example.demo.dto.AddToCartEventResponseDTO;
import com.example.demo.dto.EventDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class KurrentDbServiceImpl implements KurrentDbService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final EventStoreDBClient client;

    private static final String STREAM_NAME = "orders-stream-DIY";

    @Override
    public AddToCartEventResponseDTO appendAddToCartEvent(AddToCartRequest request) {
        try {
            String eventType = "AddToCartEventDIY"; // aka Event name
            String eventId = UUID.randomUUID().toString() + "-" + request.getId();
            long createdEpoch = Instant.now().toEpochMilli();

            EventData eventData = EventData.builderAsJson(eventType, request)
                    .eventId(UUID.fromString(eventId))
                    .build();

            WriteResult result = client.appendToStream(STREAM_NAME, eventData)
                    .get();

            System.out.println(result.toString());

            return new AddToCartEventResponseDTO(eventId, STREAM_NAME, createdEpoch);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removeItemFromCart(String eventId) {
        String eventType = "RemoveFromCartEventDIY"; // aka Event name
        
        client.appendToStream(STREAM_NAME, 
            EventData.builderAsJson(eventType, "")
            .eventId(UUID.fromString(UUID.randomUUID().toString() + "-" + eventId))
            .build()
        );
    }

    @Override
    public List<EventDTO> readEventsByItem(String itemId) {
        try {
            ReadResult eventStream = client.readStream(STREAM_NAME, ReadStreamOptions.get().fromStart()).get();

            List<EventDTO> events = new ArrayList<>();
            for (ResolvedEvent re : eventStream.getEvents()) {
                String eventDataJson = new String(re.getEvent().getEventData(), StandardCharsets.UTF_8);
                String eventType = re.getEvent().getEventType();
                String eventId = re.getEvent().getEventId().toString();
                long createdEpochMillis = re.getEvent().getCreated().toEpochMilli();

                // Deserialize event data
                AddToCartRequest data = objectMapper.readValue(eventDataJson, AddToCartRequest.class);

                // Filter by itemId
                if (data.getId().equals(itemId)) {
                    EventDTO eventDTO = new EventDTO(eventId, eventType, eventDataJson, createdEpochMillis);
                    events.add(eventDTO);
                }
            }
            return events;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void buyItem(AddToCartRequest request) {
        String eventType = "BuyItemEventDIY"; // aka Event name
        String eventId = UUID.randomUUID().toString() + "-" + request.getId();
        EventData eventData = EventData.builderAsJson(eventType, request)
                .eventId(UUID.fromString(eventId))
                .build();

        try {
            client.appendToStream(STREAM_NAME, eventData).get();    
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void buyItems(List<AddToCartRequest> requests) {
        for (AddToCartRequest request : requests) {
            buyItem(request);
        }
    }


    @Override
    public void appendAuthenticateEvent(JsonNode user) {
        EventData eventData = EventData.builderAsJson("AuthenticateEventDIY", user)
                .eventId(UUID.randomUUID())
                .build();
    }
}
