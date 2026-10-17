param(
    [switch]$Stop
)

$ErrorActionPreference = 'Stop'
$postgresRoot = Join-Path $env:LOCALAPPDATA 'Programs\PostgreSQL-17\pgsql'
$bin = Join-Path $postgresRoot 'bin'
$dataDirectory = Join-Path $env:LOCALAPPDATA 'PostgreSQL\project-management\data'
$stateDirectory = Split-Path $dataDirectory -Parent
$passwordFile = Join-Path $stateDirectory 'postgres-admin-password'
$initdb = Join-Path $bin 'initdb.exe'
$pgCtl = Join-Path $bin 'pg_ctl.exe'
$psql = Join-Path $bin 'psql.exe'
$createdb = Join-Path $bin 'createdb.exe'

if (-not (Test-Path $pgCtl)) {
    throw "PostgreSQL binaries not found at '$bin'."
}

if ($Stop) {
    if (Test-Path $dataDirectory) {
        & $pgCtl -D $dataDirectory -m fast -w stop
        if ($LASTEXITCODE -ne 0) {
            throw 'PostgreSQL did not stop cleanly.'
        }
        Write-Host 'PostgreSQL stopped. Database files remain on disk.'
    } else {
        Write-Host 'PostgreSQL has not been initialized yet.'
    }
    exit 0
}

New-Item -ItemType Directory -Force -Path $stateDirectory | Out-Null

if (-not (Test-Path (Join-Path $dataDirectory 'PG_VERSION'))) {
    if (Test-Path $dataDirectory) {
        $existingFiles = Get-ChildItem $dataDirectory -Force
        if ($existingFiles.Count -gt 0) {
            throw "The PostgreSQL data folder is not empty but is not initialized: '$dataDirectory'."
        }
    }

    if (-not (Test-Path $passwordFile)) {
        $randomBytes = [byte[]]::new(32)
        $randomNumberGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $randomNumberGenerator.GetBytes($randomBytes)
        } finally {
            $randomNumberGenerator.Dispose()
        }
        $adminPassword = [Convert]::ToBase64String($randomBytes).TrimEnd('=').Replace('+', 'A').Replace('/', 'B')
        [System.IO.File]::WriteAllText($passwordFile, $adminPassword, [System.Text.Encoding]::ASCII)
    }

    $temporaryPasswordFile = Join-Path $env:TEMP ("postgres-init-{0}.txt" -f [guid]::NewGuid())
    Copy-Item $passwordFile $temporaryPasswordFile
    try {
        & $initdb -D $dataDirectory -U postgres --encoding=UTF8 --auth=scram-sha-256 "--pwfile=$temporaryPasswordFile"
        if ($LASTEXITCODE -ne 0) {
            throw 'PostgreSQL cluster initialization failed.'
        }
    } finally {
        Remove-Item $temporaryPasswordFile -Force -ErrorAction SilentlyContinue
    }
}

& $pgCtl status -D $dataDirectory *> $null
if ($LASTEXITCODE -ne 0) {
    $logFile = Join-Path $stateDirectory 'postgresql.log'
    & $pgCtl -D $dataDirectory -l $logFile -o '-h 127.0.0.1 -p 5432' -w start
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL failed to start. Check '$logFile'."
    }
}

$env:PGPASSWORD = [System.IO.File]::ReadAllText($passwordFile).Trim()
$roleExists = & $psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -tA -c "SELECT 1 FROM pg_roles WHERE rolname = 'project_app'"
if ($LASTEXITCODE -ne 0) {
    throw 'Could not connect to PostgreSQL as its local administrator.'
}
if ("$roleExists".Trim() -ne '1') {
    $appPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'project_dev_password' }
    $escapedPassword = $appPassword.Replace("'", "''")
    & $psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -c "CREATE ROLE project_app LOGIN PASSWORD '$escapedPassword'"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not create the project_app database role.'
    }
}

$databaseExists = & $psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -tA -c "SELECT 1 FROM pg_database WHERE datname = 'project_management'"
if ($LASTEXITCODE -ne 0) {
    throw 'Could not check for the project_management database.'
}
if ("$databaseExists".Trim() -ne '1') {
    & $createdb -h 127.0.0.1 -p 5432 -U postgres -O project_app project_management
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not create the project_management database.'
    }
}

Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
Write-Host 'PostgreSQL is running on localhost:5432.'
Write-Host 'Database: project_management; application role: project_app.'
