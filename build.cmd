@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin
set "LIFEAC_CSC=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if not exist "%LIFEAC_CSC%" set "LIFEAC_CSC=%WINDIR%\Microsoft.NET\Framework\v4.0.30319\csc.exe"
"%LIFEAC_CSC%" /nologo /target:exe /platform:x86 /optimize+ /out:bin\LifeAC-FrameWorker.exe /reference:System.Web.Extensions.dll /reference:System.IO.Compression.dll /reference:System.IO.Compression.FileSystem.dll native\VendorSdk.cs native\FrameWorker.cs
if errorlevel 1 exit /b 1
"%LIFEAC_CSC%" /nologo /target:exe /platform:anycpu /out:bin\LifeAC-Certificate.exe native\Certificate.cs
if errorlevel 1 exit /b 1
"%LIFEAC_CSC%" /nologo /target:exe /platform:anycpu /out:bin\LifeAC-MidiWorker.exe /reference:System.Web.Extensions.dll native\MidiWorker.cs
if errorlevel 1 exit /b 1
"%LIFEAC_CSC%" /nologo /target:exe /platform:x86 /out:bin\LifeAC-StandaloneWorker.exe /reference:System.Web.Extensions.dll tools\standalone-reader\StandaloneReaderWorker.cs
if errorlevel 1 exit /b 1
"%LIFEAC_CSC%" /nologo /target:winexe /platform:anycpu /out:"LifeAC Lighting.exe" /win32icon:web\assets\lifeac.ico /reference:System.Windows.Forms.dll /reference:System.Drawing.dll /reference:System.Web.Extensions.dll /reference:Microsoft.CSharp.dll native\Launcher.cs native\Discovery.cs
exit /b %errorlevel%
