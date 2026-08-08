# ASPbackup v1.0.0 发布包

## 包含内容

### 1. ASPbackup-1.0.0.jar — Minecraft 服务器插件
放入服务器的 `plugins/` 目录即可使用。

**功能：**
- 伺服器启动/关闭时自动备份
- 支持手动触发备份（/aspbackup start）
- 备份中断与断点续传
- 分布式传输至接收端
- 完整中文界面与日志

### 2. receiver/ — 备份接收端（独立运行）
这是**独立应用程序**，不是 Minecraft 插件。运行在备份存储服务器上。

**启动方式：**
- Windows：双击 `start.bat`
- Linux/macOS：`./start.sh`

**首次使用请修改 `application.yml` 中的 `auth-token`！**

## 快速安装

1. 将 `ASPbackup-1.0.0.jar` 复制到 Minecraft 服务器的 `plugins/` 目录
2. 启动服务器，插件会自动生成 `plugins/ASPbackup/config.yml`
3. 根据需要修改配置文件
4. 如需分布式传输，在备份存储服务器上启动接收端

## 系统要求

- Java 21+
- Spigot/Paper 1.20+（插件）
- 任意支持 Java 21 的系统（接收端）