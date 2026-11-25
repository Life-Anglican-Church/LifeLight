// The supplied ProDMX libraries use the amber type for explicitly named UV
// emitters too. Only disambiguate known emitter channels, never function/reset
// channels or ambiguous combined Amber/UV controls.
export function namedUvEmitter(name){return /(?:^|[^a-z])uv(?:$|[^a-z])|ultra[\s-]*violet/i.test(String(name??''))&&!/\bamber\b|reset|macro|mode|strobe|shutter|undef|internal preset/i.test(String(name??''));}
export function correctUvRoles(profiles){let changed=false;for(const p of profiles??[])for(const [i,c]of (p.channels??[]).entries())if(c.role==='amber'&&namedUvEmitter(c.name)){const partner=c.coarse&&p.channels[i+1]?.fine?p.channels[i+1]:c.fine&&p.channels[i-1]?.coarse?p.channels[i-1]:null;c.role='uv';if(partner?.role==='amber')partner.role='uv';changed=true;}return changed;}
