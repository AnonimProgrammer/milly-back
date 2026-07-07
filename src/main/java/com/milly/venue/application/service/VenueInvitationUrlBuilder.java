package com.milly.venue.application.service;

import com.milly.config.infrastructure.config.client.ClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenueInvitationUrlBuilder {

    private final ClientProperties clientProperties;

    public String build(UUID token) {
        String baseUrl = clientProperties.url().stripTrailing();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/join-venue/invite/" + token;
    }
}
