import {execFileSync} from 'node:child_process';

let cached;
export function parseCredential(output){
 const fields=Object.fromEntries(String(output).split(/\r?\n/).filter(line=>line.includes('=')).map(line=>{const i=line.indexOf('=');return [line.slice(0,i),line.slice(i+1)];}));
 if(fields.host!=='github.com'||fields.protocol!=='https'||!['lifeacisaac','lifeanglicanchurch'].includes(fields.username?.toLowerCase())||!fields.password)throw Error('GitHub sign-in did not return the expected account.');
 return fields.password;
}
export function publishingCredential(){
 if(cached)return cached;
 for(const account of ['LifeACIsaac','LifeAnglicanChurch'])try{cached=parseCredential(execFileSync('git',['credential','fill'],{input:'protocol=https\nhost=github.com\nusername='+account+'\n\n',encoding:'utf8',windowsHide:true,timeout:15000,stdio:['pipe','pipe','pipe'],env:{...process.env,GIT_TERMINAL_PROMPT:'0',GCM_INTERACTIVE:'never'}}));return cached;}catch{}
 throw Error('GitHub publishing requires Windows Git Credential Manager sign-in: git credential-manager github login --username LifeACIsaac --browser');
}
