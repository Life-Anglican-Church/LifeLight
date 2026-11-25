// Capture the host's commanded look before the temporary 11am fixture mute.
// It is not a physical reading from the fixtures.
export function saveLook(show,engine,{fixtureIds,sceneId,name,folder,id}){
 if(!Array.isArray(fixtureIds)||!fixtureIds.length||new Set(fixtureIds).size!==fixtureIds.length||fixtureIds.some(id=>!show.fixtures.some(f=>f.id===id&&f.universe===1)))throw Error('Select at least one configured light');
 const selected=new Set(fixtureIds),frame=engine.captureFrame();let scene;
 if(sceneId){scene=show.scenes.find(s=>s.id===sceneId&&!s.unsupported);if(!scene)throw Error('Choose a playable scene');}
 else{
  if(typeof name!=='string'||!name.trim()||name.trim().length>160||!show.folders.includes(folder))throw Error('Enter a scene name and folder');
  scene={id,name:name.trim(),page:folder,fadeMs:1000,loop:false,effects:[],unsupported:null,mask:engine.policies.map(p=>p.kind!=='protected'),steps:[{values:engine.rawFrame.slice(),fadeMs:1000,waitMs:1000}]};show.scenes.push(scene);
 }
 for(let i=0;i<512;i++)if(selected.has(engine.policies[i].fixtureId)&&engine.policies[i].kind!=='protected'){scene.mask[i]=true;for(const step of scene.steps)step.values[i]=frame[i];}
 // Captured fixtures become static; timing and effects on other lights remain.
 scene.effects=(scene.effects??[]).map(e=>({...e,fixtureIds:e.fixtureIds.filter(id=>!selected.has(id))})).filter(e=>e.fixtureIds.length);
 scene.source='LifeAC captured look';return scene.id;
}
