package com.example.demo.config;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "kurrentdb")
public class KurrentDbConfig {
    private String connectionString;

    @Bean
    public EventStoreDBClient eventStoreDBClient() {
        EventStoreDBClient client = EventStoreDBClient.create(EventStoreDBConnectionString.parseOrThrow(connectionString));
        return client;
    }
}
