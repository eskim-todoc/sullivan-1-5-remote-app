const fs=require('fs'),path=require('path');const p=path.join(__dirname,'preview/portrait/app.js');let s=fs.readFileSync(p,'utf8');
s=s.replace(/^(function registration\(\).*)$/m,line=>line.replace("+button('+ 새 등록','new-record','tonal')",'').replace("+'</div>';}\n", "+'</div>'; }\n").replace("+'</div>';}","+'</div><div class=\"management-actions\">'+button('+ 새 등록','new-record','tonal')+'</div>';}"));
s=s.replace(/^(function editing\(\).*)$/m,line=>line.replace("${button('삭제…','delete-record','flat')}${button('저장','save-record','primary')}",'').replace('</p></form>', '</p></form><div class="management-actions">${button(\'삭제…\',\'delete-record\',\'flat\')}${button(\'저장\',\'save-record\',\'primary\')}</div>'));
s=s.replace(/^(function logs\(\).*)$/m,line=>line.replace("+button('내보내기','export','tonal')",'').replace("+logRows()+'</div>';}","+logRows()+'</div><div class=\"management-actions\">'+button('내보내기','export','tonal')+'</div>';}"));
fs.writeFileSync(p,s);
