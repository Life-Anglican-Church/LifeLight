import fs from 'node:fs';import path from 'node:path';import {fileURLToPath} from 'node:url';
const root=path.resolve(fileURLToPath(new URL('..',import.meta.url))),workspace=path.dirname(root);
const credential=which=>fs.readFileSync(path.join(workspace,'.release-secrets',which+'.txt'),'utf8').trim();
async function api(url,{method='GET',body,token=credential('publish'),binary=false}={}){const r=await fetch(url.startsWith('https:')?url:'https://api.github.com'+url,{method,headers:{Authorization:`Bearer ${token}`,'User-Agent':'LifeAC-Lighting-Release','X-GitHub-Api-Version':'2022-11-28',Accept:'application/vnd.github+json',...(body?{'Content-Type':binary?'application/octet-stream':'application/json'}:{})},body:body?(binary?body:JSON.stringify(body)):undefined});if(!r.ok){let message='';try{message=(await r.json()).message;}catch{}throw Error(`GitHub ${r.status}: ${message}`);}return r.status===204?null:r.json();}
const mode=process.argv[2]??'inspect';
if(mode==='status'){const r=await api('/repos/LifeAnglicanChurch/LifeACLighting');const contents=await api('/repos/LifeAnglicanChurch/LifeACLighting/contents').catch(()=>[]);console.log(JSON.stringify({private:r.private,size:r.size,branch:r.default_branch,files:contents.map(x=>x.name)}));}
if(mode==='inspect'){
 for(const which of ['publish','read']){const token=credential(which);const user=await api('/user',{token});const repos=await api('/user/repos?per_page=100&sort=updated',{token});console.log(JSON.stringify({credential:which,account:user.login,repositories:repos.map(r=>({name:r.full_name,private:r.private,push:r.permissions?.push}))}));}
}
if(mode==='create'){
 const repo='LifeAnglicanChurch/LifeACLighting';let found;try{found=await api('/repos/'+repo);}catch(e){if(!e.message.startsWith('GitHub 404'))throw e;found=await api('/user/repos',{method:'POST',body:{name:'LifeACLighting',description:'Life Anglican Church lighting control for Windows',private:true,auto_init:true}});}if(found.full_name.toLowerCase()!==repo.toLowerCase()||!found.private)throw Error('Expected a private LifeAC-Lighting repository');fs.writeFileSync(path.join(workspace,'.release-secrets','repository.json'),JSON.stringify({fullName:found.full_name,branch:found.default_branch,url:found.html_url}));console.log(JSON.stringify({repository:found.html_url,private:found.private}));
}
if(mode==='publish-source'){
 const info=JSON.parse(fs.readFileSync(path.join(workspace,'.release-secrets','repository.json'))),repo=info.fullName;
 const head=await api(`/repos/${repo}/git/ref/heads/${info.branch}`);const parent=await api(`/repos/${repo}/git/commits/${head.object.sha}`);const files=[];
 function walk(dir){for(const e of fs.readdirSync(path.join(root,dir),{withFileTypes:true})){const rel=path.posix.join(dir,e.name);if(e.isDirectory())walk(rel);else files.push(rel);}}
 for(const dir of ['src','web','native','docs','tests','inputs','scripts'])walk(dir);for(const name of ['README.md','package.json','build.cmd','INSTALL-FOR-ME.cmd','THIRD-PARTY-NOTICES.txt','.gitignore'])if(fs.existsSync(path.join(root,name)))files.push(name);
 const tree=[];for(const rel of files){const bytes=fs.readFileSync(path.join(root,rel));if(bytes.length>10_000_000)throw Error('Source file unexpectedly large');const text=bytes.toString('utf8');if(/github_pat_[A-Za-z0-9_]{30,}/.test(text))throw Error('Credential-like content detected; upload refused');const blob=await api(`/repos/${repo}/git/blobs`,{method:'POST',body:{content:bytes.toString('base64'),encoding:'base64'}});tree.push({path:rel,mode:'100644',type:'blob',sha:blob.sha});}
 const t=await api(`/repos/${repo}/git/trees`,{method:'POST',body:{base_tree:parent.tree.sha,tree}});const commit=await api(`/repos/${repo}/git/commits`,{method:'POST',body:{message:'Build LifeAC Lighting live-control preview with branded UI and secure release tools',tree:t.sha,parents:[head.object.sha]}});await api(`/repos/${repo}/git/refs/heads/${info.branch}`,{method:'PATCH',body:{sha:commit.sha,force:false}});console.log(JSON.stringify({commit:commit.sha,files:files.length}));
}
if(mode==='release'){
 const info=JSON.parse(fs.readFileSync(path.join(workspace,'.release-secrets','repository.json'))),repo=info.fullName;const release=await api(`/repos/${repo}/releases`,{method:'POST',body:{tag_name:'v0.1.0',target_commitish:info.branch,name:'LifeAC Lighting 0.1.0 — live-control preview',body:fs.readFileSync(path.join(root,'docs','RELEASE-NOTES.md'),'utf8'),draft:true,prerelease:false}});fs.writeFileSync(path.join(workspace,'.release-secrets','release.json'),JSON.stringify(release));console.log(JSON.stringify({id:release.id,url:release.html_url}));
}
if(mode==='upload'){
 const release=JSON.parse(fs.readFileSync(path.join(workspace,'.release-secrets','release.json')));const file=path.resolve(process.argv[3]),name=path.basename(file);const asset=await api(release.upload_url.split('{')[0]+'?name='+encodeURIComponent(name),{method:'POST',body:fs.readFileSync(file),binary:true});console.log(JSON.stringify({name:asset.name,id:asset.id,size:asset.size,digest:asset.digest,url:asset.browser_download_url}));
}
if(mode==='finalize'){
 const info=JSON.parse(fs.readFileSync(path.join(workspace,'.release-secrets','repository.json'))),release=JSON.parse(fs.readFileSync(path.join(workspace,'.release-secrets','release.json')));const published=await api(`/repos/${info.fullName}/releases/${release.id}`,{method:'PATCH',body:{draft:false}});const check=await api(`/repos/${info.fullName}/releases/latest`,{token:credential('read')});console.log(JSON.stringify({url:published.html_url,readOnlyCredentialVerified:check.id===published.id,assets:check.assets.map(a=>({name:a.name,id:a.id,size:a.size,digest:a.digest}))}));
}
