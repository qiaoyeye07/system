#!/usr/bin/env bash
# ================================================================
#  Flea Market - First-Run Setup (Unix/macOS/Git Bash)
#  Run once per machine.
# ================================================================

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo ""
echo "  ============================================================"
echo "    Flea Market - First-Time Setup"
echo "  ============================================================"
echo ""

read -p "  MySQL root password [MySql_123456]: " DB_PASSWORD
DB_PASSWORD="${DB_PASSWORD:-MySql_123456}"

cat > .env << EOF
# Flea Market environment config
# Created by setup.sh $(date)
DB_PASSWORD=${DB_PASSWORD}
DB_HOST=localhost
DB_PORT=3306
DB_NAME=flea_market
DB_USER=root
JWT_SECRET=flea-market-jwt-secret-change-in-production
EOF

echo ""
echo "  [OK] .env created."
echo ""
echo "  Next: Initialize database:"
echo "    mysql -u root -p < database/init.sql"
echo ""
echo "  Then: ./start.sh"
echo ""
