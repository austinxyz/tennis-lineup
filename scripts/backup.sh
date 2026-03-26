#!/bin/bash
# 将 Fly.io Volume 上的数据备份到本地
# 用法: ./scripts/backup.sh [app-name]
APP=${1:-tennis-lineup}
DATE=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="./backups"
mkdir -p "$BACKUP_DIR"
echo "Backing up $APP data..."
flyctl ssh console --app "$APP" -C "cat /data/tennis-data.json" > "$BACKUP_DIR/tennis-data-$DATE.json"
flyctl ssh console --app "$APP" -C "cat /data/tennis-config.json" > "$BACKUP_DIR/tennis-config-$DATE.json"
echo "Done. Files saved to $BACKUP_DIR/"
