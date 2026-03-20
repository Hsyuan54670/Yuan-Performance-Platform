# Deploy Scripts

这里存放部署阶段会用到的辅助脚本。

当前提供：

- `import-nacos-config.ps1`：把 `deploy/nacos/<env>` 下的 yml 模板批量导入 Nacos

## 示例

```powershell
pwsh -File deploy/scripts/import-nacos-config.ps1 -Env dev -ServerAddr 127.0.0.1:8848
```
