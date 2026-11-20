import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import os from 'node:os';import path from 'node:path';import {fileURLToPath} from 'node:url';import {createApp} from '../src/server.mjs';import {SimTransport} from '../src/transport.mjs';
const root=path.resolve(fileURLToPath(new URL('..',import.meta.url)));
test('authenticated local server: CSRF, roles, conflicts, scene control and patch persistence',async()=>{
 const dir=fs.mkdtempSync(path.join(os.tmpdir(),'lifeac-http-test-'));const app=await createApp({root,dataDir:dir,transport:new SimTransport(),testing:true});let cookie='',csrf='',state;
 const call=async(url,body,extra={})=>{if(body?.op)body={clientId:'tab-a',...body};const r=await fetch(app.url+url,{method:body?'POST':'GET',headers:{Origin:app.url.slice(0,-1),Cookie:cookie,...(body?{'Content-Type':'application/json','X-CSRF':csrf}:{}),...extra},body:body?JSON.stringify(body):undefined});const c=r.headers.get('set-cookie');if(c)cookie=c.split(';')[0];const d=await r.json();return {status:r.status,data:d};};
 try{
  assert.equal((await call('api/state')).status,401);let r=await call('api/bootstrap',{token:app.bootstrap});assert.equal(r.status,200);state=r.data;csrf=state.csrf;assert.equal(state.show.fixtures.length,32);assert.equal(state.engine.output,'disconnected');
  assert.equal((await call('api/command',{op:'hold'},{'X-CSRF':'bad'})).status,403);
  assert.equal((await call('api/command',{op:'hold'},{Origin:'https://evil.invalid'})).status,403);
  r=await call('api/command',{op:'arm',sceneId:state.show.scenes[0].id,approved:false});assert.equal(r.status,400);
  r=await call('api/command',{op:'arm',sceneId:state.show.scenes[0].id,approved:true});assert.equal(r.status,200);state=r.data;assert.equal(state.engine.output,'simulation');
  r=await call('api/command',{op:'play',sceneId:state.show.scenes[1].id});assert.equal(r.status,200);
  r=await call('api/command',{op:'preview',clientId:'tab-a'});assert.equal(r.status,200);
  assert.equal((await call('api/command',{op:'preview',clientId:'tab-b'})).status,409);
  await call('api/command',{op:'cancelPreview'});
  r=await call('api/command',{op:'addFixtures',revision:state.show.revision,quantity:1,profileId:state.show.profiles[0].id,name:'New Eve',group:'Back wall'});assert.equal(r.status,200);state=r.data;assert.equal(state.show.fixtures.length,33);assert.equal(state.show.fixtures.at(-1).address,1);
  assert.equal((await call('api/command',{op:'addFixtures',revision:1,quantity:1,profileId:state.show.profiles[0].id})).status,409);
  await call('api/command',{op:'release'});const original=state.show.fixtures[0],oldValue=state.show.scenes[0].steps[0].values[12];
  r=await call('api/command',{op:'updateFixture',revision:state.show.revision,id:original.id,name:original.name,group:original.group,address:2});assert.equal(r.status,200);state=r.data;assert.equal(state.show.scenes[0].steps[0].values[1],oldValue);assert.equal(state.show.scenes[0].mask[12],false);
  assert.equal((await call('api/command',{op:'updateFixture',revision:state.show.revision,id:original.id,name:original.name,address:3000000000})).status,400);
  await call('api/command',{op:'arm',sceneId:state.show.scenes[0].id,approved:true});
  await call('api/command',{op:'preview',clientId:'tab-a'});const added=state.show.fixtures.at(-1);await call('api/command',{op:'adjust',clientId:'tab-a',fixtureId:added.id,offset:0,value:150});
  r=await call('api/command',{op:'saveScene',clientId:'tab-a',revision:state.show.revision,id:state.show.scenes[0].id,name:'Saved preview',fadeMs:1000,mask:state.show.scenes[0].mask,fromPreview:true});assert.equal(r.status,200);state=r.data;assert.equal(state.show.scenes[0].steps[0].values[0],150);assert.equal(state.show.scenes[0].mask[0],true);assert.equal(state.engine.preview,false);
  await call('api/command',{op:'setUser',name:'operator',password:'a good strong passphrase',role:'operator'});await call('api/login',{name:'operator',password:'a good strong passphrase'}).then(r=>{assert.equal(r.status,200);csrf=r.data.csrf;});
  assert.equal((await call('api/command',{op:'simulate',enabled:true})).status,403);assert.equal((await call('api/export')).status,403);assert.equal((await call('api/command',{op:'hold'})).status,200);
  assert.equal((await call('api/command',{op:'blackout'})).status,200);
 }finally{await app.close();fs.rmSync(dir,{recursive:true,force:true});}
});
