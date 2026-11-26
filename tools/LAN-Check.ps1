$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -TypeDefinition @'
using System;using System.IO;using System.Net.Sockets;using System.Net.Security;using System.Text;using System.Security.Authentication;
public static class LifeACLanProbe {
 public static object Check(string address,int port) {
  string stage="TCP";bool connected=false;try{using(var client=new TcpClient()){var pending=client.BeginConnect(address,port,null,null);using(pending.AsyncWaitHandle){if(!pending.AsyncWaitHandle.WaitOne(2000))throw new TimeoutException("TCP connection timed out");client.EndConnect(pending);}connected=true;using(var stream=client.GetStream()){stream.ReadTimeout=4000;stream.WriteTimeout=4000;stage="TLS";using(var tls=new SslStream(stream,false,(sender,cert,chain,errors)=>true)){tls.ReadTimeout=4000;tls.WriteTimeout=4000;tls.AuthenticateAsClient(address,null,SslProtocols.Tls12,false);stage="HTTPS";byte[] request=Encoding.ASCII.GetBytes("GET /api/hello HTTP/1.1\r\nHost: "+address+":"+port+"\r\nConnection: close\r\n\r\n");tls.Write(request);byte[] bytes=new byte[8192];int count=tls.Read(bytes,0,bytes.Length);string response=Encoding.UTF8.GetString(bytes,0,count);return new{address,port,connected,stage,response};}}}}catch(Exception e){return new{address,port,connected,stage,error=e.GetType().Name+": "+e.Message};}
 }
}
'@
function Read-Json($file) { try { Get-Content -LiteralPath $file -Raw | ConvertFrom-Json } catch { $null } }
function Capture($action) { try { @(& $action) } catch { @(@{error=$_.Exception.Message}) } }
$data = Join-Path $env:LOCALAPPDATA 'LifeAC-Lighting'
$settings = Read-Json (Join-Path $data 'settings.json')
$installation = Read-Json (Join-Path $data 'install.json')
$role = Read-Json (Join-Path $data 'role.json')
$report = [ordered]@{schema=1;createdUtc=[DateTime]::UtcNow.ToString('o');purpose='Read-only LAN troubleshooting; no DMX, MIDI, firewall or application changes';version=$installation.version;role=$role.role}
$report.settings = @{lanEnabled=$settings.lanEnabled;host=$settings.host;port=$settings.port;certificateFingerprint=$settings.fingerprint}
$report.adapters = Capture { Get-NetIPAddress -AddressFamily IPv4 | Select-Object IPAddress,PrefixLength,InterfaceIndex,AddressState }
$report.routes = Capture { Get-NetRoute -AddressFamily IPv4 | Select-Object DestinationPrefix,NextHop,InterfaceIndex,RouteMetric }
$report.profiles = Capture { Get-NetConnectionProfile | Select-Object InterfaceIndex,NetworkCategory,IPv4Connectivity }
$report.listeners = Capture { Get-NetTCPConnection -State Listen | Where-Object {$_.LocalPort -in @(47831,47832)} | ForEach-Object { $process=Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue; @{address=$_.LocalAddress;port=$_.LocalPort;pid=$_.OwningProcess;process=$process.ProcessName;path=$process.Path} } }
$report.udp = Capture { Get-NetUDPEndpoint -ErrorAction SilentlyContinue | Where-Object {$_.LocalPort -eq 47833} | Select-Object LocalAddress,LocalPort,OwningProcess }
$report.firewallProfiles = Capture { Get-NetFirewallProfile -PolicyStore ActiveStore | Select-Object Name,Enabled,DefaultInboundAction,AllowLocalFirewallRules,AllowInboundRules }
$report.netshRules = Capture { foreach($protocol in @('TCP','UDP')) { & (Join-Path $env:WINDIR 'System32\netsh.exe') advfirewall firewall show rule ('name=LifeAC Lighting LAN ' + $protocol) verbose } }
$report.rules = Capture { Get-NetFirewallRule -PolicyStore ActiveStore -DisplayName 'LifeAC Lighting LAN *' | ForEach-Object { @{name=$_.DisplayName;enabled=$_.Enabled;action=[string]$_.Action;profile=[string]$_.Profile;status=$_.PrimaryStatus;program=@($_ | Get-NetFirewallApplicationFilter | Select-Object Program);ports=@($_ | Get-NetFirewallPortFilter | Select-Object Protocol,LocalPort);scope=@($_ | Get-NetFirewallAddressFilter | Select-Object RemoteAddress)} } }
$targets = @('127.0.0.1','192.168.1.153') + @($settings.host) + @($report.adapters | ForEach-Object {$_.IPAddress})
$targets = @($targets | Where-Object {$_ -match '^\d{1,3}(\.\d{1,3}){3}$' -and $_ -notlike '169.254.*'} | Select-Object -Unique -First 8)
$report.probes = @($targets | ForEach-Object { [LifeACLanProbe]::Check($_,47832) })
$report.lanEvents = Capture { Get-Content -LiteralPath (Join-Path $data 'logs\lighting.jsonl') -Tail 500 | ForEach-Object { try { $row=$_ | ConvertFrom-Json; if($row.op -in @('LAN','discovery','hostStart','hostStop')){$row} } catch {} } }
$report.startup = Capture { Get-Content -LiteralPath (Join-Path $data 'startup.log') -Tail 60 }
$json = $report | ConvertTo-Json -Depth 12
# No account store, tokens, private certificate or password settings are read.
$json = $json.Replace($env:USERPROFILE,'[UserProfile]').Replace($env:USERPROFILE.Replace('\','\\'),'[UserProfile]')
if ($env:USERNAME.Length -gt 2) { $json=$json.Replace($env:USERNAME,'[User]') }
if ($env:COMPUTERNAME.Length -gt 2) { $json=$json.Replace($env:COMPUTERNAME,'[Computer]') }
$json = [regex]::Replace($json,'github_pat_[A-Za-z0-9_]+|#[a-fA-F0-9]{64}','[credential]')
$json = [regex]::Replace($json,'[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}','[email]')
$dialog = New-Object System.Windows.Forms.SaveFileDialog
$dialog.Title = 'Save the LAN report to send back in chat'
$dialog.Filter = 'JSON report (*.json)|*.json'
$dialog.InitialDirectory = [Environment]::GetFolderPath('Desktop')
$dialog.FileName = 'LifeAC-LAN-' + $(if($role.role){$role.role}else{'controller'}) + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.json'
if ($dialog.ShowDialog() -eq 'OK') { [IO.File]::WriteAllText($dialog.FileName,$json);[System.Windows.Forms.MessageBox]::Show('Report saved. Please attach this file in the chat. No lighting or firewall settings were changed.','LifeAC LAN check') | Out-Null }
