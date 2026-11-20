import {app,BrowserWindow,powerSaveBlocker,session} from 'electron';import path from 'node:path';import {fileURLToPath} from 'node:url';
const root=path.resolve(fileURLToPath(new URL('..',import.meta.url)));let host,window,quitting=false,blocker;
if(!app.requestSingleInstanceLock()){app.quit();}else{
 app.on('second-instance',()=>{window?.show();window?.focus();});
 await app.whenReady();const {createApp}=await import(new URL('../src/server.mjs',import.meta.url));host=await createApp({root});
 const allowed=new URL(host.url).origin;window=new BrowserWindow({width:1440,height:960,minWidth:900,minHeight:650,title:'LifeAC Lighting',icon:path.join(root,'web/assets/lifeac.ico'),backgroundColor:'#123b48',autoHideMenuBar:true,webPreferences:{nodeIntegration:false,contextIsolation:true,sandbox:true,backgroundThrottling:false}});
 window.webContents.setWindowOpenHandler(()=>({action:'deny'}));window.webContents.on('will-navigate',(event,url)=>{if(new URL(url).origin!==allowed)event.preventDefault();});
 session.defaultSession.setPermissionRequestHandler((_wc,_p,callback)=>callback(false));
 window.on('close',event=>{if(!quitting){event.preventDefault();window.hide();}});
 host.engine.on('change',()=>{if(host.engine.output==='active'&&blocker==null)blocker=powerSaveBlocker.start('prevent-app-suspension');else if(host.engine.output!=='active'&&blocker!=null){powerSaveBlocker.stop(blocker);blocker=null;}});
 await window.loadURL(host.url+'#'+host.bootstrap);console.log('READY '+JSON.stringify({url:host.url}));
 async function stop(){if(quitting)return;quitting=true;await host.close();app.quit();}
 process.stdin.resume();process.stdin.on('data',b=>{for(const command of b.toString().trim().split(/\r?\n/)){if(command==='open'){window.show();window.focus();}if(command==='stop')stop();}});process.stdin.on('end',stop);app.on('before-quit',event=>{if(!quitting){event.preventDefault();stop();}});
}
