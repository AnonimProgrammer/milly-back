package com.milly.auth.application.port.outbound;

import java.util.UUID;

public interface RefreshTokenStore {

    void register(String jti, UUID userId);

    boolean consume(String jti, UUID userId);

    void revoke(String jti);
}
