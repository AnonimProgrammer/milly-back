package com.milly;

import com.milly.config.domain.AbstractITest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

import javax.sql.DataSource;

@Slf4j
class ApplicationITest extends AbstractITest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() throws Exception {
        try (var connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            log.info("Integration test context loaded — datasource URL: {}", url);
        }
    }

    @Test
    void healthEndpointIsAvailable() {
        restClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk();
    }
}
