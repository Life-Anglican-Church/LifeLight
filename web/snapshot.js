// An older HTTP response must not replace a newer host event or scene cue.
export function mergeSnapshot(current,next){
 if(current&&Number.isFinite(current.sequence)&&Number.isFinite(next.sequence)&&next.sequence<=current.sequence)return current;
 return {...current,...next,show:next.show&&(!current?.show||next.show.revision>=current.show.revision)?next.show:current?.show};
}
