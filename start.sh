#!/usr/bin/env bash
# ================================================================
#  Flea Market - Unix/macOS/Git Bash Startup
#  Run: ./start.sh
#  Prerequisites: JDK 17+, Node.js 18+, MySQL running
# ================================================================
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

# --- .env check --------------------------------------------------
if [ ! -f ".env" ]; then
    echo ""
    echo "  [SETUP NEEDED] No .env file found."
    echo "  Run: ./setup.sh"
    echo "  Or:  cp .env.template .env  and set DB_PASSWORD"
    echo ""
    exit 1
fi

# --- Load config -------------------------------------------------
source .env 2>/dev/null || true
DB_PASSWORD="${DB_PASSWORD:-MySql_123456}"

# --- Prerequisites -----------------------------------------------
command -v java  >/dev/null 2>&1 || { echo "[FAIL] JDK 17+ required"; exit 1; }
command -v node  >/dev/null 2>&1 || { echo "[FAIL] Node.js 18+ required"; exit 1; }
[ -f "backend/mvnw" ] || { echo "[FAIL] backend/mvnw missing"; exit 1; }

# --- Banner ------------------------------------------------------
clear 2>/dev/null || true
echo ""
echo "  ============================================================"
echo "    Flea Market"
echo "    Backend : http://localhost:8081"
echo "    Frontend: http://localhost:5173"
echo "  ============================================================"
echo ""

# --- Kill old instances ------------------------------------------
kill $(lsof -ti:8081) 2>/dev/null || true
kill $(lsof -ti:5173) 2>/dev/null || true

# --- Install frontend if needed ----------------------------------
if [ ! -d "frontend/node_modules" ]; then
    echo "  Installing frontend dependencies..."
    cd frontend
    npm install
    cd ..
    echo "  Done."
fi

# --- Start backend (mvnw wrapper) --------------------------------
echo "  Starting backend..."
cd backend
chmod +x mvnw 2>/dev/null || true
export DB_PASSWORD
./mvnw spring-boot:run -DskipTests &
BACKEND_PID=$!
cd "$ROOT"

# --- Wait for backend --------------------------------------------
echo "  Waiting for backend..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8081/api/products >/dev/null 2>&1; then
        echo "  Backend ready."
        break
    fi
    sleep 2
done

# --- Start frontend ----------------------------------------------
echo "  Starting frontend..."
cd frontend
npx vite --host 0.0.0.0 &
FRONTEND_PID=$!
cd "$ROOT"

sleep 3

echo ""
echo "  ============================================================"
echo "    Backend  http://localhost:8081/api"
echo "    Frontend http://localhost:5173"
echo "    Login    admin / 12345678"
echo "  ============================================================"
echo "    Press Ctrl+C to stop all services"
echo ""

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT TERM
wait
