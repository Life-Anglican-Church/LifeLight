import {fixtureVisual} from '../web/fixture-visual.js';
import fs from 'node:fs';
import path from 'node:path';
import {createHash,randomBytes,randomUUID} from 'node:crypto';
import {commonControls,controlChanges,readControl} from '../web/fixture-controls.js';
import {colourChanges,colourTargets,readColour} from '../web/colour-model.js';

const secret = () => randomBytes(32).toString('hex');
const digest = value => createHash('sha256').update(value).digest('hex');
const isSecret = value => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value);
const noAccess = () => Object.assign(Error('Device approval required'), {status:401});
const roles = new Set(['dimmer','pan','tilt']);
const physical = (show,id) => show.fixtures.some(f => f.id === id && f.universe === 1);
const percent = (value,max) => Math.round(value * 100 / max);

export class MobileDevices {
 constructor(dir) {
  this.file = path.join(dir,'mobile-devices.json');
  this.devices = [];
  this.pairing = null;
  try {
   if (!fs.existsSync(this.file)) return;
   const devices = JSON.parse(fs.readFileSync(this.file,'utf8'));
   if (!Array.isArray(devices) || devices.some(d=>!d||typeof d.id!=='string'||typeof d.name!=='string'||!isSecret(d.hash)))
    throw Error('Invalid approvals');
   this.devices = devices;
  } catch {
   this.invalid = true;
  }
 }
 list() {
  if (this.invalid) throw Error('Paired device storage could not be read');
  return this.devices.map(({id,name,pairedAt}) => ({id,name,pairedAt}));
 }
 save(devices) {
  const pending = this.file+'.new';
  fs.writeFileSync(pending,JSON.stringify(devices),{mode:0o600});
  fs.renameSync(pending,this.file);
  this.devices = devices;
 }
 issue(url,fingerprint) {
  if (this.invalid) throw Error('Paired device storage could not be read');
  if (!url || !fingerprint) throw Error('Start the LAN listener before pairing');
  const code = secret();
  const expires = Date.now()+120000;
  this.pairing = {hash:digest(code),expires};
  return {type:'lifeac-pair-v1',url,fingerprint,code,expires};
 }
 pair(code,name) {
  if (!isSecret(code) || !this.pairing || Date.now() >= this.pairing.expires
   || this.pairing.hash !== digest(code)) throw noAccess();
  if (typeof name !== 'string' || !name.trim() || name.length > 60 || /[\x00-\x1f\x7f]/.test(name))
   throw Error('Enter a device name (up to 60 characters)');
  if (this.devices.length >= 100) throw Error('Remove an unused device before pairing another');
  const token = secret();
  const device = {id:randomUUID(),name:name.trim(),pairedAt:new Date().toISOString(),hash:digest(token)};
  this.save([...this.devices,device]);
  this.pairing = null;
  return {id:device.id,name:device.name,token};
 }
 authenticate(header) {
  const token = /^Bearer ([a-f0-9]{64})$/.exec(header??'')?.[1];
  const hash = token && digest(token);
  const device = hash && this.devices.find(d => d.hash === hash);
  if (!device) throw noAccess();
  return device;
 }
 remove(id) {
  if (!this.devices.some(d => d.id === id)) throw Error('Device not found');
  this.save(this.devices.filter(d => d.id !== id));
 }
}

export function mobileSnapshot(show,engine) {
 const s=engine.state();
 const held=new Set([...s.overrides.map(o=>o.fixtureId),...Object.keys(s.brightness)]);
 return {
  revision:show.revision, folders:show.folders,
  scenes:show.scenes.map(({id,name,page,unsupported})=>({id,name,folder:page,playable:!unsupported})),
  fixtures:show.fixtures.filter(f=>f.universe===1).map(f=>{
   const controls=commonControls(show,[f.id]).filter(c=>roles.has(c.role));
   const targets=colourTargets(show,[f.id]);
   const levels=Object.fromEntries(controls.map(c=>[c.key,percent(readControl(c,s.captureFrame,s.brightness).value,c.max)]));
   return {id:f.id,name:f.name,group:f.group??'',position:f.position??null,held:held.has(f.id),
    levels,colour:targets.length?readColour(targets,s.captureFrame).rgb:null,
    ...(f.footprint?{footprint:f.footprint,footprintOutput:fixtureVisual(show,f,s.frame).rgb}:{})};
  }),
  sceneId:s.sceneId,output:s.output,preview:s.preview,dark:s.dark
 };
}

export function mobileChanges(show,engine,c) {
 if (!Array.isArray(c.ids) || !c.ids.length || c.ids.length>512 || new Set(c.ids).size!==c.ids.length
   || c.ids.some(id=>!physical(show,id))) throw Error('Select configured fixtures');
 if (c.op==='release') return {op:'releaseOverrides',fixtureIds:c.ids};
 if (c.op==='colour') {
  const targets=colourTargets(show,c.ids);
  if (!targets.length) throw Error('Selection does not support RGB colour');
  return {op:'manualBatch',changes:colourChanges(targets,c.rgb,engine.captureFrame(),Object.fromEntries(engine.brightness))};
 }
 const control=commonControls(show,c.ids).find(x=>x.key===c.key&&roles.has(x.role));
 if (!control || !Number.isInteger(c.value) || c.value<0 || c.value>100) throw Error('Invalid fixture control');
 return {op:'manualBatch',changes:controlChanges(control,Math.round(c.value/100*control.max))};
}
