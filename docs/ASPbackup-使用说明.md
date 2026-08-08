# ASPbackup 使用說明書

> Minecraft Spigot 伺服器進階備份插件 | Java 21 | Spigot 1.20.6+

---

## 目錄

1. [安裝與需求](#1-安裝與需求)
2. [快速開始](#2-快速開始)
3. [配置檔案詳解](#3-配置檔案詳解)
4. [命令參考](#4-命令參考)
5. [備份流程說明](#5-備份流程說明)
6. [分散式傳輸設定](#6-分散式傳輸設定)
7. [接收端應用程式](#7-接收端應用程式)
8. [常見問題](#8-常見問題)
9. [權限節點](#9-權限節點)

---

## 1. 安裝與需求

### 系統需求

| 項目 | 需求 |
|------|------|
| Java | **21** 或更高版本 (Temurin/OpenJDK) |
| 伺服器 | Spigot / Paper **1.20.6+** |
| 儲存空間 | 備份目標至少需要與來源目錄相同的可用空間 |

### 安裝步驟

1. 從 [Releases](https://github.com/HDB-Studio/ASPbackup/releases) 下載 `ASPbackup-1.0.0.jar`
2. 將 jar 檔案放入伺服器的 `plugins/` 目錄
3. 啟動（或重啟）伺服器
4. 插件會自動生成 `plugins/ASPbackup/config.yml` 配置檔案
5. 編輯配置檔案以符合您的需求
6. 執行 `/aspbackup reload` 重新載入配置

### 目錄結構

```
plugins/ASPbackup/
├── config.yml          # 主配置檔案
├── temp/               # 暫存目錄（壓縮中的備份檔）
├── checkpoints/        # 中斷備份的檢查點檔案
├── logs/               # 備份日誌檔案
│   ├── backup-2026-08-08.log
│   └── backup-2026-08-09.log
└── backups/            # 本地備份目標（預設）
    ├── aspbackup-a1b2c3d4-20260808-143022.tar.gz
    └── aspbackup-e5f6g7h8-20260809-080015.tar.gz
```

---

## 2. 快速開始

### 基本操作

```
# 手動觸發備份（使用預設本地目標）
/aspbackup start

# 指定備份目標
/aspbackup start --target local

# 查看備份狀態
/aspbackup status

# 查看所有備份任務
/aspbackup list --all

# 重新載入配置
/aspbackup reload

# 查看所有命令
/aspbackup help
```

### 自動備份

插件預設啟用**啟動備份**和**關閉備份**，無需任何額外設定：

- **伺服器啟動時**：延遲 30 秒後自動執行備份（可在配置中調整）
- **伺服器關閉時**：收到 `/stop` 命令後自動執行備份

### 定時備份

在 `config.yml` 中啟用定時備份：

```yaml
schedule:
  enabled: true
  interval-minutes: 360    # 每 6 小時備份一次
  backup-type: "full"
  target-id: "local"
  quiet-hours:
    - "02:00-04:00"        # 凌晨 2-4 點不執行備份
```

---

## 3. 配置檔案詳解

### 3.1 備份設定 (`backup`)

```yaml
backup:
  # 伺服器啟動時是否自動備份
  auto-on-start: true

  # 伺服器關閉時是否自動備份
  auto-on-shutdown: true

  # 啟動備份的延遲秒數（讓伺服器穩定後再備份）
  start-delay-seconds: 30

  # 暫存目錄
  temp-directory: "plugins/ASPbackup/temp"

  # 單次備份最大大小（MB），0 = 無限制
  max-backup-size-mb: 0

  # 備份完成後是否驗證完整性
  verify-after-backup: true

  # 壓縮設定
  compression:
    format: "targz"       # "zip" 或 "targz"
    level: 6              # 1 (最快) 到 9 (最小)
```

### 3.2 備份來源 (`sources`)

```yaml
backup:
  sources:
    - path: "world"           # 相對於伺服器根目錄的路徑
      name: "main-world"      # 顯示名稱
      exclude:                # 排除的檔案（glob 模式）
        - "**/session.lock"
        - "**/uid.dat"
      max-depth: 100          # 最大目錄深度

    - path: "world_nether"
      name: "nether"
      exclude:
        - "**/session.lock"

    - path: "world_the_end"
      name: "the-end"
      exclude:
        - "**/session.lock"

    - path: "plugins"
      name: "plugins"
      exclude:
        - "**/ASPbackup/temp/**"
        - "**/dynmap/web/**"
        - "**/*.log"
      max-depth: 50
```

### 3.3 檔案過濾規則 (`file-filter`)

```yaml
backup:
  file-filter:
    # 包含的檔案（glob 模式）
    include:
      - "**/*"

    # 排除的檔案
    exclude:
      - "**/*.tmp"
      - "**/*.lock"
      - "**/*.pid"

    # 最小檔案大小（位元組），0 = 不限制
    min-size-bytes: 0

    # 最大檔案大小（位元組），0 = 不限制
    max-size-bytes: 0
```

**Glob 模式說明**：

| 模式 | 說明 | 範例 |
|------|------|------|
| `*` | 匹配任意字元（不含 `/`） | `*.log` 匹配所有 .log 檔案 |
| `**` | 匹配任意目錄層級 | `**/temp/**` 匹配所有 temp 目錄下的檔案 |
| `?` | 匹配單個字元 | `region/r.?.?.mca` |

### 3.4 備份目標 (`targets`)

```yaml
backup:
  targets:
    # 本地目標
    - id: "local"
      type: "LOCAL"
      path: "backups/"              # 相對於伺服器根目錄
      retention-count: 10           # 保留最近 10 個備份
      min-free-space-mb: 1024       # 最少需要 1GB 可用空間

    # NAS 目標（掛載的網路磁碟）
    - id: "nas"
      type: "NAS"
      path: "/mnt/nas/minecraft-backups/"
      retention-count: 5
      min-free-space-mb: 5120

    # 遠端分散式目標
    - id: "remote-cluster"
      type: "REMOTE"
      retention-count: 3
      min-free-space-mb: 10240
```

### 3.5 分散式傳輸設定 (`transfer`)

```yaml
transfer:
  # 分塊大小（KB）
  chunk-size-kb: 1024

  # 並行傳輸執行緒數
  parallel-threads: 4

  # 連線超時（毫秒）
  connect-timeout-ms: 10000

  # 讀取超時（毫秒）
  read-timeout-ms: 30000

  # 失敗重試次數
  retry-count: 3

  # 重試間隔（毫秒）
  retry-delay-ms: 5000

  # 負載均衡策略："round_robin" 或 "least_loaded"
  load-balance-strategy: "least_loaded"

  # 傳輸節點列表
  nodes:
    - id: "node-1"
      host: "192.168.1.101"
      port: 9876
      auth-token: "your-secure-token-here"
      enabled: true
      weight: 1
```

### 3.6 檢查點設定 (`checkpoint`)

```yaml
checkpoint:
  # 啟用中斷續傳
  enabled: true

  # 檢查點儲存目錄
  directory: "plugins/ASPbackup/checkpoints"

  # 自動清理超過 N 天的檢查點
  max-age-days: 7
```

### 3.7 日誌設定 (`logging`)

```yaml
logging:
  # 日誌級別："DEBUG", "INFO", "WARN", "ERROR"
  level: "INFO"

  # 日誌目錄
  directory: "plugins/ASPbackup/logs"

  # 保留天數
  retention-days: 30

  # 詳細傳輸日誌（記錄每個分塊的傳輸狀態）
  verbose-transfer: false

  # 同時輸出到控制台
  console-output: true
```

### 3.8 磁碟空間監控 (`disk-space`)

```yaml
disk-space:
  # 可用空間低於此百分比時警告
  warn-threshold-percent: 15

  # 檢查間隔（秒）
  check-interval-seconds: 300
```

---

## 4. 命令參考

### 命令總覽

| 命令 | 權限 | 說明 |
|------|------|------|
| `/aspbackup start` | `aspbackup.start` | 手動開始備份 |
| `/aspbackup stop <id>` | `aspbackup.stop` | 停止備份（儲存檢查點） |
| `/aspbackup status [id]` | `aspbackup.status` | 查看備份狀態 |
| `/aspbackup resume <id>` | `aspbackup.resume` | 從檢查點續傳 |
| `/aspbackup list` | `aspbackup.list` | 列出備份歷史 |
| `/aspbackup nodes` | `aspbackup.nodes` | 管理傳輸節點 |
| `/aspbackup verify <id>` | `aspbackup.verify` | 驗證備份完整性 |
| `/aspbackup reload` | `aspbackup.reload` | 重新載入配置 |
| `/aspbackup about` | `aspbackup.command` | 顯示插件資訊 |
| `/aspbackup info` | `aspbackup.command` | 顯示運行狀態 |
| `/aspbackup help` | `aspbackup.command` | 顯示命令幫助 |

### 詳細命令說明

#### `/aspbackup start`

```
/aspbackup start [--full|--incremental] [--target <目標ID>]
```

啟動一個新的備份任務。

**參數**：
- `--full`：完整備份（預設）
- `--incremental`：增量備份（僅備份變更的檔案）
- `--target <id>`：指定目標（預設使用第一個配置的目標）

**範例**：
```
/aspbackup start
/aspbackup start --target nas
/aspbackup start --incremental --target local
```

**輸出**：
```
Backup started! Task ID: a1b2c3d4
```

---

#### `/aspbackup stop`

```
/aspbackup stop <任務ID>
```

安全停止正在執行的備份任務，並儲存檢查點以便後續續傳。

**範例**：
```
/aspbackup stop a1b2c3d4
```

**輸出**：
```
Backup task a1b2c3d4 has been stopped.
A checkpoint has been saved. Use /aspbackup resume a1b2c3d4 to continue.
```

---

#### `/aspbackup status`

```
/aspbackup status [任務ID]
```

查看備份任務的狀態。

**不帶參數**：顯示所有任務摘要
**帶任務ID**：顯示該任務的詳細進度

**範例輸出（摘要）**：
```
===== Backup Tasks =====
[COMPLETED] a1b2c3d4 - 100.0% - FULL
[PAUSED]    e5f6g7h8 - 47.3% - FULL
[RUNNING]   i9j0k1l2 - 12.8% - INCREMENTAL
```

**範例輸出（詳細）**：
```
===== Task: a1b2c3d4 =====
Type: FULL
State: COMPLETED
Progress: 100.0%
Files: 15234/15234
Bytes: 2.1 GB/2.1 GB
Target: local
Started: 2026-08-08T14:30:22Z
```

---

#### `/aspbackup resume`

```
/aspbackup resume <任務ID>
```

從中斷的檢查點恢復備份任務。

**注意**：僅能恢復狀態為 `PAUSED` 的任務。

**範例**：
```
/aspbackup resume e5f6g7h8
```

---

#### `/aspbackup list`

```
/aspbackup list [--active|--completed|--failed|--all] [--page <頁碼>]
```

列出備份任務歷史。

**範例**：
```
/aspbackup list --completed
/aspbackup list --all --page 2
```

---

#### `/aspbackup nodes`

```
/aspbackup nodes list
/aspbackup nodes status [節點ID]
```

管理分散式傳輸節點。

**範例輸出**：
```
===== Transfer Nodes =====
  node-1 - 192.168.1.101:9876 [ENABLED]
  node-2 - 192.168.1.102:9876 [DISABLED]
```

---

#### `/aspbackup verify`

```
/aspbackup verify <任務ID>
```

驗證已完成備份的 SHA-256 完整性。

---

#### `/aspbackup reload`

```
/aspbackup reload
```

重新載入 `config.yml`，無需重啟伺服器。

**注意**：正在執行的備份任務不受影響，新配置將在下一次備份時生效。

---

#### `/aspbackup about`

```
/aspbackup about
```

顯示插件名稱、版本、作者和網站資訊。

---

#### `/aspbackup info`

```
/aspbackup info
```

顯示當前運行狀態，包括：
- 啟動/關閉備份是否啟用
- 定時備份狀態
- 備份來源數量
- 備份目標數量
- 傳輸節點數量
- 活動備份任務數

---

## 5. 備份流程說明

### 5.1 完整備份流程

```
1. 觸發備份（手動/自動/定時）
       │
2. 儲存世界資料（主執行緒）
   ├── /save-off   → 關閉自動存檔
   ├── /save-all   → 強制儲存所有世界
   └── /save-on    → 重新開啟自動存檔
       │
3. 檢查磁碟空間
   ├── 不足 → 終止備份，記錄錯誤
   └── 充足 → 繼續
       │
4. 收集檔案（遍歷所有來源目錄，套用過濾規則）
       │
5. 壓縮檔案（ZIP 或 Tar.gz，支援 SHA-256 計算）
       │
6. 傳輸到目標
   ├── LOCAL  → 複製到本地目錄
   ├── NAS    → 複製到網路磁碟（含重試機制）
   └── REMOTE → TCP 分塊傳輸到接收端（含 ACK 確認、SHA-256 校驗）
       │
7. 驗證完整性
       │
8. 清理舊備份（保留策略）
       │
9. 完成 → 記錄日誌
```

### 5.2 阻塞式備份機制

啟動和關閉備份使用**阻塞式備份**，確保備份完整執行後再繼續：

- **伺服器啟動時**：主執行緒在 `save-off/save-all/save-on` 後，將備份提交到背景執行緒，然後阻塞等待完成
- **伺服器關閉時**：攔截 `/stop` 命令，先執行備份（阻塞），備份完成後再關閉伺服器
- **手動備份**：非阻塞，玩家可繼續遊戲

```
主執行緒                              背景執行緒
  │
  ├─ save-off / save-all / save-on     │
  ├─ future.get() ← 阻塞等待...        ├─ 收集檔案
  │                                   ├─ 壓縮
  │                                   ├─ 傳輸到目標
  │                                   └─ 完成 ✓
  │ ← 備份完成，繼續載入/關閉            │
```

### 5.3 中斷與續傳流程

```
備份進行中
       │
管理員執行 /aspbackup stop
       │
       ├── 設定中斷標記
       ├── 在安全的邊界點暫停（當前檔案處理完後）
       ├── 儲存檢查點 → plugins/ASPbackup/checkpoints/<id>.properties
       └── 任務狀態變為 PAUSED
       
管理員稍後執行 /aspbackup resume
       │
       ├── 載入檢查點
       ├── 從中斷位置繼續收集檔案
       └── 完成後刪除檢查點
```

### 5.4 備份任務狀態

| 狀態 | 說明 |
|------|------|
| `INITIALIZING` | 任務已建立，正在初始化 |
| `COLLECTING` | 正在收集要備份的檔案 |
| `COMPRESSING` | 正在壓縮檔案 |
| `TRANSFERRING` | 正在傳輸到目標 |
| `VERIFYING` | 正在驗證完整性 |
| `PAUSED` | 已暫停，檢查點已儲存 |
| `COMPLETED` | 備份成功完成 |
| `CANCELLED` | 備份已被取消 |
| `FAILED` | 備份失敗 |

---

## 6. 分散式傳輸設定

### 6.1 架構說明

```
┌─────────────────┐
│  Minecraft 伺服器 │  (ASPbackup 插件)
│  192.168.1.100   │
└────────┬────────┘
         │ TCP 9876
         ├──────────────┬──────────────┐
         ▼              ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  接收端 #1   │ │  接收端 #2   │ │  接收端 #3   │
│ 10.0.0.101  │ │ 10.0.0.102  │ │ 10.0.0.103  │
│  NAS 掛載    │ │  本地磁碟    │ │  本地磁碟    │
└─────────────┘ └─────────────┘ └─────────────┘
```

### 6.2 負載均衡策略

**輪詢（Round Robin）**：
- 依序將分塊分配給每個線上節點
- 適合節點效能相近的環境

**最少負載（Least Loaded）**：
- 將分塊分配給目前傳輸量最少的節點
- 考慮節點權重（weight）
- 節點失敗時增加虛擬負載作為懲罰
- 適合節點效能不同的環境

### 6.3 傳輸協議

```
每個封包格式：
┌──────────┬──────────┬──────────┬──────────┬──────────┐
│ Magic(2B)│Type(1B)  │Length(4B)│Payload   │CRC32(4B) │
│ 0x41 0x42│          │          │(NB)      │          │
└──────────┴──────────┴──────────┴──────────┴──────────┘

封包類型：
  0x01 - Handshake      (認證握手)
  0x03 - ChunkData      (分塊數據)
  0x04 - Ack            (確認回覆)
  0x07 - ResumeRequest  (續傳請求)
  0x09 - Status         (狀態報告)
```

---

## 7. 接收端應用程式

### 7.1 快速啟動

**Windows**：雙擊 `start.bat`

**Linux/macOS**：
```bash
chmod +x start.sh
./start.sh
```

**或手動啟動**：
```bash
java -jar ASPbackup-receiver-1.0.0.jar \
  --port 9876 \
  --dir /mnt/backups/minecraft \
  --token your-secure-token
```

### 7.2 接收端目錄結構

```
receiver/
├── start.bat                        # Windows 啟動腳本
├── start.sh                         # Linux/macOS 啟動腳本
├── ASPbackup-receiver-1.0.0.jar     # 接收端主程式
├── application.yml                  # 設定檔
└── received-backups/                # 接收的備份輸出目錄
    └── <任務ID>/
        └── backup.tar.gz
```

### 7.3 參數說明

| 參數 | 預設值 | 說明 |
|------|--------|------|
| `--port` | `9876` | 監聽埠號 |
| `--dir` | `received-backups` | 備份輸出目錄 |
| `--token` | `change-me` | 認證令牌（必須與插件 `auth-token` 一致） |
| `--help` | - | 顯示幫助 |

### 7.4 傳輸協議

```
握手階段：
  插件 → 接收端：protocol_version(2B) + token_len(1B) + token + node_id_len(1B) + node_id + capabilities(1B)
  接收端 → 插件：ACK(4B, 0=成功)

分塊傳輸：
  插件 → 接收端：task_id_len(2B) + task_id + chunk_index(8B) + total_chunks(8B) + offset(8B) + data_len(4B) + checksum_len(2B) + checksum + data
  接收端 → 插件：task_id + chunk_index(8B) + success(1B) + error_msg

組裝完成：
  接收端 → 插件：task_id + success(1B) + message
  接收端自動組裝分塊 → backup.tar.gz → SHA-256 校驗
```

### 7.5 安全建議

1. **使用防火牆**：限制只有 Minecraft 伺服器 IP 可以連接收端埠
2. **使用強密碼令牌**：避免使用預設的 `change-me`
3. **使用 VPN**：如果接收端在遠端網路，建議透過 VPN 連接
4. **定期更換令牌**：定期更新 `auth-token` 並重啟兩端服務

---

## 8. 常見問題

### Q: 備份失敗，提示 "Insufficient disk space"

**原因**：目標磁碟空間不足。

**解決方案**：
1. 清理目標目錄的舊備份
2. 調整 `retention-count` 減少保留數量
3. 降低 `min-free-space-mb` 閾值
4. 擴充目標磁碟容量

### Q: 備份過程中伺服器 lag

**原因**：壓縮大型世界檔案消耗 CPU 和 I/O。

**解決方案**：
1. 降低 `compression.level`（如從 6 降到 3）
2. 排除不必要的目錄（如 `dynmap/web/`）
3. 使用 `quiet-hours` 在玩家較少的時段執行定時備份
4. 增加 `start-delay-seconds` 避免啟動時備份

### Q: 如何恢復備份？

備份檔案為標準的 `.tar.gz` 或 `.zip` 格式：

```bash
# 解壓縮 tar.gz
tar -xzf aspbackup-a1b2c3d4-20260808-143022.tar.gz

# 解壓縮 zip
unzip aspbackup-a1b2c3d4-20260808-143022.zip
```

解壓後直接將目錄內容複製回伺服器對應位置即可。

### Q: 中斷的備份如何續傳？

```bash
# 1. 查看暫停的任務
/aspbackup status

# 2. 續傳
/aspbackup resume <任務ID>

# 3. 如果檢查點損壞，可以刪除檢查點檔案重新開始
rm plugins/ASPbackup/checkpoints/<任務ID>.properties
```

### Q: NAS 目標掛載中斷怎麼辦？

NAS 目標內建重試機制（最多重試 3 次，每次間隔遞增），如果 NAS 暫時不可用，插件會自動重試。如果 NAS 長時間不可用，備份會失敗並記錄錯誤。

### Q: 如何變更備份目標？

編輯 `config.yml` 中的 `backup.targets` 後：

```
/aspbackup reload
/aspbackup start --target <新目標ID>
```

---

## 9. 權限節點

| 權限節點 | 說明 | 預設 |
|----------|------|------|
| `aspbackup.command` | 使用 `/aspbackup` 命令 | OP |
| `aspbackup.start` | 手動開始備份 | OP |
| `aspbackup.stop` | 停止備份任務 | OP |
| `aspbackup.status` | 查看備份狀態 | OP |
| `aspbackup.resume` | 續傳中斷的備份 | OP |
| `aspbackup.list` | 列出備份歷史 | OP |
| `aspbackup.nodes` | 管理傳輸節點 | OP |
| `aspbackup.verify` | 驗證備份完整性 | OP |
| `aspbackup.reload` | 重新載入配置 | OP |
| `aspbackup.*` | 所有權限（萬用字元） | OP |

### 權限設定範例（LuckPerms）

```
# 給予管理員所有權限
/lp user Admin permission set aspbackup.*

# 給予協管員基本權限
/lp group moderator permission set aspbackup.command
/lp group moderator permission set aspbackup.status
/lp group moderator permission set aspbackup.list
```

---

## 技術支援

- **GitHub Issues**: [https://github.com/HDB-Studio/ASPbackup/issues](https://github.com/HDB-Studio/ASPbackup/issues)
- **授權**: Apache License 2.0

---

*ASPbackup v1.0.0-SNAPSHOT — 文件最後更新：2026-08-08*