import fs from 'node:fs/promises';import path from 'node:path';import {createHash} from 'node:crypto';
// Folder access is asynchronous so an unavailable network share cannot stall DMX.
export class FolderFailover{
 constructor({folder,role,getShow,getScene,applyShow,onStatus=()=>{}}){Object.assign(this,{folder,role,getShow,getScene,applyShow,onStatus});this.lastHash=null;this.lastSave=0;this.primarySeen=0;this.ready=false;this.busy=false;}
 async atomic(name,data){const file=path.join(this.folder,name),tmp=file+'.'+process.pid+'.tmp';await fs.writeFile(tmp,JSON.stringify(data));await fs.rename(tmp,file);}
 async tick(force=false){if(this.busy||!this.folder)return;this.busy=true;try{if(this.role==='primary'){
  await fs.mkdir(this.folder,{recursive:true});const show=this.getShow(),hash=createHash('sha256').update(JSON.stringify(show)).digest('hex');if(hash!==this.lastHash&&(force||Date.now()-this.lastSave>=3600000)){await this.atomic('LifeAC-show.json',{schema:1,hash,show});this.lastHash=hash;this.lastSave=Date.now();}
  const playback=this.getScene();await this.atomic('LifeAC-primary.json',{schema:1,at:Date.now(),...(typeof playback==='string'||playback===null?{sceneId:playback}:playback),hash:this.lastHash});this.ready=true;
 }else if(this.role==='backup'){
  const heartbeat=JSON.parse(await fs.readFile(path.join(this.folder,'LifeAC-primary.json'),'utf8'));if(heartbeat.schema!==1||!Number.isFinite(heartbeat.at))throw Error('Invalid primary heartbeat');this.primarySeen=heartbeat.at;this.sceneId=heartbeat.sceneId;this.dark=heartbeat.dark===true;this.serviceEnabled=heartbeat.serviceEnabled===true;
  if(heartbeat.hash&&heartbeat.hash!==this.lastHash){const data=JSON.parse(await fs.readFile(path.join(this.folder,'LifeAC-show.json'),'utf8')),hash=createHash('sha256').update(JSON.stringify(data.show)).digest('hex');if(data.schema!==1||data.hash!==hash||heartbeat.hash!==hash)throw Error('Shared show checksum mismatch');if(await this.applyShow(data.show)===false)return;this.lastHash=hash;this.ready=true;}
 }this.onStatus(null);}catch(e){this.onStatus(e.message);}finally{this.busy=false;}}
 canTakeover(now=Date.now()){return this.role!=='backup'||this.ready&&this.primarySeen>0&&now-this.primarySeen>12000;}
}
