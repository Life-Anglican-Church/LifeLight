// Reorder the real grid as the pointer moves; persist once, on a valid drop.
export function wireSceneDrag(grid,onCommit){
 let drag=null;
 const cards=()=>[...grid.querySelectorAll('[data-scene-id]')];
 const moveGhost=e=>{if(drag&&e.clientX&&e.clientY){drag.ghost.style.left=e.clientX-drag.dx+'px';drag.ghost.style.top=e.clientY-drag.dy+'px';}};
 const cleanup=restore=>{if(!drag)return;const d=drag;drag=null;document.removeEventListener('dragover',moveGhost);d.ghost.remove();d.source.classList.remove('drag-placeholder');if(restore&&grid.isConnected)for(const node of d.original)grid.append(node);};
 for(const source of cards()){
  const grip=source.querySelector('.drag-grip');if(!grip)continue;
  grip.addEventListener('dragstart',e=>{
   const r=source.getBoundingClientRect(),ghost=source.cloneNode(true);ghost.removeAttribute('data-scene-id');ghost.querySelectorAll('[data-play]').forEach(b=>{b.removeAttribute('data-play');b.disabled=true;});ghost.classList.add('scene-drag-preview');ghost.style.width=r.width+'px';ghost.style.height=r.height+'px';document.body.append(ghost);
   drag={source,ghost,original:cards(),dx:e.clientX-r.left,dy:e.clientY-r.top};moveGhost(e);
   const blank=document.createElement('canvas');blank.width=blank.height=1;e.dataTransfer.setDragImage(blank,0,0);e.dataTransfer.setData('application/lifeac-scene',source.dataset.sceneId);e.dataTransfer.effectAllowed='move';source.classList.add('drag-placeholder');document.addEventListener('dragover',moveGhost);
  });
  grip.addEventListener('dragend',()=>cleanup(true));
 }
 grid.addEventListener('dragover',e=>{
  if(!drag)return;e.preventDefault();e.dataTransfer.dropEffect='move';moveGhost(e);
  const target=e.target.closest('[data-scene-id]');if(!target||target===drag.source||!grid.contains(target))return;
  const nodes=cards(),before=new Map(nodes.map(n=>[n,n.getBoundingClientRect()])),from=nodes.indexOf(drag.source),to=nodes.indexOf(target);
  grid.insertBefore(drag.source,from<to?target.nextSibling:target);
  if(!matchMedia('(prefers-reduced-motion: reduce)').matches)for(const n of nodes){if(n===drag.source)continue;const a=before.get(n),b=n.getBoundingClientRect();n.getAnimations().forEach(a=>a.cancel());n.animate([{transform:`translate(${a.left-b.left}px,${a.top-b.top}px)`},{transform:'translate(0,0)'}],{duration:150,easing:'ease-out'});}
 });
 grid.addEventListener('drop',e=>{if(!drag)return;e.preventDefault();const ids=cards().map(n=>n.dataset.sceneId),old=drag.original.map(n=>n.dataset.sceneId);cleanup(false);if(ids.some((id,i)=>id!==old[i]))Promise.resolve(onCommit(ids)).catch(()=>{for(const id of old){const n=grid.querySelector('[data-scene-id="'+id+'"]');if(n)grid.append(n);}});});
 // A host revision/role change replaces the grid; remove its floating preview.
 const observer=new MutationObserver(()=>{if(!grid.isConnected){cleanup(false);observer.disconnect();}});observer.observe(grid.parentNode,{childList:true});
 return ()=>{cleanup(false);observer.disconnect();};
}
