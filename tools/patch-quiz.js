// Removes quizzes entirely from the Android static assets:
// 1) deletes the quiz/ pages, 2) strips quiz links from static HTML,
// 3) nulls out the quiz-link JSX in the hydration chunks so links
//    do NOT reappear after React hydrates.
const fs = require('fs');
const path = require('path');

const ROOT = 'C:/Projects/ANDROID-02.PWA-DE';
const WWW = path.join(ROOT, 'android/app/src/main/assets/www');

// --- 1. Delete quiz pages ------------------------------------------------
fs.rmSync(path.join(WWW, 'quiz'), { recursive: true, force: true });
console.log('deleted quiz/');

// --- 2. Strip quiz <a> blocks from static HTML ---------------------------
const htmlFiles = ['index.html'];
for (let w = 1; w <= 12; w++) htmlFiles.push('week/' + w + '/index.html');
const linkRe = /<a [^>]*href="\/quiz\/[^"]*"[^>]*>[\s\S]*?<\/a>/g;
let stripped = 0;
for (const f of htmlFiles) {
  const fp = path.join(WWW, f);
  if (!fs.existsSync(fp)) continue;
  const html = fs.readFileSync(fp, 'utf8');
  const out = html.replace(linkRe, () => { stripped++; return ''; });
  fs.writeFileSync(fp, out);
}
console.log('stripped quiz links from ' + stripped + ' html blocks');

// --- 3. Null out quiz-link JSX in hydration chunks -----------------------
const chunks = path.join(WWW, '_next/static/chunks');

// week page chunk: g&&(0,t.jsxs)(i.default,{href:`/quiz/${e}`,...}) -> g&&null
const weekFile = path.join(chunks, '1mwwsvf3kjwha.js');
let week = fs.readFileSync(weekFile, 'utf8');
const wStart = week.indexOf('(0,t.jsxs)(i.default,{href:`/quiz/${e}`');
const wEndMarker = 'children:"10 вопросов \u2192"})]})';
const wEnd = week.indexOf(wEndMarker, wStart);
if (wStart === -1 || wEnd === -1) {
  console.error('WEEK CHUNK PATTERN NOT FOUND - manual fix needed');
} else {
  const wEndPos = wEnd + wEndMarker.length;
  week = week.slice(0, wStart) + 'null' + week.slice(wEndPos);
  fs.writeFileSync(weekFile, week);
  console.log('patched week chunk');
}

// home page chunk: (0,t.jsx)(r.default,{href:"/quiz/1",...}) -> null
const homeFile = path.join(chunks, '3hnbn_zqb1tkk.js');
let home = fs.readFileSync(homeFile, 'utf8');
const hStart = home.indexOf('(0,t.jsx)(r.default,{href:"/quiz/1"');
const hEndMarker = 'children:"Квиз недели 1"})';
const hEnd = home.indexOf(hEndMarker, hStart);
if (hStart === -1 || hEnd === -1) {
  console.error('HOME CHUNK PATTERN NOT FOUND - manual fix needed');
} else {
  const hEndPos = hEnd + hEndMarker.length;
  home = home.slice(0, hStart) + 'null' + home.slice(hEndPos);
  fs.writeFileSync(homeFile, home);
  console.log('patched home chunk');
}

console.log('done');
