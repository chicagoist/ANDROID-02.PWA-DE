#!/usr/bin/env node
//
// tools/translate-builtin.js — programmatic DE→RU translation of empty
// dialog[*].russian fields in lessons.json, via translate.googleapis.com
// gtx_ endpoint (free public mirror, no key required, but rate-limited).
//
// Usage:
//   node tools/translate-builtin.js                # preview only
//   node tools/translate-builtin.js --apply        # overwrite lessons.json
//   node tools/translate-builtin.js --apply --rpm=180   # gentler throttle
//
// Honest disclaimer:
//   - gtx_ is a non-public Google endpoint; fine for development, NOT for
//     production sign-off on Play Store.
//   - Each translated line is suffixed with "[auto]" so you can easily grep
//     and audit. Replace with curated translations later if you want.
//   - Network blips → the offending phrase is logged + skipped; rerun the
//     script and the cache fills in the missing ones.

'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const args = process.argv.slice(2);
const apply = args.includes('--apply');
const rpm = parseInt((args.find(a => a.startsWith('--rpm=')) || '--rpm=240').split('=')[1], 10);
const throttleMs = Math.max(50, Math.round(60000 / rpm));
const dryRun = args.includes('--dry-run');

const ROOT = path.resolve(__dirname, '..');
const LESSONS_PATH = path.join(ROOT, 'app', 'src', 'main', 'assets', 'lessons.json');
const OUT_DIR  = path.join(__dirname, 'build');
const OUT_PATH = path.join(OUT_DIR, 'lessons.ru.json');
const CACHE_PATH = path.join(OUT_DIR, 'translation-cache.json');

const SOURCE = 'googletranslate-gtx-free';

/**
 * Translate one DE string via the gtx_ endpoint. Returns null on any failure
 * (HTTP error, JSON shape mismatch, empty body). Backoff-friendly: caller
 * handles 429 by sleeping longer.
 */
async function fetchTranslation(deText, attempt = 1) {
  const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=de&tl=ru&dt=t&q=${encodeURIComponent(deText)}`;
  let res;
  try {
    res = await fetch(url, { headers: { 'User-Agent': 'JustGermanImporter/1.0' } });
  } catch (e) {
    if (attempt < 3) {
      await sleep(800 * attempt);
      return fetchTranslation(deText, attempt + 1);
    }
    throw new Error(`network: ${e.message}`);
  }
  if (res.status === 429 && attempt < 5) {
    await sleep(5000 * attempt);
    return fetchTranslation(deText, attempt + 1);
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const json = await res.json();
  // gtx shape: [ [ ["translation", "source", null, null, 10], ... ] ]
  if (!Array.isArray(json) || !Array.isArray(json[0])) return null;
  const parts = json[0]
    .filter(chunk => Array.isArray(chunk) && typeof chunk[0] === 'string')
    .map(chunk => chunk[0]);
  return parts.join('').trim() || null;
}

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

function sha1Key(s) {
  return crypto.createHash('sha1').update(s).digest('hex').slice(0, 16);
}

function loadCache() {
  if (!fs.existsSync(CACHE_PATH)) return {};
  try { return JSON.parse(fs.readFileSync(CACHE_PATH, 'utf8')); }
  catch { return {}; }
}
function saveCache(cache) {
  if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });
  fs.writeFileSync(CACHE_PATH, JSON.stringify(cache, null, 2), 'utf8');
}

function collectEmptyUnique(lessons) {
  const unique = new Set();
  let emptyLines = 0;
  for (const lesson of lessons.lessons) {
    for (const line of (lesson.dialog || [])) {
      if (!line.german || !line.german.trim()) continue;
      if (line.russian && line.russian.trim()) continue;
      emptyLines++;
      unique.add(line.german.trim());
    }
  }
  return { unique: [...unique], emptyLines };
}

async function main() {
  if (dryRun) {
    const lessons = JSON.parse(fs.readFileSync(LESSONS_PATH, 'utf8'));
    const { unique, emptyLines } = collectEmptyUnique(lessons);
    console.log(`empty lines: ${emptyLines}`);
    console.log(`unique phrases: ${unique.size}`);
    console.log(`estimated time @ ${rpm} rpm: ${Math.round(unique.length * 60000 / rpm / 1000)}s`);
    return;
  }

  const lessons = JSON.parse(fs.readFileSync(LESSONS_PATH, 'utf8'));
  const cache = loadCache();
  const { unique, emptyLines } = collectEmptyUnique(lessons);
  console.log(`empty dialog lines: ${emptyLines}; unique DE phrases: ${unique.size}; rpm=${rpm}`);

  let translated = 0, cached = 0, failed = 0;
  const failedPhrases = [];

  for (const phrase of unique) {
    const key = sha1Key(phrase);
    if (cache[key]) {
      cached++;
      continue;
    }
    try {
      const ru = await fetchTranslation(phrase);
      if (ru) {
        cache[key] = { de: phrase, ru, source: SOURCE, at: new Date().toISOString() };
        translated++;
        if (translated <= 5 || translated % 25 === 0) {
          console.log(`  [${translated + cached}/${unique.size}] ${phrase}  →  ${ru}`);
        }
      } else {
        failed++;
        failedPhrases.push(phrase);
      }
    } catch (e) {
      failed++;
      failedPhrases.push(phrase);
      console.error(`  ! ${phrase.slice(0, 40)} …  ${e.message}`);
    }
    saveCache(cache);
    await sleep(throttleMs);
  }

  console.log(`\nDone: translated=${translated} cached=${cached} failed=${failed}/${unique.size}`);
  if (failedPhrases.length) {
    console.log(`First failures:\n  ${failedPhrases.slice(0, 5).map(p => '— ' + p).join('\n  ')}`);
  }

  // Apply translations to lessons.json.
  let filled = 0;
  for (const lesson of lessons.lessons) {
    for (const line of (lesson.dialog || [])) {
      if (!line.german || !line.german.trim()) continue;
      if (line.russian && line.russian.trim()) continue;
      const key = sha1Key(line.german.trim());
      const entry = cache[key];
      if (entry && entry.ru) {
        // Mark with [auto] so anyone reading the data knows it's machine output.
        line.russian = entry.ru + ' [auto]';
        filled++;
      }
    }
  }
  if (!lessons.metadata) lessons.metadata = {};
  lessons.metadata.lastAutoTranslation = {
    source: SOURCE,
    rpm,
    throttleMs,
    at: new Date().toISOString(),
    filled,
    failed,
    notice: 'machine-translated via googletranslate gtx_; verify before production release'
  };

  if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });
  fs.writeFileSync(OUT_PATH, JSON.stringify(lessons, null, 2), 'utf8');
  console.log(`Preview written: ${OUT_PATH}`);

  if (apply) {
    fs.writeFileSync(LESSONS_PATH, JSON.stringify(lessons, null, 2), 'utf8');
    console.log(`Applied: ${LESSONS_PATH} updated. ${filled} dialog lines now have Russian translations.`);
    console.log('Next steps:');
    console.log('  1. ./gradlew.bat assembleDebug');
    console.log('  2. adb install -r app/build/outputs/apk/debug/app-debug.apk');
    console.log('  3. Spot-check lessons on device — translations are machine output.');
  } else {
    console.log('Source JSON NOT modified. Re-run with --apply after review.');
  }
}

main().catch(e => { console.error('FATAL:', e); process.exit(1); });
