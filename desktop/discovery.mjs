import dgram from 'node:dgram';
import os from 'node:os';
import dns from 'node:dns/promises';
import net from 'node:net';
export function normalizeHost(value){const text=String(value??'').trim();if(!text)throw Error('Enter the host IP address or computer name.');const url=new URL(text.includes('://')?text:'https://'+text);if(url.protocol!=='https:'||url.username||url.password||url.pathname!=='/'||url.search||url.hash)throw Error('Enter an HTTPS host address, without a path.');if(!url.port)url.port='47832';return url.href;}
export function broadcast(address,mask){return address.split('.').map((v,i)=>Number(v)|(~Number(mask.split('.')[i])&255)).join('.');}
export async function discover({hostname,port=47833,duration=4200,interfaces=os.networkInterfaces(),probeAddresses=null}={}){
 const targets=new Set(probeAddresses??['255.255.255.255']);if(!probeAddresses)for(const n of Object.values(interfaces).flat())if(n?.family==='IPv4'&&!n.internal&&n.netmask)targets.add(broadcast(n.address,n.netmask));
 // Routed VPNs often suppress broadcast. A remembered computer name can still
 // resolve to its current address and be queried directly, without saving an IP.
 if(hostname&&!net.isIP(hostname))await Promise.all([hostname,hostname+'.local'].map(async name=>{let timer;try{const addresses=await Promise.race([dns.lookup(name,{family:4,all:true}),new Promise(resolve=>{timer=setTimeout(()=>resolve([]),1500);})]);for(const item of addresses)targets.add(item.address);}catch{}finally{clearTimeout(timer);}}));
 return new Promise(resolve=>{const found=new Map(),socket=dgram.createSocket('udp4');let timer,repeat,done=false;const finish=()=>{if(done)return;done=true;clearTimeout(timer);clearInterval(repeat);try{socket.close();}catch{}resolve([...found.values()]);};socket.on('error',finish);socket.on('message',(bytes,peer)=>{if(bytes.length>4096)return;try{const info=JSON.parse(bytes),url=new URL(info.url);if(info.service!=='lifeac-lighting'||url.protocol!=='https:'||url.hostname!==peer.address||url.username||url.password||url.pathname!=='/'||url.search||url.hash)return;found.set(url.origin,{...info,url:url.href});}catch{}});socket.bind(0,'0.0.0.0',()=>{socket.setBroadcast(true);const send=()=>{for(const address of targets)socket.send(Buffer.from('{"service":"lifeac-discover"}'),port,address,()=>{});};send();repeat=setInterval(send,900);timer=setTimeout(finish,duration);});});
}
