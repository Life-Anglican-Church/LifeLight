import {execFileSync} from 'node:child_process';

let cached;
export function parseCredential(output){
 const fields=Object.fromEntries(String(output).split(/\r?\n/).filter(line=>line.includes('=')).map(line=>{const i=line.indexOf('=');return [line.slice(0,i),line.slice(i+1)];}));
 if(fields.host!=='github.com'||fields.protocol!=='https'||fields.username?.toLowerCase()!=='lifeanglicanchurch'||!fields.password)throw Error('GitHub sign-in did not return the LifeAnglicanChurch account.');
 return fields.password;
}
export function publishingCredential(){
 if(cached)return cached;
 try{cached=parseCredential(execFileSync('git',['credential','fill'],{input:'protocol=https\nhost=github.com\nusername=LifeAnglicanChurch\n\n',encoding:'utf8',windowsHide:true,timeout:15000,stdio:['pipe','pipe','pipe'],env:{...process.env,GIT_TERMINAL_PROMPT:'0',GCM_INTERACTIVE:'never'}}));}
 catch{throw Error('GitHub publishing requires Windows Git Credential Manager sign-in: git credential-manager github login --username LifeAnglicanChurch --device');}
 return cached;
}
