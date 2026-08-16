const DEFAULT_SERVER_URL = 'http://192.168.1.10:3000';
const ADMIN_PIN = '065065';
let serverUrl = localStorage.getItem('serverUrl') || DEFAULT_SERVER_URL;
let token = localStorage.getItem('token') || '';
let deviceId = (window.AndroidBridge && AndroidBridge.getDeviceId()) || localStorage.getItem('deviceId') || ('native-' + Math.random().toString(36).slice(2));
let playlist = [];
let index = 0;
let checkTimer = null;
let heartbeatTimer = null;
let queueTimer = null;
let syncTimer = null;
let playTimer = null;
let updateBusy = false;
let downloadResolvers = {};
localStorage.setItem('deviceId', deviceId);
window.onerror = function (message, source, lineno) { alert('Player JS error: ' + message + '\nLine: ' + lineno); return false; };
function el(id){return document.getElementById(id)}
function show(id){['setup','waiting','errorBox','stage'].forEach(function(x){const b=el(x);if(b)b.classList.add('hidden')});const t=el(id);if(t)t.classList.remove('hidden')}
function overlay(text){const b=el('overlay');if(!b)return;if(!text){b.style.display='none';return}b.innerText=text;b.style.display='block'}
function normalizeServerUrl(value){let url=String(value||'').trim();if(!url)return'';if(!/^https?:\/\//i.test(url))url='http://'+url;url=url.replace(/\/+$/,'');try{return new URL(url).origin}catch(e){return url.replace(/\/player\.html.*$/i,'').replace(/\/api\/.*$/i,'').replace(/\/+$/,'')}}
function setSetupDefault(){const input=el('serverUrl');const hint=el('setupHint');if(input&&!input.value)input.value=serverUrl||'';if(hint)hint.innerText=serverUrl?'Server: '+serverUrl:'Server URL not set'}
function saveServer(){const input=el('serverUrl');serverUrl=normalizeServerUrl(input?input.value:'');if(!serverUrl){alert('Please enter server URL like http://192.168.1.10:3000');return}localStorage.setItem('serverUrl',serverUrl);localStorage.removeItem('token');token='';overlay('Connecting to '+serverUrl);show('waiting');init()}
function resetServer(){clearInterval(checkTimer);clearInterval(heartbeatTimer);clearInterval(queueTimer);clearInterval(syncTimer);clearTimeout(playTimer);localStorage.removeItem('token');localStorage.removeItem('serverUrl');token='';serverUrl='';playlist=[];index=0;playTimer=null;const input=el('serverUrl');if(input)input.value='';const hint=el('setupHint');if(hint)hint.innerText='Server URL cleared. Enter local IP and press Connect.';overlay('');show('setup')}
function retryNow(){init()}
function showError(message){if(playlist.length){overlay(message);return}const e=el('errorText');if(e)e.innerText=message;show('errorBox')}
function showMessage(message){const s=el('stage');if(!s)return;s.innerHTML='<div style="height:100vh;display:flex;align-items:center;justify-content:center;background:#020617;color:white;font:30px Arial;text-align:center;white-space:pre-line;padding:25px;box-sizing:border-box">'+message+'</div>';show('stage')}
async function fetchJson(url,options){const r=await fetch(url,options||{});if(r.status===401){localStorage.removeItem('token');token='';throw new Error('invalid-token')}if(!r.ok){let b='';try{b=await r.text()}catch(e){}throw new Error('HTTP '+r.status+' '+b.substring(0,200))}return r.json()}
function safeName(item){return String(item.file_name||item.original_name||('media_'+(item.id||Date.now()))).replace(/[^a-zA-Z0-9._-]/g,'_')}
function randomFx(){const e=['fx-fade','fx-zoom','fx-slide-left','fx-slide-right','fx-slide-up','fx-slide-down','fx-soft-blur','fx-soft-rotate'];return e[Math.floor(Math.random()*e.length)]}
function saveLocalPlaylist(items){try{localStorage.setItem('localPlaylist',JSON.stringify(items||[]))}catch(e){}}
function loadLocalPlaylist(){try{return JSON.parse(localStorage.getItem('localPlaylist')||'[]')}catch(e){return[]}}
function hasPlayableUrl(item){return!!(item&&(item.local_url||item.full_url||item.file_url))}
function startLocalPlaybackIfAvailable(){playlist=loadLocalPlaylist().filter(hasPlayableUrl);if(playlist.length){show('stage');if(!playTimer)playNext();overlay('Playing saved ads offline');setTimeout(function(){overlay('')},3000);return true}return false}
window.onAndroidDownloadProgress=function(downloadId,percent,file){overlay('Updating '+percent+'%\n'+file)};
window.onAndroidDownloadComplete=function(downloadId,result){if(downloadResolvers[downloadId]){downloadResolvers[downloadId](result);delete downloadResolvers[downloadId]}};
function nativeDownload(item){return new Promise(function(resolve){if(!window.AndroidBridge||!AndroidBridge.downloadMediaAsync){resolve({success:false,error:'AndroidBridge missing'});return}const id='d_'+Date.now()+'_'+Math.random().toString(36).slice(2);downloadResolvers[id]=resolve;AndroidBridge.downloadMediaAsync(id,item.full_url,safeName(item))})}
async function ensureLocal(item,pos,total,jobId){const fileName=safeName(item);try{if(window.AndroidBridge&&AndroidBridge.fileExists){const exists=JSON.parse(AndroidBridge.fileExists(fileName));if(exists.exists){item.local_url=exists.localUrl;item.local_path=exists.path;return item}}}catch(e){}overlay('Updating '+pos+'/'+total+'\n'+fileName);const result=await nativeDownload(item);if(result&&result.success){item.local_url=result.localUrl;item.local_path=result.path;if(jobId)await reportProgress(jobId,Math.round((pos/total)*100),fileName,'downloading','saved local');return item}item.local_url=item.full_url;if(jobId)await reportProgress(jobId,Math.round((pos/total)*100),fileName,'downloading','server fallback');return item}
async function init(){clearInterval(checkTimer);clearInterval(heartbeatTimer);clearInterval(queueTimer);clearInterval(syncTimer);clearTimeout(playTimer);playTimer=null;serverUrl=normalizeServerUrl(localStorage.getItem('serverUrl')||serverUrl||'');setSetupDefault();const localStarted=startLocalPlaybackIfAvailable();if(!serverUrl){show('setup');return}if(token){startOnline();return}if(!localStarted)show('waiting');const info=el('serverInfo');if(info)info.innerText='Server URL: '+serverUrl;try{const data=await fetchJson(serverUrl+'/api/client/init',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({device_id:deviceId})});if(data.registered){token=data.token;localStorage.setItem('token',token);startOnline();return}const c=el('code');if(c)c.innerText=data.pairing_code||'------';overlay('');checkTimer=setInterval(checkApproval,3000)}catch(e){showError('Cannot connect to server.\n'+e.message+'\n\nCheck local IP, Wi-Fi, firewall, and npm start.')}}
async function checkApproval(){try{const data=await fetchJson(serverUrl+'/api/client/check/'+deviceId);if(data.registered){token=data.token;localStorage.setItem('token',token);clearInterval(checkTimer);startOnline();return}const c=el('code');if(data.pairing_code&&c)c.innerText=data.pairing_code}catch(e){}}
async function startOnline(){show('stage');await syncPlaylist();heartbeat();heartbeatTimer=setInterval(heartbeat,15000);syncTimer=setInterval(syncPlaylist,20000);queueTimer=setInterval(checkQueue,10000);checkQueue();if(!playlist.length)showMessage('Waiting for playlist...\nPlease add media to playlist in CMS.')}
async function syncPlaylist(){if(!token||updateBusy)return false;try{const data=await fetchJson(serverUrl+'/api/client/playlist/'+token);const items=data.items||[];if(!items.length)return false;updateBusy=true;for(let i=0;i<items.length;i++){await ensureLocal(items[i],i+1,items.length,null)}playlist=items.filter(hasPlayableUrl);saveLocalPlaylist(playlist);updateBusy=false;if(!playTimer)playNext();return true}catch(e){updateBusy=false;if(String(e.message).includes('invalid-token')){overlay('Client removed from server. Re-pairing...');localStorage.removeItem('token');token='';setTimeout(init,1500)}else if(playlist.length){overlay('Server offline, playing saved ads')}return false}}
async function checkQueue(){if(updateBusy||!token)return;try{const data=await fetchJson(serverUrl+'/api/client/download-job/'+token);if(!data.can_download){if(data.status==='waiting')overlay('Queue waiting');else overlay('');return}updateBusy=true;const job=data.job;const items=data.items||[];for(let i=0;i<items.length;i++){await ensureLocal(items[i],i+1,items.length,job.id)}await reportProgress(job.id,100,'Completed','completed','download complete');playlist=items.filter(hasPlayableUrl);saveLocalPlaylist(playlist);overlay('Update complete');setTimeout(function(){overlay('')},3000);updateBusy=false;if(!playTimer)playNext()}catch(e){updateBusy=false;if(playlist.length)overlay('Server offline, playing saved ads');syncPlaylist()}}
async function reportProgress(jobId,progressValue,file,status,note){try{await fetch(serverUrl+'/api/client/download-progress',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({job_id:jobId,progress:progressValue,current_file:file,status:status,note:note})})}catch(e){}}
function playNext(){const stage=el('stage');if(!stage)return;clearTimeout(playTimer);playTimer=null;show('stage');if(!playlist.length){showMessage('No downloaded ads available');playTimer=setTimeout(playNext,5000);return}const item=playlist[index%playlist.length];index++;const url=item.local_url||item.full_url||item.file_url;const duration=Number(item.duration);const hasDuration=!isNaN(duration)&&duration>0;const effect=randomFx();if(item.file_type==='video'){stage.innerHTML='<video id="adVideo" class="ad-item '+effect+'" autoplay playsinline src="'+url+'"></video>';const video=el('adVideo');if(video){video.muted=false;video.volume=1;video.onended=playNext;video.onerror=playNext;video.play().catch(function(){video.muted=true;video.play().catch(function(){playTimer=setTimeout(playNext,2000)})});if(hasDuration){playTimer=setTimeout(function(){video.pause();playNext()},duration*1000)}}}else{stage.innerHTML='<img class="ad-item '+effect+'" src="'+url+'">';playTimer=setTimeout(playNext,(hasDuration?duration:10)*1000)}}
async function heartbeat(){
  if(!token)return;
  try{
    const response=await fetch(serverUrl+'/api/client/heartbeat',{
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({
        token:token,
        message:'online',
        playing_file:playlist.length?playlist[(index-1+playlist.length)%playlist.length].file_name:'',
        storage_info:window.AndroidBridge?AndroidBridge.getStorageInfo():'native'
      })
    });
    if(response.status===401){
      localStorage.removeItem('token');
      token='';
      overlay('Client removed from server. Re-pairing...');
      setTimeout(init,1500);
    }
  }catch(e){}
}
function ensureAdminUI(){if(document.getElementById('adminHotspot'))return;const hotspot=document.createElement('div');hotspot.id='adminHotspot';hotspot.style.cssText='position:fixed;left:0;top:0;width:130px;height:130px;z-index:999999;background:transparent;';document.body.appendChild(hotspot);const modal=document.createElement('div');modal.id='adminModal';modal.style.cssText='display:none;position:fixed;inset:0;z-index:1000000;background:rgba(0,0,0,.86);color:white;font-family:Arial;align-items:center;justify-content:center;';modal.innerHTML='<div style="width:90%;max-width:520px;background:#0f172a;border:1px solid #334155;border-radius:24px;padding:24px;text-align:center;box-shadow:0 20px 60px rgba(0,0,0,.5)"><h2 style="margin:0 0 10px">Admin Exit Menu</h2><p style="color:#cbd5e1;margin:0 0 18px">Enter PIN to unlock controls.</p><input id="adminPin" type="password" placeholder="Admin PIN" style="width:100%;font-size:22px;padding:14px;border-radius:12px;border:0;margin-bottom:12px;box-sizing:border-box;text-align:center"><div id="adminActions" style="display:none"><button onclick="adminResetServer()" style="width:100%;font-size:20px;padding:13px;margin:7px 0;border:0;border-radius:12px;background:#2563eb;color:white;font-weight:bold">Reset Server / Re-pair</button><button onclick="adminChangeServerUrl()" style="width:100%;font-size:20px;padding:13px;margin:7px 0;border:0;border-radius:12px;background:#0ea5e9;color:white;font-weight:bold">Change Server URL</button><button onclick="adminRestartPlayer()" style="width:100%;font-size:20px;padding:13px;margin:7px 0;border:0;border-radius:12px;background:#7c3aed;color:white;font-weight:bold">Restart Player</button><button onclick="adminExitApp()" style="width:100%;font-size:20px;padding:13px;margin:7px 0;border:0;border-radius:12px;background:#ef4444;color:white;font-weight:bold">Exit App</button></div><button onclick="adminCheckPin()" style="width:100%;font-size:20px;padding:13px;margin:7px 0;border:0;border-radius:12px;background:#f59e0b;color:#111827;font-weight:bold">Unlock</button><button onclick="hideAdminMenu()" style="width:100%;font-size:20px;padding:13px;margin:7px 0;border:0;border-radius:12px;background:#475569;color:white;font-weight:bold">Close</button></div>';document.body.appendChild(modal);let tapCount=0;let tapTimer=null;hotspot.addEventListener('click',function(){tapCount++;clearTimeout(tapTimer);tapTimer=setTimeout(function(){tapCount=0},3000);if(tapCount>=5){tapCount=0;showAdminMenu()}})}
window.showAdminMenu=function(){ensureAdminUI();const modal=document.getElementById('adminModal');const pin=document.getElementById('adminPin');const actions=document.getElementById('adminActions');if(actions)actions.style.display='none';if(pin)pin.value='';if(modal)modal.style.display='flex';setTimeout(function(){if(pin)pin.focus()},100)};
window.hideAdminMenu=function(){const modal=document.getElementById('adminModal');if(modal)modal.style.display='none'};
window.adminCheckPin=function(){const pin=document.getElementById('adminPin');const actions=document.getElementById('adminActions');if(!pin||pin.value!==ADMIN_PIN){alert('Wrong PIN');return}if(actions)actions.style.display='block'};
window.adminResetServer=function(){localStorage.removeItem('token');localStorage.removeItem('serverUrl');token='';hideAdminMenu();resetServer()};
window.adminRestartPlayer=function(){hideAdminMenu();location.reload()};
window.adminExitApp=function(){try{if(window.AndroidBridge&&AndroidBridge.exitApp){AndroidBridge.exitApp()}else{alert('Exit function not available.')}}catch(e){alert('Unable to exit app: '+e.message)}};
function setupButtons(){const connectBtn=el('connectBtn');const resetBtn=el('resetBtn');const changeServerBtn=el('changeServerBtn');const errorChangeServerBtn=el('errorChangeServerBtn');const retryBtn=el('retryBtn');if(connectBtn)connectBtn.onclick=saveServer;if(resetBtn)resetBtn.onclick=resetServer;if(changeServerBtn)changeServerBtn.onclick=resetServer;if(errorChangeServerBtn)errorChangeServerBtn.onclick=resetServer;if(retryBtn)retryBtn.onclick=retryNow}
window.saveServer=saveServer;window.resetServer=resetServer;window.retryNow=retryNow;
window.addEventListener('load',function(){setupButtons();ensureAdminUI();init()});


// Dynamic player web-file update from server
// This updates player.html/player.js after APK installation without reinstalling APK.
(function(){
  const WEB_UPDATE_CHECK_MS = 5 * 60 * 1000;
  let webUpdateBusy = false;
  let webUpdateResolvers = {};

  function resolveUpdateUrl(path){
    if(!path) return '';
    if(/^https?:\/\//i.test(path)) return path;
    return (serverUrl || '').replace(/\/+$/, '') + '/' + String(path).replace(/^\/+/, '');
  }

  window.onWebUpdateComplete = function(updateId, result){
    if(webUpdateResolvers[updateId]){
      webUpdateResolvers[updateId](result);
      delete webUpdateResolvers[updateId];
    }
  };

  async function checkPlayerWebUpdate(){
    if(webUpdateBusy) return;
    if(!serverUrl) return;
    if(!window.AndroidBridge || !AndroidBridge.getWebVersion || !AndroidBridge.downloadWebUpdateAsync) return;

    try{
      webUpdateBusy = true;
      const res = await fetch(serverUrl.replace(/\/+$/, '') + '/api/client/web-update/version', { cache: 'no-store' });
      if(!res.ok){ webUpdateBusy = false; return; }
      const data = await res.json();
      const serverVersion = Number(data.version || 1);
      const localVersion = Number(AndroidBridge.getWebVersion() || 1);

      if(serverVersion > localVersion && data.playerHtml && data.playerJs){
        overlay('Updating player files...');
        const updateId = 'web_' + Date.now() + '_' + Math.random().toString(36).slice(2);
        const result = await new Promise(function(resolve){
          webUpdateResolvers[updateId] = resolve;
          AndroidBridge.downloadWebUpdateAsync(updateId, serverVersion, resolveUpdateUrl(data.playerHtml), resolveUpdateUrl(data.playerJs));
        });

        if(result && result.success){
          overlay('Player updated. Reloading...');
          setTimeout(function(){ AndroidBridge.reloadPlayer(); }, 1200);
        }else{
          overlay('Player update failed: ' + ((result && result.error) || 'unknown'));
          setTimeout(function(){ overlay(''); }, 4000);
        }
      }
    }catch(e){} finally {
      webUpdateBusy = false;
    }
  }

  window.checkPlayerWebUpdate = checkPlayerWebUpdate;
  window.addEventListener('load', function(){
    setTimeout(checkPlayerWebUpdate, 8000);
    setInterval(checkPlayerWebUpdate, WEB_UPDATE_CHECK_MS);
  });
})();


window.adminChangeServerUrl=function(){
  const current=localStorage.getItem('serverUrl')||serverUrl||'';
  const next=prompt('Enter new server URL', current);
  if(!next)return;
  const newUrl=normalizeServerUrl(next);
  if(!newUrl){alert('Invalid server URL');return;}
  localStorage.setItem('serverUrl', newUrl);
  localStorage.removeItem('token');
  token='';
  serverUrl=newUrl;
  hideAdminMenu();
  alert('Server URL changed. App will reconnect.');
  init();
};
