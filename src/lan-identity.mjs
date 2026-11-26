import fs from 'node:fs';import path from 'node:path';import {randomBytes,randomUUID,X509Certificate} from 'node:crypto';import {createSecureContext} from 'node:tls';
export function validLanIdentity(pfx,password){try{if(!password)return false;createSecureContext({pfx:fs.readFileSync(pfx),passphrase:password});const certificate=new X509Certificate(fs.readFileSync(pfx+'.cer'));return Date.parse(certificate.validTo)>Date.now()+86400000;}catch{return false;}}
export async function ensureLanIdentity({dataDir,settings,host,generate}){
 const pfx=path.join(dataDir,'lan.pfx'),config=path.join(dataDir,'settings.json');
 let next={...settings,lanEnabled:true,host,port:47832};
 if(!validLanIdentity(pfx,settings.password)){
  const pending=path.join(dataDir,'lan-pending-'+randomUUID()+'.pfx'),password=randomBytes(32).toString('hex');
  try{await generate(pending,password,host);if(!validLanIdentity(pending,password))throw Error('Generated LAN certificate failed validation');for(const suffix of ['','.cer']){if(fs.existsSync(pfx+suffix))fs.copyFileSync(pfx+suffix,pfx+suffix+'.previous');fs.renameSync(pending+suffix,pfx+suffix);}next.password=password;}
  finally{for(const file of [pending,pending+'.cer'])try{fs.unlinkSync(file);}catch{}}
 }
 next.fingerprint=new X509Certificate(fs.readFileSync(pfx+'.cer')).fingerprint256;
 // Save the certificate password BEFORE binding the port. A busy port must not
 // leave the next launch with a new PFX and the previous PFX's password.
 fs.writeFileSync(config+'.new',JSON.stringify(next),{mode:0o600});fs.renameSync(config+'.new',config);return next;
}
