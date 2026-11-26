// Preview commanded DMX output, not telemetry from the physical fixture.
export function fixtureVisual(show,fixture,frame){
 const profile=show.profiles.find(p=>p.id===fixture.profileId),roles=new Map();
 for(const [i,c]of (profile?.channels??[]).entries()){if(c.fine||!['dimmer','red','green','blue','white','amber','uv'].includes(c.role))continue;const pair=c.coarse&&profile.channels[i+1]?.fine&&profile.channels[i+1]?.role===c.role;if(c.coarse&&!pair)continue;const v=((frame[fixture.address-1+i]??0)*(pair?256:1)+(pair?(frame[fixture.address+i]??0):0))/(pair?65535:255);const values=roles.get(c.role)??[];values.push(v);roles.set(c.role,values);}
 const get=r=>{const values=roles.get(r)??[];return values.length?values.reduce((a,b)=>a+b,0)/values.length:0;},master=roles.has('dimmer')?get('dimmer'):1,hasColour=['red','green','blue','white','amber','uv'].some(r=>roles.has(r));
 const rgb=(!hasColour&&!roles.has('dimmer')?[0,0,0]:hasColour?[get('red')+get('white')+get('amber')+get('uv')*.45,get('green')+get('white')+get('amber')*.55+get('uv')*.06,get('blue')+get('white')+get('amber')*.04+get('uv')]:[1,.94,.83]).map(v=>Math.round(Math.min(1,v)*master*255));
 return {rgb,colour:'rgb('+rgb.join(',')+')',level:Math.max(...rgb)/255};
}
