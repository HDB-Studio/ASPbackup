#!/bin/bash
# ASPbackup Backup Receiver - Startup Script (Linux/macOS)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$SCRIPT_DIR/ASPbackup-receiver-1.0.0.jar"

# Check if jar exists
if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] Jar file not found: $JAR_FILE"
    echo "Please ensure ASPbackup-receiver.jar is in the same directory as this script."
    exit 1
fi

echo "============================================"
echo "  ASPbackup Backup Receiver v1.0.0"
echo "============================================"
echo ""
echo "Starting receiver..."
echo ""

# Default config
PORT=9876
OUTPUT_DIR="$SCRIPT_DIR/received-backups"
TOKEN="change-me"

# Uncomment and modify below to change defaults:
# PORT=9876
# OUTPUT_DIR="/mnt/backups"
# TOKEN="your-secure-token"

echo "  Port:       $PORT"
echo "  Output Dir: $OUTPUT_DIR"
echo "  Jar:        $JAR_FILE"
echo ""

java -jar "$JAR_FILE" --port $PORT --dir "$OUTPUT_DIR" --token "$TOKEN"