# Client owns user identity only

The `miniGo-client` deployment owns ordinary user registration, authentication, profile self-service, and user refresh sessions. Administrator identity, administrator login, and cross-user management belong to a separate future `admin` deployment, so this codebase must not contain an `Admin` aggregate or administrator-facing user lists. The contexts may share platform infrastructure such as Nacos and MySQL, but neither owns the other's business data.
