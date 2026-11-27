import qrcode from './vendor/qrcode.js';

export async function pairedDevices({api,modal,button,close,escape}) {
 const devices=await api('/api/paired-devices');
 modal('Paired Android devices',
  '<p>Pair an Android phone or tablet at the desk. Each code works once and expires in two minutes.</p>'+
  '<div id="mobile-qr" aria-live="polite"></div>'+
  (devices.length?'<ul>'+devices.map(d=>'<li>'+escape(d.name)+
   ' <button data-remove-mobile="'+escape(d.id)+'">Remove</button></li>').join('')+'</ul>':'<p>No approved devices.</p>'),
  [button('Pair Android device',async()=>{
   const pairing=await api('/api/paired-devices',{action:'issue'});
   const qr=qrcode(0,'M');
   qr.addData(JSON.stringify(pairing));
   qr.make();
   const image=document.createElement('img');
   image.src=qr.createDataURL(5,20);image.width=260;image.alt='Android pairing QR code';
   const label=document.createElement('p');label.textContent='Scan this code in LifeAC Lighting.';
   document.querySelector('#mobile-qr')?.replaceChildren(image,label);
  }),button('Refresh devices',()=>pairedDevices({api,modal,button,close,escape})),button('Done',close)]);
 for(const control of document.querySelectorAll('[data-remove-mobile]')){
  const remove=button('Remove',async()=>{
   await api('/api/paired-devices',{action:'remove',id:control.dataset.removeMobile});
   await pairedDevices({api,modal,button,close,escape});
  });
  control.replaceWith(remove);
 }
}
