package com.example.loanproject.config;

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
    private String streamName;
    private String groupName;

    @Bean
    public EventStoreDBClientSettings eventStoreDBClientSettings() {
        return EventStoreDBConnectionString.parseOrThrow(connectionString);
    }

    @Bean
    public EventStoreDBPersistentSubscriptionsClient eventStoreDBPersistentSubscriptionsClient(EventStoreDBClientSettings settings) {
        return EventStoreDBPersistentSubscriptionsClient.create(settings);
    }
}
