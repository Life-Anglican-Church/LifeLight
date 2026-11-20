export const DEFAULT_FOLDER='Sunday Services';
export function migrateShow(show){
 if(show.layoutVersion===2)return false;
 show.folders=[DEFAULT_FOLDER,'General'];let sunday=new Set();
 const names=['pre-service','full stage','centre focus','lights off'];
 for(const name of names){const s=show.scenes.find(s=>s.name.trim().toLowerCase()===name);if(s)sunday.add(s.id);}
 for(const s of show.scenes){s.sourcePage??=s.page;s.page=sunday.has(s.id)?DEFAULT_FOLDER:'General';if(sunday.has(s.id)&&s.name.toLowerCase()==='lights off')s.name='Lights off';s.effects??=[];s.loop??=s.steps.length>1;}
 for(const f of show.fixtures){f.group='';f.position??=null;}
 show.layoutVersion=2;return true;
}
export function validateEffects(effects,show){
 if(!Array.isArray(effects)||effects.length>8)throw Error('Use at most eight effects');
 for(const e of effects){if(!['pulse','colourCycle','panSweep'].includes(e.type)||!Array.isArray(e.fixtureIds)||e.fixtureIds.length>show.fixtures.length||e.fixtureIds.some(id=>!show.fixtures.some(f=>f.id===id))||!Number.isFinite(e.periodMs)||e.periodMs<250||e.periodMs>3600000||!Number.isFinite(e.depth)||e.depth<0||e.depth>1)throw Error('Invalid effect settings');}
 return true;
}
