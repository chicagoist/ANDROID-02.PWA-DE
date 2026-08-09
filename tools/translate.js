#!/usr/bin/env node
//
// tools/translate.js — парсер русского издания Assimil для Android PWA.
//
// Зачем: lessons.json во всех 100 уроках имеет dialog[*].russian === "" (parse-lessons.js
// старой версии не вытаскивал русский диалог из исходного HTML). Парсер восполняет эти
// пробелы по опубликованному русскому изданию.
//
// Использование:
//   node tools/translate.js <input.txt> [--splitter=gutter|prefix] [--threshold=0.40] [--apply] [--selftest]
//
// Опции:
//   <input.txt>      Путь к текстовому файлу с параллельным DE/RU диалогом.
//                    PDF/DOCX конвертируются заранее внешним инструментом —
//                    pdftotext -layout для PDF, `pandoc -t plain` для DOCX.
//   --splitter       gutter (по умолчанию) — чередующиеся DE-строки и RU-строки.
//                    prefix                — явные префиксы "DE:" / "RU:".
//   --threshold      Минимальный similarity-балл для матча (по умолчанию 0.40).
//                    Подкрутите, если получаете слишком мало/много совпадений.
//   --apply          Записать обновления обратно в app/src/main/assets/lessons.json.
//                    Без этой опции пишется preview в tools/build/lessons.ru.json.
//   --selftest       Прогнать 4 пары DE/RU и выйти — для проверки пайплайна без PDF.
//
// Выход: tools/build/lessons.ru.json — preview (для ручной проверки перед --apply).

'use strict';

const fs = require('fs');
const path = require('path');

// ---------- CLI ----------
const args = process.argv.slice(2);
function opt(name, fallback) {
  const a = args.find(s => s.startsWith('--' + name + '='));
  return a ? a.split('=')[1] : fallback;
}
const inputPath = args.find(a => !a.startsWith('--'));
const splitterMode = opt('splitter', 'gutter');
const threshold   = parseFloat(opt('threshold', '0.40'));
const apply       = args.includes('--apply');
const selftest    = args.includes('--selftest');

const ROOT = path.resolve(__dirname, '..');
const LESSONS_PATH = path.join(ROOT, 'app', 'src', 'main', 'assets', 'lessons.json');
const OUT_DIR  = path.join(__dirname, 'build');
const OUT_PATH = path.join(OUT_DIR, 'lessons.ru.json');

// ---------- Эвристики ----------
function hasCyrillic(s) { return /[А-Яа-яЁё]/.test(s); }
function hasLatin(s)    { return /[A-Za-zÄÖÜäöüß]/.test(s); }

function isRussianLine(s) {
  if (!s) return false;
  const cyr = (s.match(/[А-Яа-яЁё]/g) || []).length;
  const lat = (s.match(/[A-Za-zÄÖÜäöüß]/g) || []).length;
  return cyr > 0 && cyr >= lat;
}
function isGermanLine(s) {
  if (!s) return false;
  const lat = (s.match(/[A-Za-zÄÖÜäöüß]/g) || []).length;
  const cyr = (s.match(/[А-Яа-яЁё]/g) || []).length;
  return lat > 0 && lat >= cyr;
}

// Strip a leading "DE:" / "RU:" / "RU - " / "Перевод:" prefix.
function stripPrefix(line) {
  return line.replace(/^\s*(DE|RU|ПЕРЕВОД|ORIG|TRANSLATION)\b\s*[:\-–—]?\s*/i, '').trim();
}

// ---------- Сплиттеры ----------
/**
 * Gutter: walk lines; whenever we see a German line, hold it. The next
 * Russian line within 6 lines pairs with the held German. Tolerates short
 * head/tail inserts (page numbers, mixed garbage).
 */
function splitGutter(text) {
  const pairs = [];
  let held = null;
  let lookahead = 0;
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line) continue;
    if (isGermanLine(line)) {
      if (held) pairs.push([held, '']);
      held = line;
      lookahead = 0;
    } else if (isRussianLine(line) && held) {
      pairs.push([held, line]);
      held = null;
      lookahead = 0;
    } else if (held) {
      lookahead++;
      if (lookahead > 6) {
        pairs.push([held, '']);
        held = null;
      }
    }
  }
  if (held) pairs.push([held, '']);
  return pairs;
}

/**
 * Prefix: only lines clearly tagged as DE: / RU: (case-insensitive,
 * full/half-width colon or dash accepted). Mixed lines without a tag
 * are treated as German if mostly Latin, otherwise Russian.
 */
function splitPrefix(text) {
  const pairs = [];
  let held = null;
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line) continue;
    const isDe = /^\s*DE\b\s*[:\-–—]/i.test(line);
    const isRu = /^\s*RU\b\s*[:\-–—]/i.test(line);
    if (isDe) {
      if (held) pairs.push([held, '']);
      held = stripPrefix(line);
    } else if (isRu) {
      if (held) {
        pairs.push([held, stripPrefix(line)]);
        held = null;
      }
    } else if (isGermanLine(line)) {
      if (held) pairs.push([held, '']);
      held = line;
    } else if (isRussianLine(line) && held) {
      pairs.push([held, line]);
      held = null;
    }
  }
  if (held) pairs.push([held, '']);
  return pairs;
}

// ---------- Нормализация и сопоставление ----------
/**
 * Lowercase FIRST, then drop punctuation + transliterate umlauts + ss.
 * Case-fold first means "Ich" and "ICH" both yield "ich" — prevents
 * different capitalisation in the PDF source from breaking Jaccard
 * against the lessons.json target. Umlauts expand to ae/oe/ue/ss after
 * case-folding, so no separate uppercase rules are needed.
 */
function normalize(s) {
  return s
    .toLowerCase()
    .replace(/[„“‚‘»«…—–\]\[\(\)"\\'`!?.,;:₀₁₂₃₄₅₆₇₈₉0-9]/g, '')
    .replace(/ä/g, 'ae')
    .replace(/ö/g, 'oe')
    .replace(/ü/g, 'ue')
    .replace(/ß/g, 'ss')
    .replace(/\s+/g, ' ')
    .trim();
}
function tokens(s) {
  return normalize(s).split(' ').filter(Boolean);
}
function jaccard(a, b) {
  const ta = tokens(a);
  const tb = tokens(b);
  if (!ta.length || !tb.length) return 0;
  const setA = new Set(ta), setB = new Set(tb);
  let inter = 0;
  for (const t of setA) if (setB.has(t)) inter++;
  const uni = new Set([...ta, ...tb]).size;
  return uni ? inter / uni : 0;
}
/**
 * Similarity = Jaccard + smaller substring bonus.
 * First-word and diacritic bonuses were removed — they double-counted
 * signals already covered by Jaccard, and could amplify wrong-pair noise
 * (e.g., re-ordered speakers in PDF causing answer/question swap).
 */
function similarity(a, b) {
  let score = jaccard(a, b);
  const na = normalize(a);
  const nb = normalize(b);
  if (na && nb && (na.includes(nb) || nb.includes(na))) score += 0.10;
  return Math.min(1, score);
}

/**
 * Returns top two best matches. Required so the caller can require the
 * delta between #1 and #2 is at least MARGIN — rejects ambiguous pairs
 * (e.g., two near-identical German phrases with different Russian).
 */
function topTwoPairs(targetGerman, pairs) {
  let top = { score: -1, ru: '' }, second = { score: -1, ru: '' };
  for (const [de, ru] of pairs) {
    if (!ru) continue;
    const s = similarity(targetGerman, de);
    if (s > top.score) {
      second = top;
      top = { score: s, ru };
    } else if (s > second.score) {
      second = { score: s, ru };
    }
  }
  return [top, second];
}

// ---------- Основной пайплайн ----------
/**
 * Strict mutation allow-list. Only dialog lines' russian field is rewritten.
 * If you need to touch a new field, add it here and at the call site.
 * Never use Object.assign / spread anywhere else — it would silently
 * expand the mutation surface and break the "vocabulary / grammar /
 * mistakes / tips untouched" guarantee.
 */
function setRussianOnDialogLine(dialogLine, value) {
  if (!dialogLine || typeof dialogLine !== 'object') return false;
  if (!('russian' in dialogLine)) return false;
  dialogLine.russian = String(value);
  return true;
}

const DELTA_MARGIN = 0.05;

function fillLesson(lesson, pairs) {
  let filled = 0, considered = 0, skipped = 0;
  for (const line of (lesson.dialog || [])) {
    if (!line.german || !line.german.trim()) continue;
    if (line.russian && line.russian.trim().length > 0) continue;
    considered++;
    const [top, second] = topTwoPairs(line.german, pairs);
    if (top.score >= threshold && (top.score - second.score) >= DELTA_MARGIN && top.ru) {
      if (setRussianOnDialogLine(line, top.ru)) filled++;
    } else {
      skipped++;
    }
  }
  return { filled, considered, skipped };
}

function runReal(inputPath) {
  if (!fs.existsSync(inputPath)) {
    console.error(`Input not found: ${inputPath}`);
    process.exit(3);
  }
  const text = fs.readFileSync(inputPath, 'utf8');
  const splitterFn = splitterMode === 'prefix' ? splitPrefix : splitGutter;
  const pairs = splitterFn(text);
  const withRu = pairs.filter(p => p[1]).length;
  console.log(`splitter=${splitterMode}; lines=${pairs.length}; ${withRu} candidates with RU.`);

  const lessons = JSON.parse(fs.readFileSync(LESSONS_PATH, 'utf8'));
  let totalFilled = 0, totalConsidered = 0, totalSkipped = 0;
  for (const lesson of lessons.lessons) {
    const { filled, considered, skipped } = fillLesson(lesson, pairs);
    totalFilled += filled;
    totalConsidered += considered;
    totalSkipped += skipped;
  }
  console.log(`Matched: ${totalFilled} of ${totalConsidered} empty dialog lines` +
              (totalSkipped ? ` (${totalSkipped} skipped due to threshold/margin)` : '') +
              ` (threshold=${threshold})`);

  if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });
  fs.writeFileSync(OUT_PATH, JSON.stringify(lessons, null, 2), 'utf8');
  console.log(`Preview written: ${OUT_PATH}`);

  if (apply) {
    fs.writeFileSync(LESSONS_PATH, JSON.stringify(lessons, null, 2), 'utf8');
    console.log(`Applied: ${LESSONS_PATH} updated with augmented dialog[].russian.`);
    console.log('Now rebuild the APK (./gradlew assembleDebug) and reinstall.');
  } else {
    console.log('Source JSON NOT modified. Re-run with --apply after manual review.');
    console.log('Tip: open tools/build/lessons.ru.json and grep "russian" near "german" to spot-check.');
  }
}

function runSelfTest() {
  const text = [
    'DE: Guten Tag! Was darf es sein?',
    'RU: Здравствуйте! Что будете заказывать?',
    'DE: Ich hätte gerne einen Kaffee.',
    'RU: Я бы хотел кофе.',
    'DE: Danke sehr!',
    'RU: Большое спасибо!',
    'DE: Auf Wiedersehen!',
    'RU: До свидания!'
  ].join('\n');

  const pairs = splitPrefix(text);
  if (pairs.length !== 4) throw new Error(`selftest splitPrefix length: got ${pairs.length}, want 4`);
  if (pairs[0][0] !== 'Guten Tag! Was darf es sein?') throw new Error('selftest pair[0] de wrong');
  if (pairs[0][1] !== 'Здравствуйте! Что будете заказывать?') throw new Error('selftest pair[0] ru wrong');
  if (pairs[3][1] !== 'До свидания!') throw new Error('selftest pair[3] ru wrong');

  const s = similarity('Ich hätte gerne einen Kaffee.', 'Ich hätte gerne einen Kaffee.');
  if (s < 0.95) throw new Error(`selftest similarity identical: ${s}`);
  const sNeg = similarity('Ich hätte gerne einen Kaffee.', 'der Kaffee ist gut.');
  if (sNeg > 0.4) throw new Error(`selftest similarity negative too high: ${sNeg}`);

  // Umlaut/Capital-sensitivity regression: after lowercase-first normalize,
  // "Mädchen" and "MÄDCHEN" should normalize identically AND match each other.
  const m1 = 'Mädchen';
  const m2 = 'MAEDCHEN';
  if (normalize(m1) !== 'maedchen') throw new Error('normalize Mädchen');
  if (normalize(m2) !== 'maedchen') throw new Error('normalize MÄDCHEN (uppercase AE) — needs lowercase-first');
  if (similarity(m1, m2) < 0.95) throw new Error(`umlaut/capital sim: ${similarity(m1, m2)}`);

  console.log(`selftest OK — ${pairs.length} pairs, similarity identical=${s.toFixed(2)}, different=${sNeg.toFixed(2)}, umlaut-cased=${similarity(m1, m2).toFixed(2)}`);
}

function main() {
  if (selftest) {
    runSelfTest();
    return;
  }
  if (!inputPath) {
    console.error('Usage: node tools/translate.js <input.txt> [--splitter=gutter|prefix] [--threshold=0.4] [--apply] [--selftest]');
    process.exit(2);
  }
  runReal(inputPath);
}

main();
