import {fixtureVisual} from './fixture-visual.js';

const shapes = ['Ellipse', 'Rectangle', 'Triangle'];
const gradients = ['Radial', 'Linear'];
const bounds = {
 x:[0,1], y:[0,1], width:[.05,1], height:[.05,1],
 angle:[-180,180], hotspotX:[0,1], hotspotY:[0,1], softness:[0,1]
};

export function validateFootprint(value) {
 if (!value || !shapes.includes(value.shape) || !gradients.includes(value.falloff))
  throw Error('Choose a footprint shape and gradient');
 const clean = {shape:value.shape,falloff:value.falloff};
 for (const [key,[min,max]] of Object.entries(bounds)) {
  const n = value[key];
  if (!Number.isFinite(n) || n<min || n>max) throw Error('Invalid footprint '+key);
  clean[key] = n;
 }
 return clean;
}

// Both renderers use photo-relative coordinates and screen blending.
export function drawFootprints(canvas,show,frame,draft=null) {
 if (!canvas || canvas.hidden) return;
 const size=canvas.getBoundingClientRect(), scale=Math.min(2,window.devicePixelRatio||1);
 const w=Math.round(size.width*scale), h=Math.round(size.height*scale);
 if (!w||!h) return;
 if (canvas.width!==w) canvas.width=w;
 if (canvas.height!==h) canvas.height=h;
 const ctx=canvas.getContext('2d');
 ctx.clearRect(0,0,w,h);
 ctx.globalCompositeOperation='screen';
 for (const fixture of show.fixtures) {
  const editing=draft?.id===fixture.id;
  const f=editing?draft.footprint:fixture.footprint;
  if (!f || fixture.universe!==1) continue;
  const visual=fixtureVisual(show,fixture,frame);
  const level=editing?Math.max(.65,visual.level):visual.level;
  if (!level) continue;
  const rgb=visual.level?visual.rgb.map(n=>Math.round(n/visual.level)):[200,170,255];
  const spread=.08+(1-f.softness)*.72;
  ctx.save();
  ctx.translate(f.x*w,f.y*h);
  ctx.rotate(f.angle*Math.PI/180);
  ctx.scale(f.width*w,f.height*h);
  ctx.translate(-.5,-.5);
  let gradient;
  if (f.falloff==='Radial') {
   gradient=ctx.createRadialGradient(f.hotspotX,f.hotspotY,0,f.hotspotX,f.hotspotY,.72);
   gradient.addColorStop(0,`rgba(${rgb},${level*.86})`);
   gradient.addColorStop(spread,`rgba(${rgb},${level*.47})`);
   gradient.addColorStop(1,`rgba(${rgb},0)`);
  } else {
   const radius=Math.max(f.hotspotY,1-f.hotspotY);
   gradient=ctx.createLinearGradient(0,f.hotspotY-radius,0,f.hotspotY+radius);
   gradient.addColorStop(0,`rgba(${rgb},0)`);
   gradient.addColorStop((1-spread)/2,`rgba(${rgb},${level*.47})`);
   gradient.addColorStop(.5,`rgba(${rgb},${level*.86})`);
   gradient.addColorStop((1+spread)/2,`rgba(${rgb},${level*.47})`);
   gradient.addColorStop(1,`rgba(${rgb},0)`);
  }
  ctx.fillStyle=gradient;
  ctx.beginPath();
  if(f.shape==='Ellipse')ctx.ellipse(.5,.5,.5,.5,0,0,2*Math.PI);
  else if(f.shape==='Rectangle')ctx.rect(0,0,1,1);
  else {ctx.moveTo(.5,0);ctx.lineTo(1,1);ctx.lineTo(0,1);ctx.closePath();}
  ctx.fill();
  if(editing){
   ctx.globalCompositeOperation='source-over';
   ctx.setTransform(1,0,0,1,0,0);
   ctx.strokeStyle='#fff';
   ctx.lineWidth=1.5*scale;
   ctx.setLineDash([4*scale,3*scale]);
   ctx.stroke();
  }
  ctx.restore();
 }
}

export function openFootprintEditor({getState,modal,button,close,save,escape,fixtureId}) {
 const initial=getState().show;
 const fixtures=initial.fixtures.filter(f=>f.universe===1);
 if (!fixtures.length) throw Error('Add a fixture before drawing footprints');
 const drafts=new Map();
 let id=fixtures.some(f=>f.id===fixtureId)?fixtureId:fixtures[0].id;
 const getDraft=()=>{
  if (!drafts.has(id)){
   const fixture=fixtures.find(f=>f.id===id);
   drafts.set(id,structuredClone(fixture.footprint??{
    shape:'Ellipse',falloff:'Radial',x:fixture.position?.x??.5,y:.7,width:.3,height:.25,
    angle:0,hotspotX:.5,hotspotY:.5,softness:.75
   }));
  }
  return drafts.get(id);
 };
 const fields=[
  ['x','Horizontal position',0,100],['y','Vertical position',0,100],
  ['width','Width',5,100],['height','Height',5,100],['angle','Rotation',-180,180],
  ['hotspotX','Hotspot left / right',0,100],['hotspotY','Hotspot up / down',0,100],
  ['softness','Softness',0,100]
 ];
 const change=async footprint=>{await save({id,footprint,revision:initial.revision});await close();};
 modal('Light footprints',`<div class="footprint-editor">
  <div><div class="footprint-editor-photo"><img src="assets/stage.jpg" alt="Stage"><canvas aria-hidden="true"></canvas></div>
  <p class="small">Drag on the photo to position the footprint. The selected footprint is brightened here for editing.</p></div>
  <div><label>Fixture<select id="footprint-fixture">${fixtures.map(f=>`<option value="${escape(f.id)}">${escape(f.name)} · ${f.address}</option>`).join('')}</select></label>
  <label>Shape<select data-fp="shape">${shapes.map(s=>`<option>${s}</option>`).join('')}</select></label>
  <label>Gradient<select data-fp="falloff">${gradients.map(s=>`<option>${s}</option>`).join('')}</select></label>
  ${fields.map(([key,label,min,max])=>`<label>${label} <output data-fp-value="${key}"></output><input data-fp="${key}" type="range" min="${min}" max="${max}" step="1"></label>`).join('')}</div>
 </div>`,[button('Cancel',close),button('Remove footprint',()=>change(null)),button('Save footprint',()=>change(getDraft()),'primary')]);
 const editor=document.querySelector('.footprint-editor'), canvas=editor.querySelector('canvas');
 const photo=editor.querySelector('.footprint-editor-photo');
 const redraw=()=>drawFootprints(canvas,initial,getState().engine.frame,{id,footprint:getDraft()});
 function sync(){
  const draft=getDraft();
  for (const input of editor.querySelectorAll('[data-fp]')){
   const key=input.dataset.fp;
   input.value=input.type==='range'?Math.round(draft[key]*(key==='angle'?1:100)):draft[key];
   const label=editor.querySelector('[data-fp-value="'+key+'"]');
   if(label)label.value=input.value+(key==='angle'?'°':'%');
   if(key==='hotspotX')input.disabled=draft.falloff==='Linear';
  }
  redraw();
 }
 const picker=editor.querySelector('#footprint-fixture');
 picker.value=id;
 picker.onchange=()=>{id=picker.value;sync();};
 for(const input of editor.querySelectorAll('[data-fp]'))input.oninput=()=>{
  const key=input.dataset.fp;
  getDraft()[key]=input.type==='range'?Number(input.value)/(key==='angle'?1:100):input.value;
  sync();
 };
 const move=event=>{
  const r=photo.getBoundingClientRect(), draft=getDraft();
  draft.x=Math.max(0,Math.min(1,(event.clientX-r.left)/r.width));
  draft.y=Math.max(0,Math.min(1,(event.clientY-r.top)/r.height));
  sync();
 };
 photo.onpointerdown=event=>{
  if(event.button!==0)return;
  photo.setPointerCapture(event.pointerId);
  move(event);
 };
 photo.onpointermove=event=>{if(photo.hasPointerCapture(event.pointerId))move(event);};
 editor.querySelector('img').onload=redraw;
 sync();
}
