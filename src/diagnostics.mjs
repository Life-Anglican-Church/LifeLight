import fs from 'node:fs';import path from 'node:path';
export class DiagnosticLog{
 constructor(dir){this.dir=path.join(dir,'logs');fs.mkdirSync(this.dir,{recursive:true});this.file=path.join(this.dir,'lighting.jsonl');this.recent=[];}
 redact(value){let s=JSON.stringify(value);for(const name of [process.env.USERNAME,process.env.COMPUTERNAME])if(name&&name.length>2)s=s.split(name).join('[redacted]');return JSON.parse(s.replace(/github_pat_[A-Za-z0-9_]+/g,'[credential]').replace(/#[a-f0-9]{64}/gi,'#[credential]').replace(/[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}/g,'[email]').replace(/[A-Za-z]:\\\\[^"\n]*/g,'[local path]'));}
 record(op,detail){const row=this.redact({at:new Date().toISOString(),op,detail});this.recent.push(row);if(this.recent.length>500)this.recent.shift();try{if(fs.existsSync(this.file)&&fs.statSync(this.file).size>2_000_000){for(let i=3;i>=1;i--){const old=this.file+'.'+i;if(i===3&&fs.existsSync(old))fs.unlinkSync(old);if(i<3&&fs.existsSync(old))fs.renameSync(old,this.file+'.'+(i+1));}fs.renameSync(this.file,this.file+'.1');}fs.appendFileSync(this.file,JSON.stringify(row)+'\n');}catch{this.writeFailed=true;}return row;}
 export(){return [3,2,1,0].flatMap(i=>{try{return fs.readFileSync(this.file+(i?'.'+i:''),'utf8').trim().split('\n').filter(Boolean).map(x=>JSON.parse(x));}catch{return [];}});}
}
