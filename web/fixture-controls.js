const roles=new Set(['dimmer','red','green','blue','white','amber','pan','tilt','zoom']);
const emitters=new Set(['red','green','blue','white','amber']);
export function controlsFor(show,id){
 const f=show.fixtures.find(f=>f.id===id);if(!f)return [];
 const p=show.profiles.find(p=>p.id===f.profileId),counts={};
 const controls=p.channels.flatMap((c,offset)=>{
  if(c.fine||!roles.has(c.role))return [];
  const pair=!!c.coarse;if(pair&&!(p.channels[offset+1]?.fine&&p.channels[offset+1].role===c.role))return [];
  const ordinal=counts[c.role]=(counts[c.role]??0)+1;
  return [{key:c.role+':'+ordinal,role:c.role,label:c.role+(ordinal>1?' '+ordinal:''),fixtureId:id,offset,address:f.address-1+offset,pair,max:pair?65535:255}];
 });
 const channels=controls.filter(c=>emitters.has(c.role));
 if(!controls.some(c=>c.role==='dimmer')&&channels.length)controls.unshift({key:'dimmer:1',role:'dimmer',label:'dimmer',fixtureId:id,virtual:true,max:255,channels});
 return controls.sort((a,b)=>Number(b.role==='dimmer')-Number(a.role==='dimmer'));
}
const channelValue=(t,frame)=>(frame[t.address]??0)*(t.pair?256:1)+(t.pair?(frame[t.address+1]??0):0);
export function brightnessValue(target,frame){return Math.round(Math.max(...target.channels.map(t=>channelValue(t,frame)/t.max))*255);}
// Only known emitter channels participate; memory retains colour through zero.
export function setBrightness(target,value,frame,memory=new Map(),mask){
 let levels=target.channels.map(t=>channelValue(t,frame)/t.max),peak=Math.max(...levels);
 if(peak>0){levels=levels.map(v=>v/peak);memory.set(target.fixtureId,levels);}
 else levels=memory.get(target.fixtureId);
 if(levels?.length!==target.channels.length)levels=null;
 if(!levels){const white=target.channels.some(t=>t.role==='white'),rgb=target.channels.some(t=>['red','green','blue'].includes(t.role));levels=target.channels.map(t=>white?Number(t.role==='white'):rgb?Number(['red','green','blue'].includes(t.role)):1);}
 target.channels.forEach((t,n)=>{const v=Math.round(levels[n]*value/255*t.max);frame[t.address]=t.pair?v>>8:v;if(mask)mask[t.address]=true;if(t.pair){frame[t.address+1]=v&255;if(mask)mask[t.address+1]=true;}});
}
export function commonControls(show,ids){const groups=[...new Set(ids)].map(id=>controlsFor(show,id));if(!groups.length)return [];return groups[0].flatMap(c=>{const targets=groups.map(g=>g.find(x=>x.key===c.key));if(targets.some(x=>!x))return [];const percent=c.role==='dimmer'||targets.some(x=>x.max!==c.max)||targets.some(x=>x.virtual);return [{...c,max:percent?100:c.max,percent,targets}];});}
export function readControl(control,frame,held={}){const values=control.targets.map(t=>Math.round((t.virtual?(held[t.fixtureId]??brightnessValue(t,frame)):channelValue(t,frame))/t.max*control.max));return {value:values[0],mixed:values.some(v=>v!==values[0])};}
export function controlChanges(control,value){return control.targets.map(t=>t.virtual?{fixtureId:t.fixtureId,brightness:Math.round(value/control.max*255)}:{fixtureId:t.fixtureId,offset:t.offset,value:Math.round(value/control.max*t.max)});}
export function writeControl(control,value,frame,mask,memory){for(const t of control.targets){const v=Math.round(value/control.max*t.max);if(t.virtual)setBrightness(t,v,frame,memory,mask);else{frame[t.address]=t.pair?v>>8:v;mask[t.address]=true;if(t.pair){frame[t.address+1]=v&255;mask[t.address+1]=true;}}}}
