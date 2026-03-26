package com.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB auto-configuration handles connection via application.yml.
    // Custom converters or index creation can be added here.
}
