package com.milly.config.domain;

import com.milly.Application;
import com.milly.config.infrastructure.adapter.DataSourceInitializer;
import com.milly.auth.infrastructure.adapter.outbound.auth.AppleJwtTokenService;
import com.milly.auth.infrastructure.adapter.outbound.auth.GoogleJwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@ContextConfiguration(initializers = DataSourceInitializer.Initializer.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Application.class)
@AutoConfigureRestTestClient
@ActiveProfiles("itest")
public abstract class AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @MockitoBean
    private GoogleJwtTokenService googleJwtTokenService;

    @MockitoBean
    private AppleJwtTokenService appleJwtTokenService;

    @BeforeEach
    void clearAuthCookies() {
        restClient.post()
                .uri("/api/v1/auth/logout")
                .exchange()
                .expectStatus()
                .isOk();
    }
}