package com.example.demo.config;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.eventstore.dbclient.EventStoreDBPersistentSubscriptionsClient;
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
    public EventStoreDBClientSettings eventStoreDBClientSettings() {
        return EventStoreDBConnectionString.parseOrThrow(connectionString);
    }

    @Bean
    public EventStoreDBClient eventStoreDBClient(EventStoreDBClientSettings settings) {
        return EventStoreDBClient.create(settings);
    }

    @Bean
    public EventStoreDBPersistentSubscriptionsClient eventStoreDBPersistentSubscriptionsClient(EventStoreDBClientSettings settings) {
        return EventStoreDBPersistentSubscriptionsClient.create(settings);
    }
}
