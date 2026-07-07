package com.milly.venue.application.service;

import com.milly.config.infrastructure.config.client.ClientProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VenueInvitationUrlBuilderTest {

    @Test
    void buildsInviteUrlWithoutTrailingSlashOnBaseUrl() {
        // Arrange
        VenueInvitationUrlBuilder builder = new VenueInvitationUrlBuilder(
                new ClientProperties("https://app.example.com/"));
        UUID token = UUID.fromString("11111111-1111-1111-1111-111111111111");

        // Act
        String inviteUrl = builder.build(token);

        // Assert
        assertThat(inviteUrl)
                .isEqualTo("https://app.example.com/join-venue/invite/11111111-1111-1111-1111-111111111111");
    }

    @Test
    void buildsInviteUrlWhenBaseUrlHasNoTrailingSlash() {
        // Arrange
        VenueInvitationUrlBuilder builder = new VenueInvitationUrlBuilder(
                new ClientProperties("https://app.example.com"));
        UUID token = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Act
        String inviteUrl = builder.build(token);

        // Assert
        assertThat(inviteUrl)
                .isEqualTo("https://app.example.com/join-venue/invite/22222222-2222-2222-2222-222222222222");
    }
}
