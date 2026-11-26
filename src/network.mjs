import os from 'node:os';
const integer=ip=>ip.split('.').reduce((n,v)=>(n*256+Number(v))>>>0,0);
export function addressForPeer(peer,fallback,interfaces=os.networkInterfaces()){
 const matches=Object.values(interfaces).flat().filter(n=>n?.family==='IPv4'&&!n.internal&&n.netmask&&(integer(n.address)&integer(n.netmask))===(integer(peer)&integer(n.netmask)));
 return matches.sort((a,b)=>integer(b.netmask)-integer(a.netmask))[0]?.address??fallback;
}
