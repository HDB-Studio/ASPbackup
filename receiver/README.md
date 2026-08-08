# ASPbackup 备份接收端

## 概述

ASPbackup 备份接收端是一个**独立运行的 Java 应用程序**，用于接收来自 ASPbackup Spigot 插件
通过分布式传输发送的备份数据。它不是 Minecraft 服务器插件，而是运行在备份存储服务器上的
独立程序。

## 系统要求

- Java 21 或更高版本
- 至少 2GB 可用内存（取决于备份大小）
- 足够的磁盘空间用于存储备份档案

## 快速开始

### Windows
```
双击 start.bat 或在命令行中执行：
java -jar ASPbackup-receiver.jar
```

### Linux/macOS
```bash
chmod +x start.sh
./start.sh
```

### 自定义参数
```bash
java -jar ASPbackup-receiver.jar --port 9876 --dir /mnt/backups --token my-secure-token
```

### 参数说明

| 参数 | 说明 | 预设值 |
|------|------|--------|
| `--port` | 监听端口 | 9876 |
| `--dir` | 备份输出目录 | received-backups |
| `--token` | 认证令牌 | change-me |
| `--help` | 显示帮助 | - |

## 工作流程

1. 接收端启动后监听指定端口
2. ASPbackup 插件通过分布式传输连接接收端
3. 握手认证（验证 token）
4. 分块接收备份数据
5. 所有分块接收完毕后自动组装为完整档案
6. SHA-256 完整性校验
7. 清理分块暂存档

## 配置文件

参见 `application.yml`，支持配置端口、输出目录、认证令牌和日志设置。

## 安全建议

- 生产环境务必修改 `auth-token`，不要使用预设值 `change-me`
- 建议配置防火墙仅允许 Minecraft 服务器 IP 访问接收端端口
- 定期清理旧备份档案以释放磁盘空间