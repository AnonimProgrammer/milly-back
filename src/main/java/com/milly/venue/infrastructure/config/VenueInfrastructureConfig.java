package com.milly.venue.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VenueProperties.class)
public class VenueInfrastructureConfig {
}