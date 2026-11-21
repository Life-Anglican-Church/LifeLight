import {spawn} from 'node:child_process';import readline from 'node:readline';import path from 'node:path';
export function matches(mapping,event){return mapping.type===event.type&&Number(mapping.channel)===event.channel&&Number(mapping.key)===event.key&&(event.type!=='note'||event.value>0)&&(mapping.value==null?event.type==='program'||event.value>0:Number(mapping.value)===event.value);}
export function chooseInput(devices,name){if(name){const saved=devices.find(d=>d.name===name);if(saved)return saved;}const loops=devices.filter(d=>/loopmidi/i.test(d.name));return loops.length===1?loops[0]:!name&&devices.length===1?devices[0]:null;}
export class Midi{
 constructor(root,onEvent,onStatus,{spawnWorker=spawn,retryMs=3000}={}){Object.assign(this,{exe:path.join(root,'bin','LifeAC-MidiWorker.exe'),onEvent,onStatus,spawnWorker,child:null,connected:false,name:'',enabled:false,busy:false,generation:0,deviceId:null,stats:{received:0,lastMessage:null,lastError:null,status:'Not started'}});this.timer=setInterval(()=>this.connect(),retryMs);this.timer.unref();}
 status(connected,detail){this.connected=connected;this.stats.status=detail;this.onStatus(connected,detail);}
 async devices(){return new Promise((resolve,reject)=>{const c=this.spawnWorker(this.exe,['--list'],{windowsHide:true,stdio:['ignore','pipe','pipe']});let data='';const timeout=setTimeout(()=>{c.kill();reject(Error('MIDI discovery timed out'));},5000);c.stdout.on('data',x=>data+=x);c.on('error',e=>{clearTimeout(timeout);reject(e);});c.on('exit',code=>{clearTimeout(timeout);try{if(code)throw Error('MIDI discovery failed ('+code+')');resolve(data.trim()?data.trim().split(/\r?\n/).map(JSON.parse):[]);}catch(e){reject(e);}});});}
 configure(name,enabled=true){this.generation++;this.name=name??'';this.enabled=enabled;this.child?.stdin.end();this.child=null;this.deviceId=null;this.stats.lastError=null;this.status(false,enabled?'Looking for MIDI input on this host':'MIDI disabled');this.connect();}
 async connect(){if(!this.enabled||this.busy)return;this.busy=true;const generation=this.generation;try{
  const devices=await this.devices();if(generation!==this.generation)return;const device=chooseInput(devices,this.name);
  if(this.child&&(!device||device.id!==this.deviceId)){this.child.stdin.end();this.child=null;this.deviceId=null;this.status(false,'MIDI input changed; reconnecting');}
  if(this.child)return;
  if(!device){this.status(false,this.name?'Selected MIDI input is not available on this host':devices.length?'Choose a MIDI input in Settings':'No MIDI inputs on this host; keep loopMIDI running in this Windows login');return;}
  this.name=device.name;this.deviceId=device.id;const child=this.spawnWorker(this.exe,[String(device.id)],{windowsHide:true,stdio:['pipe','pipe','pipe']});this.child=child;
  const ready=setTimeout(()=>{if(this.child===child){this.stats.lastError='MIDI port startup timed out';this.status(false,this.stats.lastError);child.kill();}},5000);child.stdin.on('error',()=>{});child.stderr.on('data',x=>{this.stats.lastError='MIDI worker: '+x.toString().slice(0,160);});
  const stopped=(code)=>{clearTimeout(ready);if(this.child===child){this.child=null;this.status(false,this.stats.lastError??'MIDI listener stopped ('+code+'); retrying');}};
  child.on('error',e=>{this.stats.lastError='MIDI worker failed: '+e.code;stopped(e.code);});child.on('exit',stopped);
  readline.createInterface({input:child.stdout}).on('line',line=>{if(this.child!==child)return;try{const event=JSON.parse(line);
   if(event.kind==='ready'){clearTimeout(ready);this.stats.lastError=null;this.status(true,'Listening on host: '+device.name);}
   else if(event.kind==='error'){this.stats.lastError=event.stage==='closed'?'MIDI input closed; reconnecting':'Windows MIDI '+event.stage+' failed: '+event.code+(event.message?' · '+event.message:'');this.status(false,this.stats.lastError);}
   else if(event.kind==='health'){this.stats.rawReceived=event.received;}
   else if(event.kind==='event'&&this.connected){this.stats.received++;this.stats.lastMessage={type:event.type,channel:event.channel,key:event.key,value:event.value,status:event.status,at:Date.now()};if(['note','cc','program'].includes(event.type))this.onEvent({...event,at:Date.now(),source:device.name});}
  }catch(e){this.stats.lastError='Invalid MIDI worker message: '+e.message;}});
 }catch(e){this.stats.lastError=e.message;this.status(false,e.message);}finally{this.busy=false;}}
 close(){this.enabled=false;this.generation++;clearInterval(this.timer);this.child?.stdin.end();this.child=null;this.connected=false;}
}
