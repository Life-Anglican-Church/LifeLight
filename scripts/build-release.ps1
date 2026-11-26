param([string]$BuildRoot,[switch]$LightweightOnly)
$ErrorActionPreference = 'Stop'
$lightingRoot = if($BuildRoot) { $BuildRoot } else { Split-Path $PSScriptRoot }
$workspaceRoot = Split-Path $lightingRoot
$releaseRoot = Join-Path $lightingRoot 'dist\v0.8.1'
$packageRoot = Join-Path $releaseRoot 'LifeAC-Lighting'
$compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
$readCredential = Join-Path $workspaceRoot '.release-secrets\read.txt'
if (!(Test-Path -LiteralPath $readCredential)) { throw 'Supply the read-only release credential outside the source tree.' }
if (!(Test-Path -LiteralPath (Join-Path $lightingRoot 'runtime\node.exe'))) {
    $cache = Join-Path $lightingRoot '.cache'
    New-Item -ItemType Directory -Force -Path $cache,(Join-Path $lightingRoot 'runtime') | Out-Null
    $nodeArchive = Join-Path $cache 'node-v24.22.3-win-x64.zip'
    Invoke-WebRequest -UseBasicParsing 'https://nodejs.org/dist/v24.22.3/node-v24.22.3-win-x64.zip' -OutFile $nodeArchive
    $sumsFile = Join-Path $cache 'node-SHASUMS256.txt'
    Invoke-WebRequest -UseBasicParsing 'https://nodejs.org/dist/v24.22.3/SHASUMS256.txt' -OutFile $sumsFile
    $expected = ((Get-Content -LiteralPath $sumsFile | Where-Object { $_ -match ' node-v24\.22\.3-win-x64\.zip$' }) -split '\s+')[0]
    if ((Get-FileHash -LiteralPath $nodeArchive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'Official Node archive checksum mismatch.' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $nodeZip = [IO.Compression.ZipFile]::OpenRead($nodeArchive)
    try { foreach($name in @('node.exe','LICENSE')) { [IO.Compression.ZipFileExtensions]::ExtractToFile($nodeZip.GetEntry("node-v24.22.3-win-x64/$name"),(Join-Path $lightingRoot "runtime\$name"),$true) } } finally { $nodeZip.Dispose() }
}
& (Join-Path $lightingRoot 'build.cmd')
if ($LASTEXITCODE -ne 0) { throw 'Application build failed.' }
New-Item -ItemType Directory -Force -Path $releaseRoot,$packageRoot | Out-Null
& $compiler /nologo /target:winexe /platform:anycpu "/out:$releaseRoot\LifeAC-Lighting-Updater.exe" "/win32icon:$lightingRoot\web\assets\lifeac.ico" /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.Web.Extensions.dll /reference:System.IO.Compression.dll /reference:System.IO.Compression.FileSystem.dll /reference:System.Security.dll /reference:Microsoft.CSharp.dll "/resource:$readCredential,LifeAC.ReadOnlyAccess" (Join-Path $lightingRoot 'native\Deployment.cs') (Join-Path $lightingRoot 'native\Updater.cs')
if ($LASTEXITCODE -ne 0) { throw 'Updater build failed.' }
foreach($folder in @('src','web','bin','runtime','desktop-runtime')) {
    Copy-Item -LiteralPath (Join-Path $lightingRoot $folder) -Destination $packageRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $packageRoot 'desktop') | Out-Null
foreach($file in @('main.mjs','package.json','preload.cjs','connection.mjs','connection.html','connection.js','connection.css','discovery.mjs')) {
    Copy-Item -LiteralPath (Join-Path $lightingRoot "desktop\$file") -Destination (Join-Path $packageRoot 'desktop') -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $packageRoot 'inputs'),(Join-Path $packageRoot 'docs') | Out-Null
Copy-Item -LiteralPath (Join-Path $lightingRoot 'inputs\show.json'),(Join-Path $lightingRoot 'inputs\commissioning-frame.json') -Destination (Join-Path $packageRoot 'inputs')
foreach($file in @('README.md','THIRD-PARTY-NOTICES.txt','LifeAC Lighting.exe')) { Copy-Item -LiteralPath (Join-Path $lightingRoot $file) -Destination $packageRoot -Force }
foreach($file in @('COMMISSIONING.md','VALIDATION.md','VENDOR-QUESTIONS.txt','RELEASE-NOTES.md')) { Copy-Item -LiteralPath (Join-Path $lightingRoot "docs\$file") -Destination (Join-Path $packageRoot 'docs') -Force }
Copy-Item -LiteralPath (Join-Path $releaseRoot 'LifeAC-Lighting-Updater.exe') -Destination (Join-Path $packageRoot 'LifeAC Lighting Updater.exe') -Force
$archive = Join-Path $releaseRoot 'LifeAC-Lighting-win-x64.zip'
Compress-Archive -LiteralPath $packageRoot -DestinationPath $archive -Force
$archiveHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
$hashFile = Join-Path $releaseRoot 'payload-sha256.txt'
[IO.File]::WriteAllText($hashFile,$archiveHash)
if (!$LightweightOnly) {
    & $compiler /nologo /target:winexe /platform:anycpu "/out:$releaseRoot\LifeAC-Lighting-Setup-0.8.1.exe" "/win32icon:$lightingRoot\web\assets\lifeac.ico" /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.Web.Extensions.dll /reference:System.IO.Compression.dll /reference:System.IO.Compression.FileSystem.dll /reference:System.Security.dll /reference:Microsoft.CSharp.dll "/resource:$readCredential,LifeAC.ReadOnlyAccess" "/resource:$archive,LifeAC.Payload" "/resource:$hashFile,LifeAC.PayloadHash" (Join-Path $lightingRoot 'native\Deployment.cs') (Join-Path $lightingRoot 'native\Setup.cs')
    if ($LASTEXITCODE -ne 0) { throw 'Installer build failed.' }
}
$manifest = @{schema='1';version='0.8.1';package='LifeAC-Lighting-win-x64.zip';sha256=$archiveHash;entry='LifeAC Lighting.exe'}
[IO.File]::WriteAllText((Join-Path $releaseRoot 'update-manifest.json'),($manifest | ConvertTo-Json -Compress))
$sums = Get-ChildItem -LiteralPath $releaseRoot -File | Where-Object {$_.Extension -eq '.exe' -or $_.Name -in @('LifeAC-Lighting-win-x64.zip','update-manifest.json')} | ForEach-Object { ((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()) + '  ' + $_.Name }
$sums | Set-Content -LiteralPath (Join-Path $releaseRoot 'SHA256SUMS.txt') -Encoding ASCII
Write-Output 'Installer, updater, application archive and update manifest built. No installation was performed.'

