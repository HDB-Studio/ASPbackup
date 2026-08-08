#!/bin/bash
# ASPbackup 备份接收端 — 启动脚本 (Linux/macOS)

echo "============================================"
echo "  ASPbackup 备份接收端 v1.0.0"
echo "============================================"
echo ""
echo "正在启动接收端..."

# 预设配置（可在此修改）
PORT=9876
OUTPUT_DIR="received-backups"
TOKEN="change-me"

java -jar ASPbackup-receiver.jar --port $PORT --dir "$OUTPUT_DIR" --token $TOKEN