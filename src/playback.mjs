import fs from 'node:fs';import os from 'node:os';
const bytes=a=>Array.isArray(a)&&a.length===512&&a.every(v=>Number.isInteger(v)&&v>=0&&v<=255);
const boot=()=>Date.now()-os.uptime()*1000;
export function playbackSnapshot(engine,show,{clean=false}={}){
 if(!engine.sceneId||engine.preview||engine.restorePending)return null;const now=engine.clock();
 return {schema:1,at:new Date().toISOString(),boot:boot(),revision:show.revision,sourceHash:show.sourceHash,sceneId:engine.sceneId,baseFrame:engine.baseFrame.slice(),rawFrame:engine.rawFrame.slice(),transition:engine.transition?{...structuredClone(engine.transition),elapsed:now-engine.transition.at}:null,program:engine.program?{index:engine.program.index,remaining:Math.max(0,engine.program.nextAt-now)}:null,effects:structuredClone(engine.effects),effectElapsed:now-engine.effectAt,overrides:clean?[]:[...engine.overrides],brightness:clean?[]:[...engine.brightness],brightnessMemory:clean?[]:[...engine.brightnessMemory],dark:!clean&&engine.dark,serviceEnabled:!clean&&engine.serviceEnabled};
}
export function writePlayback(file,engine,show,options){let data=playbackSnapshot(engine,show,options);if(!data&&options?.clean){data=readPlayback(file,show);if(data){data.overrides=[];data.brightness=[];data.brightnessMemory=[];data.serviceEnabled=false;data.dark=false;}}if(!data)return;fs.writeFileSync(file+'.new',JSON.stringify(data));fs.renameSync(file+'.new',file);}
export function readPlayback(file,show){try{const p=JSON.parse(fs.readFileSync(file,'utf8'));return p.schema===1&&p.sourceHash===show.sourceHash&&show.scenes.some(s=>s.id===p.sceneId&&!s.unsupported)?p:null;}catch{return null;}}
export function restorePlayback(engine,show,p){
 if(!p||p.revision!==show.revision||!bytes(p.baseFrame)||!bytes(p.rawFrame))return false;
 const scene=show.scenes.find(s=>s.id===p.sceneId&&!s.unsupported);if(!scene)return false;const now=engine.clock();
 if(p.transition&&(!bytes(p.transition.from)||!bytes(p.transition.to)||!Number.isFinite(p.transition.duration)||p.transition.duration<1||!Number.isFinite(p.transition.elapsed)||p.transition.elapsed<0))return false;
 if(p.program&&(!Number.isInteger(p.program.index)||!scene.steps[p.program.index]||!Number.isFinite(p.program.remaining)||p.program.remaining<0||p.program.remaining>7200000))return false;
 const pairs=v=>Array.isArray(v)&&v.every(x=>Array.isArray(x)&&x.length===2);
 if(!pairs(p.overrides??[])||!pairs(p.brightness??[])||!pairs(p.brightnessMemory??[]))return false;
 engine.baseFrame=p.baseFrame.slice();engine.rawFrame=p.rawFrame.slice();engine.sceneId=p.sceneId;engine.transition=p.transition?{...structuredClone(p.transition),at:now-p.transition.elapsed}:null;engine.program=p.program?{scene:structuredClone(scene),index:p.program.index,nextAt:now+p.program.remaining}:null;engine.effects=structuredClone(scene.effects??[]);engine.effectAt=now-(Number.isFinite(p.effectElapsed)?p.effectElapsed:0);
 const sameBoot=Math.abs(boot()-p.boot)<120000;
 if(sameBoot){for(const [i,v]of p.overrides??[])if(Number.isInteger(i)&&['analog','pair','fine'].includes(engine.policies[i]?.kind)&&Number.isInteger(v)&&v>=0&&v<=255)engine.overrides.set(i,v);for(const [id,v]of p.brightness??[])if(show.fixtures.some(f=>f.id===id)&&Number.isInteger(v)&&v>=0&&v<=255)engine.brightness.set(id,v);for(const [id,values]of p.brightnessMemory??[])if(Array.isArray(values)&&values.length>0&&values.every(v=>Number.isFinite(v)&&v>=0&&v<=1))engine.brightnessMemory.set(id,values);engine.dark=p.dark===true;engine.serviceEnabled=p.serviceEnabled===true;}
 engine.pauseClock();engine.applyOutput();engine.note='Previous playback retained; waiting for its USB controller';return true;
}
