import dgram from 'node:dgram';
export function advertise(getInfo,{port=47833,onError=()=>{},onReady=()=>{},retryDelay=3000,bindAddress='0.0.0.0'}={}){
 let socket,retry,stopped=false;const replies=new Set();
 function bind(){if(stopped)return;const current=socket=dgram.createSocket({type:'udp4',reuseAddr:true});current.on('error',error=>{onError(error);if(socket!==current||stopped)return;socket=null;try{current.close();}catch{}clearTimeout(retry);retry=setTimeout(bind,retryDelay);retry.unref();});
 current.on('message',(bytes,peer)=>{if(bytes.length>1024||replies.size>=32||stopped)return;try{if(JSON.parse(bytes).service!=='lifeac-discover')return;const reply=dgram.createSocket('udp4');replies.add(reply);const close=()=>{replies.delete(reply);try{reply.close();}catch{}};reply.on('error',error=>{onError(error);close();});reply.connect(peer.port,peer.address,()=>{try{const info=getInfo(peer.address,reply.address().address);if(!info?.url)return close();reply.send(Buffer.from(JSON.stringify({service:'lifeac-lighting',...info})),close);}catch{close();}});}catch{}});
 current.bind(port,bindAddress,()=>{if(!stopped)onReady(current.address());});}
 bind();return ()=>{stopped=true;clearTimeout(retry);for(const reply of replies)try{reply.close();}catch{}replies.clear();if(socket)try{socket.close();}catch{}};
}
