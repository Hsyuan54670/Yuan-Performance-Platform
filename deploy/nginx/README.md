# Nginx Frontend Proxy

这个目录提供前端静态部署时的 Nginx 反向代理样板。

它完成两件事：

- 让前端静态资源按 SPA 方式回落到 `index.html`
- 把 `/api/*` 转发到网关 `8080`
- 把 `/ws/*` 升级转发到网关 WebSocket

默认假设网关地址是 `127.0.0.1:8080`。如果网关和 Nginx 不在同一台机器，请修改 `yuan-web.conf` 中的 `proxy_pass`。
