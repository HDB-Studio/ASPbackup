# ASPbackup

> Advanced Backup System for Minecraft Spigot Servers

ASPbackup is a comprehensive backup plugin for Spigot/Paper Minecraft servers (1.20.6+) featuring distributed multi-node transfer, checkpoint/resume, integrity verification, and flexible scheduling.

## Features

### 🔄 Backup Management
- **Auto-backup** on server start and shutdown (configurable)
- **Manual backup** via in-game commands or console
- **Full and incremental** backup modes
- **Custom source paths** with per-source include/exclude filtering
- **Multiple target types**: Local, NAS, and Remote (distributed)

### ⚡ Advanced Capabilities
- **Interrupt & Resume**: Safely stop backups mid-operation and resume from checkpoints
- **Integrity Verification**: SHA-256 checksums with manifest generation
- **Retention Management**: Auto-cleanup of old backups
- **Disk Space Monitoring**: Pre-backup space checks and warnings

### 🌐 Distributed Transfer
- **Multi-node** parallel transfer for faster backups
- **Load balancing**: Round-robin and least-loaded strategies
- **Chunked transfer**: Large files split into configurable chunks
- **Connection pooling**: Reusable TCP connections to reduce overhead
- **Custom binary protocol**: Magic bytes + CRC32 framing for reliability

### 📋 Management
- **9 subcommands**: `start`, `stop`, `status`, `resume`, `list`, `nodes`, `verify`, `reload`, `help`
- **Tab completion** for all commands
- **Permission-based** access control
- **Bukkit events** for plugin interoperability

### 🛡️ Reliability
- **Daily log files** with configurable retention
- **Error isolation**: Backup failures don't crash the server
- **Network retry**: Automatic retry for failed chunk transfers
- **Graceful shutdown**: Checkpoints saved on plugin disable

## Requirements

- **Java 21** or higher
- **Spigot/Paper 1.20.6+** (API version 1.20)
- **Maven 3.8+** (for building from source)

## Quick Start

### Plugin Installation

1. Download `ASPbackup-1.0.0-SNAPSHOT.jar` from [Releases](https://github.com/HDB-Studio/ASPbackup/releases)
2. Place it in your server's `plugins/` folder
3. Start (or restart) your server
4. Edit `plugins/ASPbackup/config.yml` to configure your backup sources and targets
5. Run `/aspbackup reload` to apply configuration changes

### Basic Usage

```
# Start a manual backup
/aspbackup start --target local

# Check backup status
/aspbackup status

# View backup history
/aspbackup list --all

# Reload configuration
/aspbackup reload

# Show all commands
/aspbackup help
```

### Receiver Setup (Distributed Transfer)

1. Download `ASPbackup-receiver-1.0.0-SNAPSHOT.jar`
2. Run on each node:
   ```bash
   java -jar ASPbackup-receiver-1.0.0-SNAPSHOT.jar --port 9876 --dir /backups --token your-secure-token
   ```
3. Configure nodes in `plugins/ASPbackup/config.yml`:
   ```yaml
   transfer:
     nodes:
       - id: "node-1"
         host: "192.168.1.101"
         port: 9876
         auth-token: "your-secure-token"
         enabled: true
         weight: 1
   ```

## Configuration

The main configuration file is `plugins/ASPbackup/config.yml`. Key sections:

| Section | Purpose |
|---------|---------|
| `backup` | Auto-backup settings, compression, sources, targets, file filters |
| `schedule` | Periodic backup scheduling with quiet hours |
| `transfer` | Chunk size, threads, timeouts, retry, load balancing, node list |
| `checkpoint` | Resume support and checkpoint cleanup |
| `logging` | Log level, directory, retention, console output |
| `disk-space` | Free space monitoring thresholds |

## Command Reference

| Command | Permission | Description |
|---------|------------|-------------|
| `/aspbackup start [--full\|--incremental] [--target <id>]` | `aspbackup.start` | Start a manual backup |
| `/aspbackup stop <task-id>` | `aspbackup.stop` | Stop an active backup |
| `/aspbackup status [task-id]` | `aspbackup.status` | View backup status |
| `/aspbackup resume <task-id>` | `aspbackup.resume` | Resume a paused backup |
| `/aspbackup list [--active\|--completed\|--failed]` | `aspbackup.list` | List backup history |
| `/aspbackup nodes <list\|status>` | `aspbackup.nodes` | Manage transfer nodes |
| `/aspbackup verify <task-id>` | `aspbackup.verify` | Verify backup integrity |
| `/aspbackup reload` | `aspbackup.reload` | Reload configuration |
| `/aspbackup help [command]` | `aspbackup.command` | Show help |

## Building from Source

```bash
# Clone the repository
git clone https://github.com/HDB-Studio/ASPbackup.git
cd ASPbackup

# Build the plugin
mvn clean package

# Build the receiver
cd receiver
mvn clean package
```

Output files:
- `target/ASPbackup-1.0.0-SNAPSHOT.jar` — Spigot plugin
- `receiver/target/ASPbackup-receiver-1.0.0-SNAPSHOT.jar` — Standalone receiver

## Architecture

```
ASPbackup/
├── src/main/java/com/aspbackup/     # Spigot Plugin
│   ├── ASPBackup.java               # Plugin main class
│   ├── core/
│   │   ├── backup/                  # Backup engine (sources, targets, compression)
│   │   ├── transfer/                # Distributed transfer (protocol, chunks, load balancing)
│   │   ├── config/                  # Configuration management
│   │   ├── schedule/                # Auto-backup scheduling
│   │   └── resume/                  # Checkpoint & resume
│   ├── command/                     # Command system (9 subcommands)
│   ├── event/                       # Bukkit custom events
│   ├── logging/                     # Backup log system
│   ├── model/                       # Data models
│   └── util/                        # Utilities
└── receiver/                        # Standalone Receiver Application
    └── src/main/java/com/aspbackup/receiver/
        ├── BackupReceiver.java      # Main entry point
        ├── server/                  # Netty TCP server
        ├── assembly/                # Chunk assembly
        └── verification/            # Integrity checking
```

## Network Protocol

The distributed transfer uses a custom binary protocol:

```
┌──────────┬──────────┬──────────┬──────────┬──────────┐
│ Magic(2B)│Type(1B)  │Length(4B)│Payload(NB)│CRC32(4B) │
└──────────┴──────────┴──────────┴──────────┴──────────┘
```

Packet types: Handshake (0x01), ChunkData (0x03), Ack (0x04), ResumeRequest (0x07), Status (0x09)

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an Issue on the [GitHub repository](https://github.com/HDB-Studio/ASPbackup).