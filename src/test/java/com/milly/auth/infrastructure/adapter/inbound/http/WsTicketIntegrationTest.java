package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.domain.model.AuthUser;
import com.milly.auth.domain.valueobject.RoleName;
import com.milly.auth.infrastructure.adapter.outbound.security.AuthCookieWriter;
import com.milly.auth.infrastructure.adapter.outbound.security.JwtTokenService;
import com.milly.config.infrastructure.adapter.outbound.websocket.NoOpStompSessionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WsTicketIntegrationTest {

    private static final long CONNECT_TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void authenticatedUserReceivesTicket() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/ws-ticket").with(authentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketId").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
    }

    @Test
    void missingSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/ws-ticket"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/ws-ticket").cookie(new Cookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issueAndClaimFlow() throws Exception {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtTokenService.issueAccessToken(new AuthUser(userId, List.of(RoleName.USER)));

        MvcResult result = mockMvc.perform(post("/api/v1/ws-ticket")
                        .cookie(new Cookie(AuthCookieWriter.ACCESS_TOKEN_COOKIE, accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        String ticketId = extractJsonField(result.getResponse().getContentAsString(), "ticketId");

        StompSession session = connect("?ticket=" + ticketId).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    void reusedTicketRejectedAtHandshake() throws Exception {
        UUID userId = UUID.randomUUID();
        String ticketId = issueTicket(userId);

        StompSession firstSession = connect("?ticket=" + ticketId).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        firstSession.disconnect();

        assertThrows(
                ExecutionException.class,
                () -> connect("?ticket=" + ticketId).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private String issueTicket(UUID userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ws-ticket").with(authentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of()))))
                .andExpect(status().isOk())
                .andReturn();
        return extractJsonField(result.getResponse().getContentAsString(), "ticketId");
    }

    private String extractJsonField(String json, String fieldName) {
        String marker = "\"" + fieldName + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Field not found in response: " + fieldName);
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private CompletableFuture<StompSession> connect(String query) {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        String url = "ws://localhost:" + port + "/ws" + query;
        return stompClient.connectAsync(url, new WebSocketHttpHeaders(), new NoOpStompSessionHandler());
    }
}
