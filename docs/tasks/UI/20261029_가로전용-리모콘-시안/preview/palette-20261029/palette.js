/* Palette preview only. Data comes from the same approved, local-only portrait prototype. */
'use strict';
const paletteDefinitions = {
 A:{base:'#181919',background:'radial-gradient(ellipse 110% 52% at 0% 2%,#40747060,#3b6e6812 55%,transparent 100%),radial-gradient(ellipse 118% 53% at 108% 46%,#80465480,#6a3d4920 55%,transparent 100%),radial-gradient(ellipse 110% 50% at 42% 96%,#44787070,#3c6c6516 55%,transparent 100%),#181919',dialog:['#2c3b37e8','#302d32e8']},
 B:{base:'#222123',background:'radial-gradient(ellipse 130% 64% at 0% 4%,rgba(104,160,153,.34),transparent 63%),radial-gradient(ellipse 145% 65% at 106% 43%,rgba(168,94,118,.44),transparent 65%),radial-gradient(ellipse 130% 60% at 40% 103%,rgba(97,155,141,.34),transparent 66%),#222123',glass:['#d6c8ce20','#302c3340','#1d20214d'],border:'#c5c5c466',reflection:'#f0e8ec2a',dialog:['#3a3e3de8','#43373fe8']},
 C:{base:'#262127',background:'radial-gradient(ellipse 140% 68% at -3% 3%,rgba(92,146,140,.30),transparent 65%),radial-gradient(ellipse 155% 78% at 106% 38%,rgba(187,88,113,.56),transparent 70%),radial-gradient(ellipse 145% 68% at 30% 104%,rgba(116,160,145,.35),transparent 69%),#262127',glass:['#d9bcc825','#30293247','#201f2652'],border:'#d0bdc56b',reflection:'#ffeaf130',dialog:['#423b40e8','#4a3541e8']},
 D:{base:'#23232b',background:'radial-gradient(ellipse 145% 70% at -2% 3%,rgba(110,128,171,.36),transparent 65%),radial-gradient(ellipse 150% 78% at 106% 42%,rgba(174,108,132,.46),transparent 70%),radial-gradient(ellipse 135% 67% at 38% 104%,rgba(139,119,170,.32),transparent 68%),#23232b',glass:['#d4c9e123','#2c2b3745','#22212d50'],border:'#c6bfd66b',reflection:'#f4ecff2c',dialog:['#3b3c48e8','#443b4be8']}
};
const chosenVariant = new URLSearchParams(location.search).get('variant') || 'A';
const chosenPalette = paletteDefinitions[chosenVariant] || paletteDefinitions.A;
document.body.dataset.palette = chosenVariant;
const paletteStyle = document.createElement('style');
paletteStyle.textContent = `body{background:${chosenPalette.base}}.ambient{background:${chosenPalette.background}}.overlay{background:#00000024;backdrop-filter:blur(8px)}.dialog{background:linear-gradient(140deg,${chosenPalette.dialog[0]},${chosenPalette.dialog[1]});backdrop-filter:blur(8px) saturate(110%);border-color:#e8dfe75c;box-shadow:inset 0 1px #fff4,0 12px 30px #0006}`;
if(chosenPalette.glass)paletteStyle.textContent += `.glass{background:linear-gradient(180deg,${chosenPalette.glass[0]},${chosenPalette.glass[1]} 48%,${chosenPalette.glass[2]});border-color:${chosenPalette.border};box-shadow:inset 0 1px ${chosenPalette.reflection},0 3px 9px #00000012}.devicebar{background:linear-gradient(180deg,${chosenPalette.glass[0]},#181b1b20)}.navigation{background:linear-gradient(180deg,${chosenPalette.glass[0]},${chosenPalette.glass[2]})}`;
document.head.append(paletteStyle);
if(new URLSearchParams(location.search).get('dialog') === '1') {
 dialog('맵·사용자 공장 복원','<p class="warning">전체 사용자 슬롯의 등록 정보·설정·맵을 제조 기본값으로 덮어씁니다.</p><p>진행하시겠습니까?</p>',button('취소','close')+button('진행','confirm-reset','danger'));
}
