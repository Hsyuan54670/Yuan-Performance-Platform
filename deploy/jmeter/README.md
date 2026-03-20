# JMeter 接入说明

当前 V1 的 `yuan-test` 依赖本机 JMeter 安装目录，并在运行时动态生成 `.jmx` 文件。

## 建议目录

- JMeter Home：`<your-jmeter-home>`
- 结果目录：`<repo>/jmeter/results/`
- 脚本目录：`<repo>/jmeter/scripts/`

## 最低要求

- JMeter 5.6+
- 本机可执行 `jmeter.bat` 或对应平台命令
- `results/` 和 `scripts/` 目录存在并有写权限

## 当前 V1 说明

当前项目已经支持：

- Constant（恒定压测）
- Linear（线性加压）
- Stair（阶梯加压）

如果你要把它部署到另一台机器，记得把 `yuan-test` 的 `jmeter.home / results-dir / scripts-dir` 配到目标环境实际路径。
