# Nginx Frontend Proxy

这个目录提供前端静态部署时的 Nginx 反向代理样板。

它完成两件事：

- 让前端静态资源按 SPA 方式回落到 `index.html`
- 把 `/api/*` 转发到网关 `8080`
- 把 `/ws/*` 升级转发到网关 WebSocket

在统一 `docker compose` 部署里，默认网关地址是容器服务名 `yuan-gateway:8080`。
如果 Nginx 和 Gateway 不在同一个 Docker 网络里，再按实际地址修改 `yuan-web.conf` 中的 `proxy_pass`。
