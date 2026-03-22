# Deploy Scripts

这里存放部署阶段会用到的辅助脚本。

当前提供：

- `import-nacos-config.ps1`：Windows / PowerShell 下把 `deploy/nacos/<env>` 下的 yml 模板批量导入 Nacos
- `import-nacos-config.sh`：Linux / sh 下把 `deploy/nacos/<env>` 下的 yml 模板批量导入 Nacos

两个脚本都支持 Nacos 鉴权，默认使用 `nacos / nacos`，也可以通过参数覆盖。

## 示例

PowerShell：

```powershell
pwsh -File deploy/scripts/import-nacos-config.ps1 -Env prod -ServerAddr 127.0.0.1:8848 -Username nacos -Password nacos
```

Linux / sh：

```bash
sh deploy/scripts/import-nacos-config.sh --env prod --server-addr 127.0.0.1:8848 --username nacos --password nacos
```