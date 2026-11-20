import {inflateSync} from 'node:zlib';
import {createHash} from 'node:crypto';
import {migrateShow} from './model.mjs';
export const hash=b=>createHash('sha256').update(b).digest('hex');
const list=(n,name)=>n?.children.filter(c=>c.name===name)??[];
const one=(n,name)=>list(n,name)[0];
const num=(n,key,d=0)=>Number.isFinite(Number(n?.attrs[key]))?Number(n.attrs[key]):d;
const text=(n,key,d='')=>n?.attrs[key]??d;
function entity(s){return s.replace(/&(#x[\da-f]+|#\d+|amp|lt|gt|quot|apos);/gi,(_,e)=>e[0]==='#'?String.fromCodePoint(e[1].toLowerCase()==='x'?parseInt(e.slice(2),16):Number(e.slice(1))):({amp:'&',lt:'<',gt:'>',quot:'"',apos:"'"}[e]));}
export function xml(source){
 if(source.length>16_000_000||/<!DOCTYPE|<!ENTITY|<!\[CDATA/i.test(source))throw Error('Unsupported or oversized XML');
 const root={name:'document',attrs:{},children:[]},stack=[root];let at=0;
 for(const m of source.matchAll(/<!--[\s\S]*?-->|<\?[\s\S]*?\?>|<[^>]*>/g)){
  if(source.slice(at,m.index).trim())throw Error('Unexpected XML text');at=m.index+m[0].length;const tag=m[0];if(tag.startsWith('<?')||tag.startsWith('<!--'))continue;
  if(tag.startsWith('</')){if(stack.length===1||stack.pop().name!==tag.slice(2,-1).trim())throw Error('Mismatched XML');continue;}
  const match=/^<([\w:-]+)([\s\S]*?)(\/?)>$/.exec(tag);if(!match)throw Error('Invalid XML tag');const attrs=Object.create(null);let cursor=0;
  for(const a of match[2].matchAll(/([\w:-]+)\s*=\s*("[^"]*"|'[^']*')/g)){if(match[2].slice(cursor,a.index).trim()||Object.hasOwn(attrs,a[1]))throw Error('Invalid XML attributes');attrs[a[1]]=entity(a[2].slice(1,-1));cursor=a.index+a[0].length;}
  if(match[2].slice(cursor).trim())throw Error('Invalid XML attribute');const node={name:match[1],attrs,children:[]};stack.at(-1).children.push(node);if(!match[3])stack.push(node);if(stack.length>100)throw Error('XML too deep');
 }
 if(stack.length!==1||source.slice(at).trim()||root.children.length!==1)throw Error('Incomplete XML');return root.children[0];
}
export function unpack(b){if(b.length<10)throw Error('Short packed data');const size=b.readUInt32BE();if(size>16_000_000)throw Error('Packed size limit');const out=inflateSync(b.subarray(4),{maxOutputLength:16_000_000});if(out.length!==size)throw Error('Packed length mismatch');return out;}
const roles={1:'pan',2:'tilt',7:'dimmer',14:'zoom',15:'shutter',25:'red',26:'green',27:'blue',31:'white',32:'amber'};
function decode(n,fixtures,inheritedIds=''){
 const hex=text(n,'SLvl');if(!/^(?:[a-f\d]{2})+$/i.test(hex))throw Error('Invalid scene data');const data=unpack(Buffer.from(hex,'hex'));const ids=(text(n,'SIds')||inheritedIds).split('|').map(Number);const values=Array(512).fill(0),used=new Set();let k=0,v=0;
 while(k<ids.length){const id=ids[k++];let count=ids[k++],slot;if(id===-1){if(count!==0)throw Error('Multiple universes unverified');slot=ids[k++];count=1;}else{const f=fixtures.find(f=>f.sourceId===id);if(!f||f.universe!==1||count!==f.width)throw Error('Fixture width mismatch');slot=f.address-1;}
  if(!Number.isInteger(slot)||slot<0||slot+count>512||v+count>data.length)throw Error('Invalid slot mapping');for(let j=0;j<count;j++){if(used.has(slot+j))throw Error('Duplicate scene slot');used.add(slot+j);values[slot+j]=data[v++];}}
 if(v!==data.length)throw Error('Scene length mismatch');return values;
}
export function importShow(buffer){
 const source=buffer[0]===60?buffer:unpack(buffer);const root=xml(source.toString('utf8'));if(root.name!=='LPFile')throw Error('Not a ProDMX show');
 const profiles=[],fixtures=[],scenes=[],standalone=[],warnings=[];let profile;
 for(const n of list(one(root,'LPPatch'),'LPFixtures').flatMap(g=>g.children)){
  if(n.name==='LPLibrary'){
   const modes=list(one(one(n,'LPProfile'),'LPModes'),'LPMode');if(modes.length!==1)throw Error('Ambiguous profile mode');const mode=modes[0],props=one(mode,'LPProperties');
   profile={id:`profile-${profiles.length+1}`,name:text(n,'LPName').replaceAll('\\','/').split('/').at(-1).replace(/\.cp\d$/,''),mode:text(props,'LPName','Imported mode'),channels:list(mode,'LPChannel').map((c,i)=>({offset:i,name:text(c,'LPChannelName',`Channel ${i+1}`),role:/undef|internal preset|reset|macro|mode/i.test(text(c,'LPChannelName'))?'protected':roles[num(c,'LPChannelType')]??'protected',fine:num(c,'LPChannelLSB')===1,coarse:num(c,'LPChannelMSB')===1,presets:list(one(c,'LPPresets'),'LPPreset').map(p=>({name:text(p,'LPPresetName'),min:num(p,'LPPresetDmxStart'),max:num(p,'LPPresetDmxEnd'),value:num(p,'LPPresetDmxDefault')}))}))};
   if(profile.channels.length!==num(mode,'LPNbChannel')||!profile.channels.length)throw Error('Invalid channel profile');profiles.push(profile);
  }else if(n.name==='LPFixture'){
   if(!profile)throw Error('Missing profile');fixtures.push({id:`fixture-${num(n,'LPId')}`,sourceId:num(n,'LPId'),name:text(n,'LPName'),profileId:profile.id,address:num(n,'LPAddress')+1,universe:num(n,'LPUniverse')+1,width:profile.channels.length,group:'',position:null});
  }
 }
 validatePatch(fixtures,profiles);
 for(const page of list(one(root,'LPPages'),'LPPage'))for(const scene of list(one(page,'LPScenes'),'LPScene')){
  const id=`scene-${scenes.length+1}`;let reason=null;const masks=list(one(scene,'LPActivUniverses'),'LPActivUniverse');const mask=text(masks.find(n=>num(n,'LPUniverse')===0),'LPActivBuffer');
  const stepNodes=list(one(scene,'LPSteps'),'LPStep'),inheritedIds=text(one(stepNodes[0],'SData'),'SIds');
  const steps=stepNodes.map(step=>{try{return {values:decode(one(step,'SData'),fixtures,inheritedIds),fadeMs:num(step,'LPFade'),waitMs:num(step,'LPWait')};}catch(e){reason=e.message;return null;}}).filter(Boolean);
  if(!steps.length)reason='Empty scene';if(mask.length!==512||/[^01]/.test(mask))reason='Invalid active-channel mask';
  const fx=(function has(n){return n.children.some(c=>c.name==='LPEffect'||has(c));})(scene);if(fx)reason='Imported procedural effect needs conversion';
  const midi=text(one(scene,'LPMainTrigger'),'LPMidiTrigger');
  scenes.push({id,name:text(scene,'LPName'),page:text(page,'LPName','Scenes'),fadeMs:num(scene,'LPFadeIn',1000),steps,loop:steps.length>1,effects:[],mask:mask.split('').map(x=>x==='1'),unsupported:reason,source:'Imported ProDMX project',midiSource:midi});
  list(one(scene,'LPSaDatas'),'LPSaData').forEach((s,i)=>{if(num(s,'LPMemory')!==0)standalone.push({sceneId:id,sceneName:text(scene,'LPName'),deviceIndex:i,button:text(one(s,'LPStandAloneTrigger'),'LPTButton','Unassigned'),page:text(one(s,'LPStandAloneTrigger'),'LPTPage','0'),source:'Imported ProDMX project',verified:false});});
 }
 const device=one(one(root,'LPDevicesConfig'),'LPDevice');warnings.push('Fixture identities and addresses come from the show, not DMX discovery.','Imported standalone assignments are not a live device-memory snapshot.','Only universe 1 output is enabled until independent port routing is commissioned.','Protected channels are held when scenes change. Review their mappings before commissioning.');
 const show={schema:1,revision:1,name:'LifeAC church lighting',profiles,fixtures,scenes,standalone,importedAt:new Date().toISOString(),sourceHash:hash(buffer),savedDevice:{model:text(device,'LPDevRef'),firmware:text(device,'LPFirmware'),universes:num(device,'LPUniverses')},migration:{supported:scenes.filter(s=>!s.unsupported).length,total:scenes.length,warnings},midiMappings:[]};migrateShow(show);return show;
}
export function validatePatch(fixtures,profiles){const used=new Set(),ids=new Set();for(const f of fixtures){const p=profiles.find(p=>p.id===f.profileId);if(!p||f.width!==p.channels.length||!Number.isInteger(f.address)||f.address<1||f.address+f.width>513||!Number.isInteger(f.universe)||f.universe<1||f.universe>16||ids.has(f.id))throw Error('Invalid fixture patch');ids.add(f.id);for(let i=0;i<f.width;i++){const key=`${f.universe}:${f.address+i}`;if(used.has(key))throw Error(`Address overlap at universe ${f.universe}, address ${f.address+i}`);used.add(key);}}return true;}
export function suggestAddress(show,profileId,quantity=1,universe=1){const p=show.profiles.find(p=>p.id===profileId);if(!p||!Number.isInteger(quantity)||quantity<1||quantity>32)throw Error('Choose a profile and quantity 1–32');for(let start=1;start+p.channels.length*quantity<=513;start++){const end=start+p.channels.length*quantity;if(!show.fixtures.some(f=>f.universe===universe&&f.address<end&&f.address+f.width>start))return start;}throw Error('No contiguous space; choose fewer fixtures');}
export function importProfile(buffer,name='Imported profile'){
 let profile;const plain=buffer[0]===60||buffer[0]===123?buffer:unpack(buffer);
 if(plain[0]===123)profile=JSON.parse(plain.toString('utf8'));else {const source=plain.toString('utf8').replace(/^\s*<\?xml[^>]*\?>/,'');const node=xml(source);const wrapped=node.name==='LPProfile'?`<LPFile><LPPatch><LPFixtures><LPLibrary LPName="Imported profile">${source}</LPLibrary></LPFixtures></LPPatch></LPFile>`:source;const imported=importShow(Buffer.from(wrapped));if(imported.profiles.length!==1)throw Error('Import one single-mode profile at a time');profile=imported.profiles[0];profile.name=name.replace(/\.[^.]+$/,'');}
 profile.id='imported-profile';validateProfiles([profile]);return profile;
}
export function validateProfiles(profiles){const ids=new Set();if(!Array.isArray(profiles)||profiles.length>500)throw Error('Profile limit');for(const p of profiles){if(typeof p.id!=='string'||ids.has(p.id)||typeof p.name!=='string'||!p.name.trim()||p.name.length>160||typeof p.mode!=='string'||!p.mode.trim()||!Array.isArray(p.channels)||p.channels.length<1||p.channels.length>512)throw Error('Enter a profile name, exact mode and channel chart');ids.add(p.id);p.channels.forEach((c,i)=>{if(c.offset!==i||typeof c.name!=='string'||c.name.length>160||!['dimmer','red','green','blue','white','amber','pan','tilt','zoom','shutter','protected'].includes(c.role)||typeof c.coarse!=='boolean'||typeof c.fine!=='boolean')throw Error('Invalid channel definition');if(c.coarse&&(!p.channels[i+1]?.fine||p.channels[i+1].role!==c.role)||c.fine&&(!p.channels[i-1]?.coarse||p.channels[i-1].role!==c.role))throw Error('16-bit coarse and fine channels must be adjacent and paired');});}return true;}
