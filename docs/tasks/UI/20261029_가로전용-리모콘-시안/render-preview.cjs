const {chromium}=require('C:/Users/EUNSU/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path=require('node:path');const fs=require('node:fs');const {pathToFileURL}=require('node:url');
(async()=>{
const root=__dirname, output=path.join(root,'preview','screens');
fs.mkdirSync(output,{recursive:true});
const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
const cases=[['remote','45'],['advanced','45'],['ff','45'],['ff','46'],['management','45'],['observation','45'],['register','45'],['ota','45'],['logs','45'],['disconnected','45']];
const results=[];
for(const [size,w,h,scale] of [['tablet',1200,760,1.5],['phone',868,411,2]]){
const context=await browser.newContext({viewport:{width:w,height:h},deviceScaleFactor:scale});const page=await context.newPage();
page.on('pageerror',e=>{results.push({error:e.message,size})});
for(const [name,fw] of cases){
 await page.goto(pathToFileURL(path.join(root,'preview','app.html')).href+`?page=${name}&fw=${fw}`);await page.evaluate(()=>document.fonts.ready);
 await page.screenshot({path:path.join(output,`${size}-${name}-${fw}.png`)});
 const audit=await page.evaluate(()=>({width:innerWidth,height:innerHeight,bodyOverflow:document.documentElement.scrollWidth>innerWidth,mainBounds:document.querySelector('main').getBoundingClientRect().toJSON(),workarea:document.querySelector('.workarea').getBoundingClientRect().toJSON(),horizontalOverflows:[...document.querySelectorAll('.panel,.status,.identity,.workarea')].filter(e=>e.scrollWidth>e.clientWidth+2).map(e=>({class:e.className,width:e.clientWidth,scroll:e.scrollWidth})),loadedFonts:document.fonts.check('500 14px Plex')&&document.fonts.check('20px Peace')}));
 results.push({size,page:name,fw,...audit});
}
// Additional reference states: device registration, search and destructive confirmation.
await page.goto(pathToFileURL(path.join(root,'preview','app.html')).href+'?page=register');await page.evaluate(()=>document.fonts.ready);await page.click('[data-action="register:device"]');await page.screenshot({path:path.join(output,`${size}-register-device.png`)});
for(const [name,pageName,modal] of [['search','disconnected','search'],['map-factory-confirm','management','map-factory'],['identity-serial','management','serial'],['identity-pairing','management','pairing'],['disconnect','remote','disconnect'],['error','remote','error']]){
 await page.goto(pathToFileURL(path.join(root,'preview','app.html')).href+`?page=${pageName}&modal=${modal}`);await page.evaluate(()=>document.fonts.ready);await page.screenshot({path:path.join(output,`${size}-${name}.png`)});
}
// Interaction and safety boundaries of the prototype.
await page.goto(pathToFileURL(path.join(root,'preview','app.html')).href+'?page=management&modal=map-factory');
const locked=await page.locator('[data-action="confirm-reset"]').isDisabled();await page.locator('#confirm-serial').fill('FFFF');const unlocked=await page.locator('[data-action="confirm-reset"]').isEnabled();await page.click('[data-action="confirm-reset"]');
await page.goto(pathToFileURL(path.join(root,'preview','app.html')).href+'?page=ff&fw=46');await page.click('[data-action="ff-mode"]');const gated=await page.locator('#modal').isHidden();await page.click('[data-action="ff-read"]');await page.click('[data-action="ff-mode"]');await page.check('input[name="ff"][value="3"]');await page.click('[data-action="ff-apply"]');const ff3=await page.evaluate(()=>preview.state.ff===3);
await page.goto(pathToFileURL(path.join(root,'preview','app.html')).href+'?page=remote');await page.click('[data-action="plus:volume"]');const volume=await page.evaluate(()=>preview.state.volume===6);
results.push({size,interactions:{destructiveLocked:locked,destructiveUnlockedAfterMatching:unlocked,ffInitiallyGated:gated,ff3,volume}});
await context.close();
}
fs.writeFileSync(path.join(root,'preview-validation.json'),JSON.stringify(results,null,2));await browser.close();console.log(JSON.stringify({screens:fs.readdirSync(output).length,audits:results.length,issues:results.filter(x=>x.error||x.bodyOverflow||x.horizontalOverflows?.length)},null,2));
})().catch(e=>{console.error(e);process.exit(1)});
