param(
    [Parameter(Mandatory = $true)]
    [string] $Tag,

    [string] $Owner = "stevennblanc",
    [string] $Repo = "StoryBoy",
    [string] $ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [string] $ManifestPath = "release\update.json"
)

$ErrorActionPreference = "Stop"

$authStatus = gh auth status 2>&1 | Out-String
if ($authStatus -notmatch "account $Owner") {
    throw "GitHub CLI is not authenticated as $Owner. Refusing to publish."
}

if ($authStatus -match "langetrinidadwebtools" -and $authStatus -match "Active account:\s+true") {
    throw "langetrinidadwebtools appears to be active. Refusing to publish."
}

if (-not (Test-Path $ApkPath)) {
    throw "APK not found at $ApkPath. Run .\gradlew.bat :app:assembleDebug first."
}

if (-not (Test-Path $ManifestPath)) {
    throw "Update manifest not found at $ManifestPath."
}

$releaseExists = gh release view $Tag --repo "$Owner/$Repo" 2>$null
if ($LASTEXITCODE -ne 0) {
    gh release create $Tag --repo "$Owner/$Repo" --title $Tag --notes "StoryBoy Android build $Tag"
}

Copy-Item -Path $ApkPath -Destination storyboy-debug.apk -Force
try {
    gh release upload $Tag storyboy-debug.apk $ManifestPath --repo "$Owner/$Repo" --clobber
}
finally {
    Remove-Item storyboy-debug.apk -Force -ErrorAction SilentlyContinue
}
