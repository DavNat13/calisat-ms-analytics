package com.califorge.msanalytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Cliente DynamoDB (credenciales por la cadena por defecto: rol de la EC2 / LabInstanceProfile). */
@Configuration
@Profile("dynamodb")
public class DynamoConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(@Value("${calisat.analytics.dynamodb.region}") String region) {
        return DynamoDbClient.builder().region(Region.of(region)).build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }
}
