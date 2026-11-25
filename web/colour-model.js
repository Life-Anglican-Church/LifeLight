import {controlsFor,brightnessValue} from './fixture-controls.js';
export const emitterRoles=new Set(['red','green','blue','white','amber']);
export const clamp=(v,min=0,max=1)=>Math.max(min,Math.min(max,v));
export function hsvToRgb(h,s,v=1){const c=v*s,x=c*(1-Math.abs((h/60)%2-1)),m=v-c;return ([[c,x,0],[x,c,0],[0,c,x],[0,x,c],[x,0,c],[c,0,x]][Math.floor(((h%360)+360)%360/60)]).map(n=>Math.round((n+m)*255));}
export function rgbToHsv(rgb){const [r,g,b]=rgb.map(x=>x/255),hi=Math.max(r,g,b),lo=Math.min(r,g,b),d=hi-lo;let h=d===0?0:hi===r?60*((g-b)/d%6):hi===g?60*((b-r)/d+2):60*((r-g)/d+4);return {h:(h+360)%360,s:hi?d/hi:0,v:hi};}
export const rgbHex=rgb=>'#'+rgb.map(v=>Math.round(clamp(v,0,255)).toString(16).padStart(2,'0')).join('').toUpperCase();
export const parseHex=s=>/^#[\da-f]{6}$/i.test(s)?[1,3,5].map(i=>parseInt(s.slice(i,i+2),16)):null;
// Flat-top hexagon. Hue follows the six RGB/CMY vertices; white is central.
export function hexRadius(angle){const sector=((angle%(Math.PI/3))+Math.PI/3)%(Math.PI/3);return Math.cos(Math.PI/6)/Math.cos(sector-Math.PI/6);}
export function hexColour(x,y){const angle=(Math.atan2(y,x)+Math.PI*2)%(Math.PI*2),s=clamp(Math.hypot(x,y)/hexRadius(angle));return {h:angle/Math.PI*180,s,rgb:hsvToRgb(angle/Math.PI*180,s)};}
export function colourTargets(show,ids){if(!ids.length)return [];const groups=[...new Set(ids)].map(id=>{const controls=controlsFor(show,id);return {id,controls,channels:controls.filter(c=>emitterRoles.has(c.role)),dimmer:controls.find(c=>c.role==='dimmer')};});return groups.every(g=>['red','green','blue'].every(role=>g.channels.some(c=>c.role===role)))?groups:[];}
export function readColour(targets,frame){
 const values=targets.map(g=>{const get=role=>{const c=g.channels.find(c=>c.role===role);return c?((frame[c.address]??0)*(c.pair?256:1)+(c.pair?(frame[c.address+1]??0):0))/c.max:0;},w=get('white'),amber=get('amber');const rgb=[get('red')+w+amber,get('green')+w+amber*.5,get('blue')+w].map(v=>clamp(v));const peak=Math.max(...rgb);return rgb.map(v=>Math.round((peak?v/peak:1)*255));});
 return {rgb:values[0]??[255,255,255],mixed:values.some(rgb=>rgb.some((v,i)=>Math.abs(v-values[0][i])>3))};
}
export function colourChanges(targets,rgb,frame,held={},autoWhite=true){
 if(!Array.isArray(rgb)||rgb.length!==3||rgb.some(v=>!Number.isInteger(v)||v<0||v>255))throw Error('Invalid colour');
 return targets.flatMap(g=>{const white=autoWhite&&g.channels.some(c=>c.role==='white')?Math.min(...rgb):0,values={red:rgb[0]-white,green:rgb[1]-white,blue:rgb[2]-white,white,amber:0};const changes=g.channels.map(c=>({fixtureId:g.id,offset:c.offset,value:Math.round(values[c.role]/255*c.max)}));if(g.dimmer?.virtual)changes.push({fixtureId:g.id,brightness:held[g.id]??brightnessValue(g.dimmer,frame)});return changes;});
}
