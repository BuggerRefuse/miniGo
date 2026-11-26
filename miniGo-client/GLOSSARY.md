# miniGo Client Accounts

This context owns ordinary user identities and the sessions they use to enter miniGo. Administrator identity belongs to a separate admin context.

## Language

**User**:
A person who signs in with an account password or a WeChat openId.
_Avoid_: Member, player account

**WeChat identity**:
The stable openId returned after the backend exchanges a WeChat login code.
_Avoid_: WeChat account, union account

**Access token**:
A short-lived JWT that authorizes API requests.
_Avoid_: Login token

**Refresh token**:
A longer-lived JWT used once to obtain a new token pair.
_Avoid_: Session key, permanent token
