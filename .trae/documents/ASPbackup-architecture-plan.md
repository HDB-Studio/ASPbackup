# ASPbackup Spigot插件架构方案

## 上下文

这是一个全新的Minecraft Spigot插件项目，目前仅包含LICENSE文件。需要开发一个名为"ASPbackup"的高级备份系统，支持：

* 自动启动/关闭备份 + 手动触发

* 备份中断与断点续传

* 分布式多点传输 + 负载均衡

* 配套的备份接收端独立应用

* 完整性校验、日志、错误处理

## 项目结构

```
ASPbackup/
├── pom.xml                          # 父POM（多模块）
├── LICENSE
├── README.md
├── src/main/java/com/aspbackup/
│   ├── ASPBackup.java               # 插件主类
│   ├── core/
│   │   ├── backup/
│   │   │   ├── BackupManager.java          # 备份编排器
│   │   │   ├── BackupTask.java             # 单个备份任务
│   │   │   ├── BackupState.java            # 状态枚举
│   │   │   ├── BackupType.java             # 类型枚举（FULL/INCREMENTAL）
│   │   │   ├── source/
│   │   │   │   ├── BackupSource.java       # 接口
│   │   │   │   ├── DirectoryBackupSource.java
│   │   │   │   └── FileCollector.java      # 文件遍历+过滤
│   │   │   ├── target/
│   │   │   │   ├── BackupTarget.java       # 接口
│   │   │   │   ├── LocalBackupTarget.java
│   │   │   │   ├── RemoteBackupTarget.java
│   │   │   │   └── NASBackupTarget.java
│   │   │   ├── compression/
│   │   │   │   ├── Compressor.java
│   │   │   │   ├── ZipCompressor.java
│   │   │   │   └── TarGzCompressor.java
│   │   │   └── verification/
│   │   │       ├── ChecksumGenerator.java
│   │   │       └── ChecksumVerifier.java
│   │   ├── transfer/
│   │   │   ├── TransferManager.java        # 分布式传输编排器
│   │   │   ├── TransferNode.java
│   │   │   ├── TransferSession.java
│   │   │   ├── chunk/
│   │   │   │   ├── ChunkedTransfer.java
│   │   │   │   ├── Chunk.java
│   │   │   │   └── ChunkAssembler.java
│   │   │   ├── protocol/
│   │   │   │   ├── Packet.java
│   │   │   │   ├── PacketCodec.java
│   │   │   │   ├── HandshakePacket.java
│   │   │   │   ├── ChunkPacket.java
│   │   │   │   ├── AckPacket.java
│   │   │   │   ├── ResumeRequestPacket.java
│   │   │   │   └── StatusPacket.java
│   │   │   ├── loadbalance/
│   │   │   │   ├── LoadBalancer.java
│   │   │   │   ├── RoundRobinBalancer.java
│   │   │   │   └── LeastLoadedBalancer.java
│   │   │   └── connection/
│   │   │       ├── ConnectionPool.java
│   │   │       └── NodeConnection.java
│   │   ├── config/
│   │   │   ├── ConfigManager.java
│   │   │   └── model/
│   │   │       ├── BackupConfig.java
│   │   │       ├── TransferConfig.java
│   │   │       ├── ScheduleConfig.java
│   │   │       └── NodeConfig.java
│   │   ├── schedule/
│   │   │   ├── AutoBackupScheduler.java
│   │   │   ├── StartupBackupListener.java
│   │   │   └── ShutdownBackupListener.java
│   │   ├── resume/
│   │   │   ├── ResumeController.java
│   │   │   └── CheckpointStore.java
│   │   ├── command/
│   │   │   ├── ASPBackupCommand.java
│   │   │   └── subcommands/
│   │   │       ├── StartCommand.java
│   │   │       ├── StopCommand.java
│   │   │       ├── StatusCommand.java
│   │   │       ├── ResumeCommand.java
│   │   │       ├── ListCommand.java
│   │   │       ├── NodesCommand.java
│   │   │       ├── VerifyCommand.java
│   │   │       ├── ReloadCommand.java
│   │   │       └── HelpCommand.java
│   │   ├── event/
│   │   │   ├── BackupStartEvent.java
│   │   │   ├── BackupProgressEvent.java
│   │   │   ├── BackupCompleteEvent.java
│   │   │   ├── BackupErrorEvent.java
│   │   │   └── BackupInterruptEvent.java
│   │   ├── logging/
│   │   │   ├── BackupLogger.java
│   │   │   └── LogEntry.java
│   │   └── util/
│   │       ├── DiskSpaceChecker.java
│   │       ├── FileUtil.java
│   │       └── ThreadUtil.java
│   └── model/
│       ├── BackupRecord.java
│       └── NodeInfo.java
├── src/main/resources/
│   ├── plugin.yml
│   └── config.yml
└── receiver/                         # 独立接收端应用
    ├── pom.xml
    └── src/main/java/com/aspbackup/receiver/
        ├── BackupReceiver.java
        ├── server/
        │   ├── ReceiverServer.java
        │   └── ClientHandler.java
        ├── assembly/
        │   ├── ChunkReceiver.java
        │   └── FileAssembler.java
        └── verification/
            └── IntegrityChecker.java
```

## 技术栈

* **构建工具**: Maven 多模块（父子POM）

* **Java版本**: Java 21（兼容Spigot 1.20.6+）

* **核心依赖**: Spigot API 1.20.6+ (provided), Apache Commons Compress, Netty (receiver), JUnit 5 + Mockito

* **压缩**: 支持ZIP和Tar.gz两种格式

* **网络协议**: 自定义二进制协议（Magic + 包类型 + 长度 + CRC32校验）

## 实施阶段（9个阶段）

### 阶段1：基础搭建

* Maven项目结构、pom.xml、plugin.yml

* ASPBackup.java主类（onEnable/onDisable）

* ConfigManager + config.yml加载和验证

* BackupLogger基础日志系统

* `/aspbackup reload` 命令

### 阶段2：核心备份引擎

* BackupSource/DirectoryBackupSource/FileCollector（文件遍历+过滤）

* BackupTarget/LocalBackupTarget（本地文件系统）

* Compressor接口 + ZipCompressor

* BackupTask（Runnable备份任务）

* BackupManager（start/stop/status）

* 命令：start、stop、status、list

### 阶段3：校验与验证

* ChecksumGenerator（SHA-256）

* ChecksumVerifier

* Manifest生成与持久化

* `/aspbackup verify` 命令

### 阶段4：断点续传

* ResumeController和CheckpointStore

* BackupTask中的检查点保存/加载

* ResumeCommand和BackupInterruptEvent

* TarGzCompressor（流式压缩支持续传）

### 阶段5：自动调度

* StartupBackupListener（启动备份）

* ShutdownBackupListener（关闭备份）

* AutoBackupScheduler（定时备份+静默时段）

* DiskSpaceChecker和DiskSpaceListener

### 阶段6：分布式传输（插件端）

* TransferNode模型、NodeConfig

* ConnectionPool、NodeConnection（TCP Socket）

* Packet层次结构、PacketCodec

* Chunk、ChunkedTransfer

* LoadBalancer接口 + RoundRobinBalancer

* TransferManager + sendToNodes()

* RemoteBackupTarget

* `/aspbackup nodes` 命令组

### 阶段7：接收端应用

* 独立BackupReceiver（Netty NIO）

* ReceiverServer、ClientHandler

* ChunkReceiver、FileAssembler

* 接收端IntegrityChecker

* 端到端测试

### 阶段8：高级功能

* LeastLoadedBalancer

* NASBackupTarget（网络中断恢复）

* 增量备份逻辑

* 性能调优、压力测试

### 阶段9：打磨

* Tab补全

* Help命令

* 游戏内进度条

* 文档（README、配置注释、Wiki）

* 单元测试

## 线程模型

* **主线程**: Bukkit生命周期、命令执行、事件监听

* **备份工作线程池**: FixedThreadPool(size=2)，执行BackupTask

* **传输线程池**: CachedThreadPool，并行发送Chunk到各节点

* **调度任务**: Bukkit异步调度器（定时备份、磁盘检查、连接清理）

* **关闭钩子**: 阻塞主线程，超时后保存检查点退出

## 网络协议

自定义二进制协议，每包格式：

```
Magic(2B) | PacketType(1B) | PayloadLen(4B) | Payload(NB) | CRC32(4B)
```

13种包类型：Handshake、ChunkData、Ack、TransferComplete、ResumeRequest、StatusRequest、Heartbeat等。

## 命令树

```
/aspbackup start [--full|--incremental] [--target <id>]
/aspbackup stop <task-id>
/aspbackup status [task-id]
/aspbackup resume <task-id>
/aspbackup list [--active|--completed|--failed] [--page <n>]
/aspbackup nodes list|add|remove|status|enable|disable
/aspbackup verify <task-id>
/aspbackup reload
/aspbackup help [subcommand]
```

## 关键设计决策

1. **接口驱动**: BackupSource、BackupTarget、Compressor、LoadBalancer均为接口，支持扩展
2. **Bukkit事件系统**: 插件暴露自定义事件，允许其他插件监听备份生命周期
3. **检查点JSON**: 存储在`plugins/ASPbackup/checkpoints/`，支持7天自动清理
4. **连接池**: 避免每次Chunk传输重新建立TCP连接
5. **安全**: 命令权限控制、接收端Token认证、IP白名单
6. **错误隔离**: 异步任务异常不导致服务器崩溃

## 验证方式

1. Maven构建：`mvn clean package` 成功
2. 部署到Spigot 1.16.5测试服务器
3. `/aspbackup reload` 加载配置
4. `/aspbackup start` 手动触发本地备份，验证文件生成
5. `/aspbackup status` 查看进度
6. `/aspbackup verify <id>` 校验完整性
7. 启动/关闭服务器验证自动备份
8. 启动Receiver应用，配置远程节点，测试分布式传输
9. `/aspbackup stop <id>` 中断备份，`/aspbackup resume <id>` 续传

