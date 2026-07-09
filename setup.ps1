# ================================================================
#  Flea Market - Database Initializer (PowerShell, portable)
#  Usage: powershell -ExecutionPolicy Bypass -File setup.ps1
# ================================================================

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

# --- Load config from .env ---------------------------------------
$DB_USER = "root"; $DB_HOST = "localhost"; $DB_PORT = "3306"
$DB_NAME = "flea_market"; $DB_PASSWORD = "MySql_123456"
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match "^\s*DB_PASSWORD\s*=\s*(.+)$") { $DB_PASSWORD = $Matches[1].Trim() }
        if ($_ -match "^\s*DB_USER\s*=\s*(.+)$")     { $DB_USER     = $Matches[1].Trim() }
        if ($_ -match "^\s*DB_HOST\s*=\s*(.+)$")     { $DB_HOST     = $Matches[1].Trim() }
        if ($_ -match "^\s*DB_PORT\s*=\s*(.+)$")     { $DB_PORT     = $Matches[1].Trim() }
        if ($_ -match "^\s*DB_NAME\s*=\s*(.+)$")     { $DB_NAME     = $Matches[1].Trim() }
    }
}

# --- Find mysql.exe ----------------------------------------------
$SearchPaths = @(
    "C:\Program Files\MySQL\MySQL Workbench 8.0\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe",
    "D:\MySQL\bin\mysql.exe"
)
$Mysql = $null
foreach ($p in $SearchPaths) { if (Test-Path $p) { $Mysql = $p; break } }
if (-not $Mysql) {
    $fromPath = (Get-Command mysql.exe -ErrorAction SilentlyContinue).Source
    if ($fromPath) { $Mysql = $fromPath }
}
if (-not $Mysql) {
    Write-Host "[FAIL] mysql.exe not found." -ForegroundColor Red
    Write-Host "Add MySQL bin directory to PATH, or edit this script."
    Read-Host "Press Enter"
    exit 1
}

$env:MYSQL_PWD = $DB_PASSWORD

Clear-Host
Write-Host ""
Write-Host "  ============================================================" -ForegroundColor Cyan
Write-Host "    Flea Market - Database Setup" -ForegroundColor Cyan
Write-Host "    $DB_USER@${DB_HOST}:$DB_PORT / $DB_NAME" -ForegroundColor Cyan
Write-Host "  ============================================================" -ForegroundColor Cyan

# --- Test connection ---------------------------------------------
Write-Host ""
Write-Host "  Testing connection..." -ForegroundColor Yellow
$test = & $Mysql -u $DB_USER -h $DB_HOST -P $DB_PORT --default-character-set=utf8mb4 -e "SELECT 1 AS ok" 2>&1
if ($test -notmatch "ok") {
    Write-Host "  [FAIL] Cannot connect. Password: $DB_PASSWORD" -ForegroundColor Red
    $new = Read-Host "  Enter correct password"
    if ($new) {
        $DB_PASSWORD = $new; $env:MYSQL_PWD = $DB_PASSWORD
        $test = & $Mysql -u $DB_USER -h $DB_HOST -P $DB_PORT -e "SELECT 1 AS ok" 2>&1
        if ($test -notmatch "ok") {
            Write-Host "  [FAIL] Still cannot connect" -ForegroundColor Red
            Read-Host "Press Enter"; exit 1
        }
        Write-Host "  [OK] Connected. Updating .env..." -ForegroundColor Green
        (Get-Content ".env") -replace "DB_PASSWORD=.*", "DB_PASSWORD=$DB_PASSWORD" | Set-Content ".env"
    } else { exit 1 }
} else {
    Write-Host "  [OK] Connected" -ForegroundColor Green
}

# --- Init database -----------------------------------------------
Write-Host ""
$sqlFile = Join-Path $Root "database\init.sql"
if (-not (Test-Path $sqlFile)) {
    Write-Host "  [FAIL] database\init.sql not found" -ForegroundColor Red
    Read-Host "Press Enter"; exit 1
}

$confirm = Read-Host "  Initialize $DB_NAME database? This destroys existing data (y/n)"
if ($confirm -ne "y") { Write-Host "  Cancelled."; exit 0 }

Write-Host "  Importing init.sql..." -ForegroundColor Yellow
Get-Content $sqlFile | & $Mysql -u $DB_USER -h $DB_HOST -P $DB_PORT --default-character-set=utf8mb4 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Database ready!" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Test accounts (password: 12345678):" -ForegroundColor Green
    Write-Host "    admin, zhangsan, lisi, wangwu, zhaoliu" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Run start.bat to launch the app!" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Import failed. Check password and try again." -ForegroundColor Red
}

Read-Host "Press Enter"
