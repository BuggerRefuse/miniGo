---
status: accepted
---

# Store refresh-token sessions in Redis

Access tokens remain short-lived stateless JWTs, while each refresh token must have a matching `jti` whitelist entry in Redis for its full refresh lifetime. Refreshing atomically consumes that entry, logout deletes it, and token values are not stored in MySQL. This keeps ordinary authenticated requests off Redis while making refresh-token reuse and logout revocation immediate, at the cost of requiring Redis for login and refresh.
