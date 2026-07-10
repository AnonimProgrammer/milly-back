package com.milly.venue.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "venue")
public record VenueProperties(
        Invitation invitation
) {

    public VenueProperties {
        if (invitation == null) {
            invitation = new Invitation(86_400);
        }
    }

    public record Invitation(long ttlSeconds) {
        public Invitation {
            if (ttlSeconds <= 0) {
                ttlSeconds = 86_400;
            }
        }
    }
}