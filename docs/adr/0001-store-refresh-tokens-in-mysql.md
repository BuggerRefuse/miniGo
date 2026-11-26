---
status: superseded by ADR-0003
---

# Store refresh tokens in MySQL for the first login release

The reference notes recommend Redis for one-time refresh tokens, but this release only has MySQL and Nacos provisioned as required runtime services. Refresh token identifiers are therefore stored in `auth_refresh_token`, and a conditional SQL update atomically claims a token exactly once before a replacement pair is issued. This keeps the distributed deployment self-contained; a later move to Redis changes only `RefreshTokenStore` and its persistence adapter.
