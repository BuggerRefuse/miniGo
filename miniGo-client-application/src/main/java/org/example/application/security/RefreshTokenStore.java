package org.example.application.security;

import java.time.Instant;

public interface RefreshTokenStore {

    void save(RefreshTokenSession session);

    boolean claim(String tokenId, Instant claimedAt);

    void revoke(String tokenId, Instant revokedAt);
}
