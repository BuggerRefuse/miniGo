# miniGo-gateway

`miniGo-gateway` 是 miniGo 的 API 网关，默认监听 `9000`，通过 Nacos 服务发现转发到 `mini-go-client`。

## 路由

- `/api/**`：转发到 `mini-go-client`，并统一校验 AccessToken。
- `/`、`/login.html`、`/welcome.html`、`/css/**`、`/js/**`：转发用户模块的页面资源。
- `9091/actuator/health`、`9091/actuator/gateway/routes`：管理端口，仅绑定本机回环地址。

登录、微信登录、刷新令牌、退出登录和注册接口不要求网关 AccessToken；其他 `/api/**` 接口默认要求登录。网关会移除客户端伪造的身份头，再根据已校验的令牌写入受信任的身份头。

## 构建和启动

需要环境变量：

- `MINIGO_JWT_SECRET`：必须与 `miniGo-client` 使用同一个 JWT 密钥。
- `NACOS_SERVER_ADDR`：默认 `127.0.0.1:8848`。
- `CORS_ALLOWED_ORIGIN_PATTERNS`：允许跨域的来源，多个值用逗号分隔。

本机启动：

```powershell
mvn clean package
java -jar target/mini-go-gateway.jar
```

Docker 部署：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1
```

## 验证

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

也可以手动检查：

```powershell
Invoke-RestMethod http://127.0.0.1:9091/actuator/health
Invoke-RestMethod http://127.0.0.1:9091/actuator/gateway/routes
Invoke-WebRequest http://127.0.0.1:9000/login.html -UseBasicParsing
```
