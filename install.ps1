#Requires -Version 5.1
<#
.SYNOPSIS
    Builds the Redisca Block mod and copies the finished jar into a Minecraft mods folder.

.DESCRIPTION
    Steps:
      1. build the mod (.\gradlew.bat build)
      2. copy build\libs\redisca_block-<version>.jar into the mods folder

.PARAMETER ModsDir
    Path to the Minecraft mods folder.
    Default: $env:APPDATA\.minecraft\mods   (the default single-instance mods folder)

.PARAMETER NoBuild
    Reuse an already-built jar (build\libs\redisca_block-<version>.jar).

    NOTE: building requires internet access (plugins.gradle.org, maven.minecraftforge.net,
    repo.maven.apache.org, services.gradle.org) and Java 25.

.EXAMPLE
    .\install.ps1
    .\install.ps1 -ModsDir "D:\Games\mods"
    .\install.ps1 -NoBuild
#>
param(
    [string]$ModsDir,
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarName = 'redisca_block-1.0.0.jar'
$jar = Join-Path $root "build\libs\$jarName"

# --- 1. sanity check: java must exist ---------------------------------------
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "ERROR: 'java' was not found in PATH." -ForegroundColor Red
    Write-Host "Install Java 25+ from https://adoptium.net/ (e.g. Temurin 25) and try again."
    exit 1
}

# --- 2. build -----------------------------------------------------------------
if (-not $NoBuild) {
    Write-Host '==> Building the mod (this downloads Gradle, Minecraft and Forge the first time)...' -ForegroundColor Cyan
    & (Join-Path $root 'gradlew.bat') build
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'ERROR: build failed. Check the messages above.' -ForegroundColor Red
        Write-Host '  - This needs internet access and Java 25.' -ForegroundColor Yellow
        Write-Host '  - ForgeGradle needs to reach maven.minecraftforge.net (and plugins.gradle.org, repo.maven.apache.org, services.gradle.org).' -ForegroundColor Yellow
        exit 1
    }
}

# --- 3. locate the jar ---------------------------------------------------------
if (-not (Test-Path $jar)) {
    Write-Host "ERROR: jar not found at $jar" -ForegroundColor Red
    Write-Host '      Run without -NoBuild to build it first.' -ForegroundColor Yellow
    exit 1
}

# --- 4. choose a mods folder ---------------------------------------------------
if ([string]::IsNullOrWhiteSpace($ModsDir)) {
    $ModsDir = Join-Path $env:APPDATA '.minecraft\mods'
    Write-Host "Defaulting mods folder to: $ModsDir" -ForegroundColor DarkYellow
}

Write-Host "==> Installing into $ModsDir" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $ModsDir | Out-Null

Copy-Item -Path $jar -Destination (Join-Path $ModsDir $jarName) -Force

Write-Host ""
Write-Host "Done. Installed: $(Join-Path $ModsDir $jarName)" -ForegroundColor Green
Write-Host "Now launch Minecraft 26.3 + Forge 66.x - blocks will place on any height."
