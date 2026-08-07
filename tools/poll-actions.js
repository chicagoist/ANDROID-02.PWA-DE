// Polls the GitHub Actions workflow run for the latest push on main.
const https = require('https');
const REPO = 'chicagoist/ANDROID-02.PWA-DE';
const MAX_POLLS = 40; // ~20 min at 30s intervals
const SLEEP_MS = 30000;

function get(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'node' } }, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => {
        try { resolve(JSON.parse(d)); } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}
const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  for (let i = 1; i <= MAX_POLLS; i++) {
    const data = await get(`https://api.github.com/repos/${REPO}/actions/runs?branch=main&per_page=1`);
    const run = data.workflow_runs && data.workflow_runs[0];
    if (!run) { console.log(`poll ${i}: no run yet`); await sleep(SLEEP_MS); continue; }
    console.log(`poll ${i}: sha=${run.head_sha.slice(0, 7)} status=${run.status} conclusion=${run.conclusion || '-'}`);
    console.log(`  ${run.html_url}`);
    if (run.status === 'completed') {
      console.log('FINAL_CONCLUSION:', run.conclusion);
      process.exit(0);
    }
    await sleep(SLEEP_MS);
  }
  console.log('TIMEOUT: run still not completed');
  process.exit(1);
})();
