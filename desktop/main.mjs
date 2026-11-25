import {app,BrowserWindow,powerSaveBlocker,session,dialog} from 'electron';import path from 'node:path';import net from 'node:net';import {fileURLToPath} from 'node:url';
const root=path.resolve(fileURLToPath(new URL('..',import.meta.url)));let host,window,quitting=false,blocker,control,parentTimer;
function open(){if(window&&!window.isDestroyed()){if(window.isMinimized())window.restore();window.show();window.focus();}}
async function stop(){if(quitting)return;quitting=true;clearInterval(parentTimer);control?.close();window?.destroy();try{// The independent host owns USB/MIDI and survives the client.
}finally{if(blocker!=null)powerSaveBlocker.stop(blocker);app.quit();}}
async function start(){
 window=new BrowserWindow({width:1440,height:960,minWidth:900,minHeight:650,title:'LifeAC Lighting',icon:path.join(root,'web/assets/lifeac.ico'),backgroundColor:'#123b48',autoHideMenuBar:true,webPreferences:{nodeIntegration:false,contextIsolation:true,sandbox:true,backgroundThrottling:false}});
 window.webContents.setWindowOpenHandler(()=>({action:'deny'}));session.defaultSession.setPermissionRequestHandler((_wc,_p,callback)=>callback(false));
 window.on('close',event=>{if(!quitting){event.preventDefault();window.hide();}});
 window.setTitle('LifeAC Lighting — opening…');
 const endpoint=process.env.LIFEAC_HOST_URL;if(!endpoint)throw Error('Open LifeAC Lighting using its Windows launcher.');const target=new URL(endpoint);if(!['http:','https:'].includes(target.protocol)||target.protocol==='http:'&&target.hostname!=='127.0.0.1')throw Error('Use HTTPS for a remote lighting host');host={url:target.origin+'/',bootstrap:target.hash.slice(1)};
 const allowed=new URL(host.url).origin;window.webContents.on('will-navigate',(event,url)=>{if(new URL(url).origin!==allowed)event.preventDefault();});
  await window.loadURL(host.url+(host.bootstrap?'#'+host.bootstrap:''));console.log('READY '+JSON.stringify({url:host.url}));
}
if(!app.requestSingleInstanceLock()){app.quit();}else{
 app.on('certificate-error',(event,contents,url,error,certificate,callback)=>{if(contents!==window?.webContents)return callback(false);event.preventDefault();dialog.showMessageBox(window,{type:'question',title:'Lighting host certificate',message:'Trust this lighting PC?',detail:new URL(url).origin+'\nCertificate: '+certificate.fingerprint+'\nCompare with the fingerprint shown in host Settings.',buttons:['Cancel','Trust for this session'],defaultId:0,cancelId:0}).then(result=>callback(result.response===1));});app.on('second-instance',open);app.on('activate',open);
 // Do not await app.whenReady() at module scope: Electron waits for its ESM
 // entry module to finish evaluating before it can emit the ready event.
 app.whenReady().then(start).catch(async error=>{console.error(error.stack??String(error));dialog.showErrorBox('LifeAC Lighting could not open',error.message+'\nPlease include the startup log when reporting this problem.');await stop();});
 // Windows GUI executables do not reliably inherit stdin. Use an authenticated
 // local named pipe; stdin EOF must never make the desktop disappear at launch.
 const pipe=process.env.LIFEAC_DESKTOP_PIPE,key=process.env.LIFEAC_DESKTOP_KEY;
 if(pipe&&key&&pipe.startsWith('\\\\.\\pipe\\LifeACLightingDesktop-')){control=net.createServer(socket=>{let input='';socket.setTimeout(3000,()=>socket.destroy());socket.on('error',()=>{});socket.on('data',chunk=>{input+=chunk.toString();if(input.length>4096)return socket.destroy();if(!input.includes('\n'))return;const [token,command,targetUrl]=input.trim().split(' ');socket.end();if(token!==key)return;if(command==='reload'&&targetUrl){try{const next=new URL(targetUrl);if(next.origin===new URL(host.url).origin)window.loadURL(next.href);}catch{}}if(command==='open')open();if(command==='stop')stop();});});control.on('error',error=>console.error('Desktop control: '+error.message));control.listen(pipe);}
 const parent=Number(process.env.LIFEAC_PARENT_PID);if(Number.isInteger(parent)&&parent>0){parentTimer=setInterval(()=>{try{process.kill(parent,0);}catch(error){if(error.code==='ESRCH')stop();}},2000);parentTimer.unref();}
 app.on('before-quit',event=>{if(!quitting){event.preventDefault();stop();}});
}
