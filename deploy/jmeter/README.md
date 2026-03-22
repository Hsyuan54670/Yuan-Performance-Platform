# JMeter 接入说明

当前 V1 的 `yuan-test` 依赖宿主机上的 JMeter 安装目录，并在运行时动态生成 `.jmx` 文件。

## 路径约定

宿主机路径：

- 根 `.env` 中的 `JMETER_HOME`
- 根 `.env` 中的 `JMETER_RESULTS_DIR`
- 根 `.env` 中的 `JMETER_SCRIPTS_DIR`

容器内路径：

- `JMETER_HOME=/opt/jmeter`
- `JMETER_RESULTS_DIR=/data/jmeter/results`
- `JMETER_SCRIPTS_DIR=/data/jmeter/scripts`

`docker-compose.yml` 会把宿主机路径挂载到这些容器内固定路径，所以 `yuan-test` 运行时读取的应该始终是容器内路径，不应该再填宿主机路径。

## 最低要求

- JMeter 5.6+
- 宿主机存在可用的 JMeter 安装目录
- 宿主机上的结果目录、脚本目录存在并有写权限

## 当前 V1 说明

当前项目已经支持：

- Constant（恒定压测）
- Linear（线性加压）
- Stair（阶梯加压）
