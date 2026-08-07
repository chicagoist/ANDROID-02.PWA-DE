const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const PDF = process.argv[2];
if (!PDF) { console.error('Usage: node analyze-pdf.js <path-to.pdf>'); process.exit(1); }

const tmpTxt = path.join(path.dirname(PDF), '_assimil_pages.txt');
execSync(`pdftotext "${PDF}" "${tmpTxt}"`, { stdio: 'inherit' });

const txt = fs.readFileSync(tmpTxt, 'utf8');
const pages = txt.split('\f');
console.log('Total pages (text-split):', pages.length);

// German ordinal + (N.) LEKTION marker. Ordinals vary (ERSTE, ZWEITE, ..., HUNDERTSTE)
const re = /\b[A-ZÄÖÜ]+\s*\((\d{1,3})\)\.?\s*LEKTION/g;

const found = [];
for (let i = 0; i < pages.length; i++) {
  const matches = [...pages[i].matchAll(re)];
  if (matches.length) {
    const lessons = matches.map(m => parseInt(m[1], 10));
    found.push({ pdfPage: i, lessons });
  }
}

console.log('Pages containing (N.) LEKTION markers:', found.length);
const lessonToPage = {};
for (const f of found) {
  for (const l of f.lessons) lessonToPage[l] = f.pdfPage;
  console.log(`PDF page ${String(f.pdfPage).padStart(3)} -> lesson(s): ${f.lessons.join(', ')}`);
}

console.log('\n--- Lesson -> PDF page map ---');
const lessons = Object.keys(lessonToPage).map(Number).sort((a, b) => a - b);
for (const l of lessons) {
  console.log(`lesson ${String(l).padStart(3)} -> PDF page ${lessonToPage[l]}`);
}
console.log('Total lessons found:', lessons.length);

// gaps check
const missing = [];
for (let l = 1; l <= 100; l++) if (!(l in lessonToPage)) missing.push(l);
console.log('Missing lessons 1-100:', missing.length ? missing.join(',') : 'none');

// sanity: context around each marker
console.log('\n--- context (first 60 chars of marker pages, first 8) ---');
for (const f of found.slice(0, 8)) {
  const clean = pages[f.pdfPage].replace(/\s+/g, ' ').slice(0, 60);
  console.log(`p${f.pdfPage}: ${clean}`);
}

fs.unlinkSync(tmpTxt);
