import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import https from 'node:https';
import os from 'node:os';
import path from 'node:path';
import {spawnSync,execFileSync} from 'node:child_process';
import {X509Certificate} from 'node:crypto';
import {createApp} from '../src/server.mjs';
import {SimTransport} from '../src/transport.mjs';
import {MobileDevices} from '../src/mobile.mjs';

const openssl=spawnSync('openssl',['version'],{stdio:'ignore'}).status===0;

test('pairing expires, superseded codes are rejected, and revocation survives a host restart',t=>{
 const dir=fs.mkdtempSync(path.join(os.tmpdir(),'lifeac-mobile-approval-'));
 t.after(()=>fs.rmSync(dir,{recursive:true,force:true}));
 t.mock.method(Date,'now',()=>120000);
 const url='https://127.0.0.1:47832/', fingerprint='ab'.repeat(32);
 const devices=new MobileDevices(dir);
 const superseded=devices.issue(url,fingerprint);
 const issued=devices.issue(url,fingerprint);
 assert.throws(()=>devices.pair(superseded.code,'Tab A8'),{status:401});
 t.mock.method(Date,'now',()=>issued.expires);
 assert.throws(()=>devices.pair(issued.code,'Tab A8'),{status:401});
 const valid=devices.issue(url,fingerprint);
 const approved=devices.pair(valid.code,'Tab A8');
 const header='Bearer '+approved.token;
 const restarted=new MobileDevices(dir);
 assert.equal(restarted.authenticate(header).name,'Tab A8');
 restarted.remove(approved.id);
 assert.throws(()=>new MobileDevices(dir).authenticate(header),{status:401});
});

test('paired devices can operate the shared show over HTTPS and lose access on removal',{skip:!openssl},async()=>{
 const dir=fs.mkdtempSync(path.join(os.tmpdir(),'lifeac-mobile-'));
 let app;
 try{
  const cert=path.join(dir,'lan.pfx.cer'),key=path.join(dir,'test.key'),pfx=path.join(dir,'lan.pfx');
  const password='mobile-test-only';
  execFileSync('openssl',['req','-x509','-newkey','rsa:2048','-nodes','-days','2',
   '-subj','/CN=127.0.0.1','-addext','subjectAltName=IP:127.0.0.1','-keyout',key,'-out',cert],{stdio:'ignore'});
  execFileSync('openssl',['pkcs12','-export','-inkey',key,'-in',cert,'-out',pfx,'-passout','pass:'+password],{stdio:'ignore'});
  const certificate=fs.readFileSync(cert);
  app=await createApp({dataDir:dir,testing:true,testLan:{password},transport:new SimTransport()});
  await app.engine.arm('scene-1',true);
  const local=new URL(app.url),remote=new URL(app.lanURL());
  const desk=(route,body,headers={})=>fetch(new URL(route,local),{
   method:body?'POST':'GET',headers:{origin:local.origin,'content-type':'application/json',...headers},
   body:body&&JSON.stringify(body)
  });
  const mobile=(route,body,token)=>new Promise((resolve,reject)=>{
   const req=https.request(new URL('/api/mobile/'+route,remote),{
    method:body?'POST':'GET',ca:certificate,headers:{origin:remote.origin,'content-type':'application/json',
     ...(token?{authorization:'Bearer '+token}:{})}
   },res=>{
    let raw='';res.setEncoding('utf8');res.on('data',chunk=>raw+=chunk);
    res.on('end',()=>resolve({status:res.statusCode,data:JSON.parse(raw)}));
   });
   req.on('error',reject);req.end(body&&JSON.stringify(body));
  });
  const bootstrap=await desk('/api/bootstrap',{token:app.bootstrap});
  const cookie=bootstrap.headers.get('set-cookie').split(';')[0];
  let state=await bootstrap.json();
  const headers={cookie,'x-csrf':state.csrf};
  assert.equal((await desk('/api/paired-devices',{action:'issue'},headers)).status,403);
  state=await (await desk('/api/command',{op:'adminUnlock',pin:'5433'},headers)).json();
  assert.equal(state.role,'admin');
  assert.equal((await desk('/api/paired-devices',{action:'issue'},{cookie})).status,403);
  const qr=await (await desk('/api/paired-devices',{action:'issue'},headers)).json();
  assert.equal(qr.url,remote.href);
  assert.equal(qr.fingerprint,new X509Certificate(certificate).fingerprint256);
  assert.equal((await desk('/api/mobile/pair',{code:qr.code,name:'Tab A8'})).status,403);
  const paired=await mobile('pair',{code:qr.code,name:'Tab A8'});
  assert.equal(paired.status,200);
  const {token,id}=paired.data;
  assert.equal((await mobile('pair',{code:qr.code,name:'Another tablet'})).status,401);
  state=(await mobile('state',null,token)).data;
  assert.ok(state.folders.includes('Sunday Services'));
  const footprint={shape:'Triangle',falloff:'Linear',x:.5,y:.7,width:.3,height:.25,
   angle:12,hotspotX:.5,hotspotY:.25,softness:.7};
  const desktopSave=await desk('/api/command',{op:'setFootprint',revision:state.revision,id:'fixture-5',footprint},headers);
  assert.equal(desktopSave.status,200);
  state=(await mobile('state',null,token)).data;
  assert.deepEqual(state.fixtures.find(f=>f.id==='fixture-5').footprint,footprint);
  assert.ok(state.fixtures.find(f=>f.id==='fixture-5').footprintOutput.every(Number.isInteger));
  const command=body=>mobile('command',{revision:state.revision,...body},token);
  assert.equal((await command({op:'saveScene'})).status,403);
  assert.equal((await command({op:'setFootprint',id:'fixture-5',footprint:null})).status,403);
  assert.equal((await command({op:'cue',sceneId:'scene-2',revision:state.revision+1})).status,409);
  await desk('/api/command',{op:'preview'},headers);
  assert.equal((await mobile('state',null,token)).data.preview,true);
  assert.equal((await command({op:'cue',sceneId:'scene-2'})).status,409);
  await desk('/api/command',{op:'cancelPreview'},headers);
  assert.equal((await mobile('state',null,token)).data.preview,false);
  const fixture='fixture-5';
  const select={ids:[fixture]};
  let result=await command({op:'adjust',...select,key:'dimmer:1',value:25});
  assert.equal(result.status,200);
  assert.equal(result.data.fixtures.find(f=>f.id===fixture).levels['dimmer:1'],25);
  assert.equal(result.data.fixtures.find(f=>f.id===fixture).held,true);
  result=await command({op:'cue',sceneId:'scene-2'});
  assert.equal(result.data.sceneId,'scene-2');
  assert.equal(result.data.fixtures.find(f=>f.id===fixture).levels['dimmer:1'],25);
  result=await command({op:'release',...select});
  assert.equal(result.data.fixtures.find(f=>f.id===fixture).held,false);
  for(const id of [fixture,'fixture-6']){
   assert.equal((await command({op:'adjust',ids:[id],key:'dimmer:1',value:32})).status,200);
  }
  const allFixtureIds=state.fixtures.map(f=>f.id);
  result=await command({op:'release',ids:allFixtureIds});
  assert.equal(result.status,200);
  assert.equal(result.data.sceneId,'scene-2');
  assert.ok(result.data.fixtures.every(f=>!f.held));
  assert.equal((await command({op:'adjust',...select,key:'dimmer:1',value:42})).status,200);
  const channel=app.store.show.fixtures.find(f=>f.id===fixture).address-1;
  const expected=app.engine.captureFrame()[channel];
  const saved=(await (await desk('/api/command',{op:'captureLook',revision:state.revision,
   sceneIds:['scene-3'],fixtureIds:[fixture]},headers)).json()).show.scenes.find(s=>s.id==='scene-3');
  assert.equal(saved.steps[0].values[channel],expected);
  const list=await (await desk('/api/paired-devices',null,headers)).json();
  assert.deepEqual(list.map(d=>[d.id,d.name]),[[id,'Tab A8']]);
  assert.ok(!fs.readFileSync(path.join(dir,'mobile-devices.json'),'utf8').includes(token));
  assert.equal((await desk('/api/paired-devices',{action:'remove',id},headers)).status,200);
  assert.equal((await mobile('state',null,token)).status,401);
  assert.equal((await command({op:'cue',sceneId:'scene-1'})).status,401);
  assert.equal(app.engine.sceneId,'scene-2');
  assert.ok(app.engine.state().overrides.some(o=>o.fixtureId===fixture));
 }finally{
  if(app)await app.close();
  fs.rmSync(dir,{recursive:true,force:true});
 }
});
