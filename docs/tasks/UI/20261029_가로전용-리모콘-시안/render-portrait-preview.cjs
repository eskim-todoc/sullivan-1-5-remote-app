const {chromium}=require('C:/Users/EUNSU/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const {pathToFileURL}=require('node:url');
const fs=require('node:fs');const path=require('node:path');
(async()=>{
 const target=path.join(__dirname,'preview','portrait');
 const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'});
 const context=await browser.newContext({viewport:{width:411,height:781},deviceScaleFactor:2});
 const page=await context.newPage(),errors=[],results=[];
 page.on('pageerror',e=>errors.push(e.message));
 const base=pathToFileURL(path.join(target,'app.html')).href;
 const cases=[['remote','page=remote'],['advanced','page=advanced'],['more','page=more'],['users','page=register'],['devices','page=register&type=device'],['user-edit','page=register&edit=user'],['device-edit','page=register&edit=device'],['management','page=management'],['maps','page=management&section=maps'],['logs','page=logs'],['ota','page=ota'],['ff','page=ff']];
 for(const [name,query] of cases){
  await page.goto(base+'?'+query);await page.evaluate(()=>document.fonts.ready);
  await page.screenshot({path:path.join(target,'screens',name+'.png')});
  const result=await page.evaluate(()=>{const rect=e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height};};const visible=e=>e.getBoundingClientRect().width&&e.getBoundingClientRect().height;return {viewport:[innerWidth,innerHeight],main:rect(document.querySelector('main')),mainScrollHeight:document.querySelector('main').scrollHeight,navigation:rect(document.querySelector('.navigation')),overflow:[...document.querySelectorAll('main,.workspace,.summary,.metric-strip,.status-matrix,.record,.menu-line,.info-card,.edit-form,.ota-summary,.page-toolbar,.devicebar')].filter(visible).filter(e=>e.scrollWidth>e.clientWidth+1).map(e=>e.className),statusOverlap:[...document.querySelectorAll('.status-pair')].filter(e=>e.querySelector('dt').getBoundingClientRect().right>e.querySelector('dd').getBoundingClientRect().left-2).map(e=>e.textContent),fontLoaded:document.fonts.check('600 12px Remote')};});
  results.push({name,...result});
 }
 await page.goto(base+'?page=more');await page.click('[data-action="open-register:device"]');
 const deviceNavigation=await page.locator('.record').first().textContent();
 await page.click('[data-action="record:device:0"]');await page.fill('#field-memo','초안 유지');
 const draftRetained=await page.inputValue('#field-memo')==='초안 유지';
 await page.goto(base+'?page=logs');await page.click('[data-action="log-filter:오류"]');const errorCount=await page.locator('.log-row').count();
 await page.click('[data-action="log-filter:전체"]');await page.fill('#log-query','Volume');const searchCount=await page.locator('.log-row').count();
 await page.setViewportSize({width:360,height:720});await page.goto(base+'?page=remote');await page.evaluate(()=>document.fonts.ready);
 const narrowOverflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth||document.querySelector('main').scrollWidth>document.querySelector('main').clientWidth+1);
 const result={errors,results,interactions:{deviceNavigation:deviceNavigation.includes('FFFF'),draftRetained,errorCount,searchCount,narrowOverflow}};
 fs.writeFileSync(path.join(__dirname,'portrait-preview-validation.json'),JSON.stringify(result,null,2));
 console.log(JSON.stringify({errors,issues:results.filter(r=>r.overflow.length||r.statusOverlap.length||!r.fontLoaded),interactions:result.interactions},null,2));
 await context.close();await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});
