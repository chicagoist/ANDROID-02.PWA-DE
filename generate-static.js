const fs = require('fs');
const path = require('path');
const http = require('http');
const { spawn } = require('child_process');

const BASE = 'http://localhost:3456';
const OUT = path.join(__dirname, 'www');
const SRC_PUBLIC = path.join('C:', 'Projects', '02.PWA-DE', 'public');
const SRC_NEXT = path.join('C:', 'Projects', '02.PWA-DE', '.next');

function mkdir(p) {
  if (!fs.existsSync(p)) fs.mkdirSync(p, { recursive: true });
}

function copyDir(src, dest) {
  if (!fs.existsSync(src)) return;
  mkdir(dest);
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, entry.name);
    const d = path.join(dest, entry.name);
    if (entry.isDirectory()) copyDir(s, d);
    else fs.copyFileSync(s, d);
  }
}

function fetch(pathname) {
  return new Promise((resolve, reject) => {
    const req = http.get(`${BASE}${pathname}`, (res) => {
      let data = '';
      res.setEncoding('utf8');
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, body: data }));
    });
    req.on('error', reject);
    req.setTimeout(30000, () => { req.destroy(); reject(new Error('timeout')); });
  });
}

async function savePage(pathname, outPath) {
  const { status, body } = await fetch(pathname);
  if (status !== 200) {
    console.warn(`  skip ${pathname} -> HTTP ${status}`);
    return false;
  }
  mkdir(path.dirname(outPath));
  fs.writeFileSync(outPath, body, 'utf8');
  console.log(`  saved ${pathname} -> ${outPath}`);
  return true;
}

async function main() {
  mkdir(OUT);

  // Copy static assets
  console.log('Copying public/...');
  copyDir(SRC_PUBLIC, OUT);

  console.log('Copying .next/static/...');
  copyDir(path.join(SRC_NEXT, 'static'), path.join(OUT, '_next', 'static'));

  // Home
  console.log('Fetching home...');
  await savePage('/', path.join(OUT, 'index.html'));

  // Dynamic routes
  const routes = [];
  for (let i = 1; i <= 100; i++) routes.push({ url: `/lesson/${i}`, dir: path.join('lesson', String(i), 'index.html') });
  for (let i = 1; i <= 12; i++) routes.push({ url: `/quiz/${i}`, dir: path.join('quiz', String(i), 'index.html') });
  for (let i = 1; i <= 12; i++) routes.push({ url: `/week/${i}`, dir: path.join('week', String(i), 'index.html') });

  console.log(`Fetching ${routes.length} dynamic routes...`);
  for (const r of routes) {
    await savePage(r.url, path.join(OUT, r.dir));
  }

  console.log('Done.');
}

main().catch(err => { console.error(err); process.exit(1); });
