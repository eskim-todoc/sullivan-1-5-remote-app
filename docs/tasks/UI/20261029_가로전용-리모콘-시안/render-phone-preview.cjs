const {chromium}=require('C:/Users/EUNSU/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const {pathToFileURL}=require('node:url');
const fs=require('node:fs');
const path=require('node:path');
const root=__dirname;
const target=path.join(root,'preview','phone');
const screens=path.join(target,'screens');
const cases=[
 ['remote','page=remote'],['advanced','page=advanced'],['advanced-rel46','page=advanced&fw=46'],
 ['management','page=management'],['maps','page=management&section=maps'],['more','page=more'],
 ['register','page=register'],['register-edit','page=register&edit=user'],['device-edit','page=register&edit=device'],
 ['ota','page=ota'],['ota-recovery','page=ota&section=recovery'],['ff-unsupported','page=ff'],
 ['ff-read','page=ff&fw=46'],['observation','page=observation'],['logs','page=logs'],
 ['setting-inline','page=advanced&modal=setting:normal'],['map-confirm','page=management&section=maps&modal=map-factory'],
 ['serial','page=management&modal=serial'],['pairing','page=management&modal=pairing']
];
async function audit(page){return page.evaluate(()=>{
 const shown=e=>{const r=e.getBoundingClientRect();return r.width&&r.height&&getComputedStyle(e).visibility!=='hidden'&&!e.closest('[hidden]');};
 const box=e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height};};
 const overflow=[...document.querySelectorAll('.workspace,.summary,.status-group,.control-card,.audio-card,.special-panels,.inline-setting,.tile-grid,.info-card,.edit-form,.ota-summary,.empty-card,.ff-summary,.dialog,.dialog-body')].filter(shown).filter(e=>e.scrollHeight>e.clientHeight+2||e.scrollWidth>e.clientWidth+2).map(e=>({class:e.className,w:e.clientWidth,sw:e.scrollWidth,h:e.clientHeight,sh:e.scrollHeight}));
 const wrap=[...document.querySelectorAll('.nav-item>span:last-child,.step-label,.tile-title,.tile-value,.page-toolbar h1,.file-row button,.dialog-footer button')].filter(shown).filter(e=>e.scrollWidth>e.clientWidth+1).map(e=>e.textContent);
 const targets=[...document.querySelectorAll('button,input:not([type=radio]):not([readonly]),select')].filter(shown).filter(e=>!e.disabled);
 const smallTargets=targets.filter(e=>{const r=e.getBoundingClientRect();return r.height<31.5||r.width<31.5;}).map(e=>({text:e.getAttribute('aria-label')||e.textContent,box:box(e)}));
 const coreSmallTargets=[...document.querySelectorAll('.setting-button,.compact-step button,.control-toggles button,.inline-setting-actions button,.nav-item')].filter(shown).filter(e=>!e.disabled).filter(e=>{const r=e.getBoundingClientRect();return r.height<43.5||r.width<43.5;}).map(e=>e.textContent);
 const rows=[...document.querySelectorAll('.status-pair')].filter(shown);
 const statusOverlap=rows.filter(e=>{const a=e.querySelector('dt').getBoundingClientRect(),b=e.querySelector('dd').getBoundingClientRect(),r=e.getBoundingClientRect();return a.right>b.left-2||b.right>r.right+1;}).map(e=>e.textContent);
 const summary=document.querySelector('.summary'),sr=summary.getBoundingClientRect();
 const statusClipping=rows.filter(e=>{const r=e.getBoundingClientRect();return r.bottom>sr.bottom||r.top<sr.top||r.left<sr.left||r.right>sr.right;}).map(e=>e.textContent);
 const buttonFaces=[...document.querySelectorAll('.setting-button')].filter(shown).map(e=>{const p=getComputedStyle(e,'::before'),r=e.getBoundingClientRect();return {label:e.textContent,hitHeight:r.height,faceHeight:r.height-parseFloat(p.top)-parseFloat(p.bottom)};});
 return {width:innerWidth,height:innerHeight,workspace:box(document.querySelector('.workspace')),status:box(summary),visibleStatusRows:rows.length,statusOverlap,statusClipping,overflow,wrap,smallTargets,coreSmallTargets,buttonFaces,documentOverflow:document.documentElement.scrollWidth>innerWidth,fonts:document.fonts.check('500 13px Plex')};
 });}
(async()=>{
 fs.mkdirSync(screens,{recursive:true});
 const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 const results=[];
 const context=await browser.newContext({viewport:{width:828,height:411},deviceScaleFactor:2});
 const page=await context.newPage();const errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 const base=pathToFileURL(path.join(target,'app.html')).href;
 for(const [name,query] of cases){
  await page.goto(base+'?'+query);await page.evaluate(()=>document.fonts.ready);
  await page.screenshot({path:path.join(screens,name+'.png')});
  results.push({name,...await audit(page)});
 }
 await page.goto(base+'?page=ff&fw=46');
 const ffGated=await page.locator('[data-action^="ff-mode:"]').count()===0;
 await page.click('[data-action="ff-read"]');await page.click('[data-action="ff-mode:3"]');
 await page.screenshot({path:path.join(screens,'ff3.png')});results.push({name:'ff3',...await audit(page)});
 const ff3=await page.evaluate(()=>phonePreview.state.ff===3);
 await page.goto(base+'?page=ota');const otaLocked=await page.locator('[data-action="ota-start"]').isDisabled();
 await page.click('[data-action="file:CM3"]');await page.click('[data-action="file-selected:CM3"]');const otaReady=await page.locator('[data-action="ota-start"]').isEnabled();
 await page.screenshot({path:path.join(screens,'ota-ready.png')});results.push({name:'ota-ready',...await audit(page)});
 await page.selectOption('#target-slot','1');const runningSlotLocked=await page.locator('[data-action="ota-start"]').isDisabled();
 await page.goto(base+'?page=management&modal=map-factory');const resetLocked=await page.locator('[data-action="confirm-reset"]').isDisabled();await page.fill('#confirm-target','ABCD');const wrongLocked=await page.locator('[data-action="confirm-reset"]').isDisabled();await page.fill('#confirm-target','FFFF');const resetReady=await page.locator('[data-action="confirm-reset"]').isEnabled();
 await page.goto(base+'?page=remote');await page.click('[aria-label="볼륨 늘리기"]');const volume=await page.evaluate(()=>phonePreview.state.volume===6);const maxOutputLocked=await page.locator('[aria-label="최대출력 늘리기"]').isDisabled();
 await page.goto(base+'?page=register&edit=user');await page.fill('#field-name','홍길동 · 긴 사용자 이름 테스트');await page.click('[data-action="save-record"]');const formKept=await page.evaluate(()=>phonePreview.state.form.name==='홍길동 · 긴 사용자 이름 테스트');
 await page.goto(base+'?page=management&modal=pairing');const keyLocked=await page.locator('[data-action="identity-review:pairing"]').isDisabled();await page.fill('#identity-value','123456');await page.fill('#identity-again','123457');const mismatchLocked=await page.locator('[data-action="identity-review:pairing"]').isDisabled();await page.fill('#identity-again','123456');const keyReady=await page.locator('[data-action="identity-review:pairing"]').isEnabled();
 results.push({interactions:{ffGated,ff3,otaLocked,otaReady,runningSlotLocked,resetLocked,wrongLocked,resetReady,volume,maxOutputLocked,formKept,keyLocked,mismatchLocked,keyReady}});
 await page.goto(base+'?page=register');await page.click('[data-action="record:user:1"]');
 const identityStable=await page.locator('.person strong').textContent()==='TODOC_OTE · 왼쪽';
 await page.fill('#field-name','DRAFT');await page.click('[data-action="record-back"]');
 const draftDiscarded=await page.evaluate(()=>phonePreview.state.users[1].name==='KIM'&&phonePreview.state.identity.name==='TODOC_OTE');
 await page.goto(base+'?page=advanced');
 const statusBefore=await page.locator('.summary').boundingBox();
 const allControlsVisible=await page.locator('.setting-button').count()===11;
 const noStatusButton=await page.locator('[data-action="status"]').count()===0;
 await page.click('[data-action="setting:normal"]');await page.click('[data-action="number:1"]');
 const draftDoesNotApply=await page.locator('[data-status="normal"] dd').textContent()==='2.8 V';
 const noKeyboardForSettings=await page.locator('#setting-value').getAttribute('readonly')!==null;
 const statusWhileEditing=JSON.stringify(await page.locator('.summary').boundingBox())===JSON.stringify(statusBefore)&&await page.locator('.status-pair').count()===24&&await page.locator('#overlay').isHidden();
 await page.screenshot({path:path.join(screens,'setting-draft.png')});
 await page.click('[data-action="apply-setting:normal"]');
 const currentUpdated=await page.locator('[data-status="normal"] dd').textContent()==='2.825 V';
 await page.screenshot({path:path.join(screens,'setting-applied.png')});
 await page.click('[data-action="setting:normal"]');const settingRetained=await page.inputValue('#setting-value')==='2.825';
 await page.click('[data-action="editor-back"]');await page.click('[data-action="setting:fixed"]');await page.click('[data-action="choice:On"]');await page.click('[data-action="apply-setting:fixed"]');await page.click('[data-action="setting:fixed"]');const toggleRetained=await page.inputValue('#setting-value')==='On';
 await page.click('[data-action="choice:Off"]');await page.click('[data-action="editor-back"]');const cancelledDraft=await page.locator('[data-status="fixed"] dd').textContent()==='On';
 results.push({persistentStatus:{allControlsVisible,noStatusButton,draftDoesNotApply,noKeyboardForSettings,statusWhileEditing,currentUpdated,cancelledDraft}});
 await page.goto(base+'?page=ota');await page.click('[data-action="file:CM3"]');await page.click('[data-action="file-selected:CM3"]');await page.click('[data-action="ota-start"]');
 const otaEditingLocked=await page.locator('[data-action="file:CM3"]').isDisabled()&&await page.locator('#target-slot').isDisabled()&&await page.locator('[data-action="section:recovery"]').isDisabled();
 await page.screenshot({path:path.join(screens,'ota-progress.png')});await page.click('[data-action="ota-stop"]');const otaCanRestart=await page.locator('[data-action="ota-start"]').isEnabled();
 results.push({reviewFixes:{identityStable,draftDiscarded,settingRetained,toggleRetained,otaEditingLocked,otaCanRestart}});
 // Reduced-height input layout: actual Android keyboard is verified separately.
 await page.goto(base+'?page=register&edit=user');await page.focus('#field-name');await page.setViewportSize({width:828,height:230});await page.evaluate(()=>window.dispatchEvent(new Event('resize')));await page.screenshot({path:path.join(screens,'keyboard-layout.png')});results.push({name:'keyboard-layout',...await audit(page)});
 // Width stress with identical height; UI should not rely on the full physical display width.
 await page.setViewportSize({width:780,height:411});await page.goto(base+'?page=remote');await page.evaluate(()=>document.fonts.ready);results.push({name:'remote-width780',...await audit(page)});
 const output={errors,results};fs.writeFileSync(path.join(root,'phone-preview-validation.json'),JSON.stringify(output,null,2));
 const issues=results.filter(x=>x.name&&(x.documentOverflow||!x.fonts||x.wrap?.length||x.smallTargets?.length||x.coreSmallTargets?.length||x.statusOverlap?.length||x.statusClipping?.length||(x.name!=='keyboard-layout'&&(x.overflow?.length||x.visibleStatusRows!==24))));
 const interactions=results.filter(x=>x.interactions||x.reviewFixes||x.persistentStatus);
 console.log(JSON.stringify({errors,audits:results.length,issues,interactions},null,2));
 await browser.close();
 if(errors.length||issues.length||interactions.some(x=>Object.values(Object.values(x)[0]).some(v=>v!==true)))process.exitCode=1;
})().catch(e=>{console.error(e);process.exit(1);});
