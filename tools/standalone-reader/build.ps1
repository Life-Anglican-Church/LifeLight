param([string]$BuildRoot)
$ErrorActionPreference='Stop'
$testerRoot=if($BuildRoot){$BuildRoot}else{$PSScriptRoot}
$compiler=Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
$out=Join-Path $testerRoot 'build'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$worker=Join-Path $out 'scene-reader-worker.exe'
& $compiler /nologo /target:exe /platform:x86 /reference:System.Web.Extensions.dll "/out:$worker" (Join-Path $testerRoot 'StandaloneReaderWorker.cs')
if($LASTEXITCODE -ne 0){throw 'Worker build failed'}
& $worker --self-test
if($LASTEXITCODE -ne 0){throw 'Offline checks failed'}
& $compiler /nologo /target:winexe /reference:System.Web.Extensions.dll /reference:System.Management.dll /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.IO.Compression.dll /reference:System.IO.Compression.FileSystem.dll "/resource:$worker,LifeAC.StandaloneReaderWorker" "/out:$out\LifeAC-Standalone-Tester-0.3.exe" (Join-Path $testerRoot 'StandaloneReader.cs') (Join-Path $testerRoot 'EnvironmentReport.cs')
if($LASTEXITCODE -ne 0){throw 'Tester build failed'}
Write-Output 'Built tester; offline checks passed. No hardware was accessed.'
