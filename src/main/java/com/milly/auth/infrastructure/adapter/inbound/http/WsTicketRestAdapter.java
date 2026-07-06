package com.milly.auth.infrastructure.adapter.inbound.http;

import com.milly.auth.application.dto.IssueWsTicketResponse;
import com.milly.auth.application.usecase.IssueWsTicketUseCase;
import com.milly.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WsTicketRestAdapter {

    private final IssueWsTicketUseCase issueWsTicketUseCase;

    @PostMapping("/ws-ticket")
    public ResponseEntity<ApiResponse<IssueWsTicketResponse>> issueWsTicket(
            @AuthenticationPrincipal UUID userId) {
        IssueWsTicketResponse response = issueWsTicketUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "WebSocket ticket issued."));
    }
}
