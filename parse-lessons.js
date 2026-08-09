const fs = require('fs');
const path = require('path');

// Helper to extract text content from HTML
function extractText(html, start, end) {
  const startIdx = html.indexOf(start);
  if (startIdx === -1) return '';
  const endIdx = html.indexOf(end, startIdx + start.length);
  if (endIdx === -1) return '';
  return html.substring(startIdx + start.length, endIdx).trim();
}

// Extract topics from subtitle
function extractTopics(html) {
  const topicsText = extractText(html, '<p class="text-xs text-zinc-600 text-center">', '</p>');
  if (!topicsText) return [];
  // Decode HTML entities (same as extractGrammar). Otherwise the apostrophe
  // in titles like "Wie geht's?" is stored as the literal "&#x27;" in the JSON,
  // which then renders verbatim on screen — verified Bug B.
  const decoded = topicsText
    .replace(/&#x27;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&');
  return decoded.split('·').map(t => t.trim()).filter(t => t.length > 0);
}

// Per-lesson title: use the first DE dialog line as the recognizable
// chapter heading (e.g. "Guten Tag!" for Lesson 1, "Ich habe großen Hunger"
// for Lesson 2). The source HTML has no discrete per-lesson <h1> — the
// only <h3>s are section labels like "Диалог урока" / "Грамматика". Assimil
// prints its chapter heading as the first dialogue line, which is
// short, unique, and recognisable in the UI. This fixes Bug A where the
// home/week/lesson screens rendered `lesson.topics` (week themes, shared
// by all 7 lessons of the week) as if it were the per-lesson title.
function extractLessonTitle(html) {
  const dialogSection = extractText(
    html,
    '<h3 class="text-sm font-semibold text-zinc-100">Диалог урока</h3>',
    '<div class="rounded-xl border border-zinc-800 bg-zinc-900/30 p-4"><button class="flex items-center justify-between w-full"><div class="flex items-center gap-2"><svg'
  );
  if (!dialogSection) return '';
  const match = dialogSection.match(
    /<p class="text-sm text-zinc-100 font-medium leading-relaxed">([^<]+)<\/p>/
  );
  if (!match) return '';
  return match[1].trim()
    .replace(/&#x27;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&');
}

// Extract audio path
function extractAudioPath(html) {
  const match = html.match(/<audio src="([^"]+)"/);
  return match ? match[1] : '';
}

// Extract dialog lines
function extractDialog(html) {
  const dialog = [];
  const dialogSection = extractText(html, '<h3 class="text-sm font-semibold text-zinc-100">Диалог урока</h3>', '<div class="rounded-xl border border-zinc-800 bg-zinc-900/30 p-4"><button class="flex items-center justify-between w-full"><div class="flex items-center gap-2"><svg');

  if (!dialogSection) return dialog;

  const lineRegex = /<p class="text-sm text-zinc-100 font-medium leading-relaxed">([^<]+)<\/p>\s*<p class="text-xs text-zinc-600 mt-0\.5 italic">\[<!--\s*-->([^<]+)<!--\s*-->\]<\/p>/g;
  let match;

  while ((match = lineRegex.exec(dialogSection)) !== null) {
    dialog.push({
      german: match[1].trim(),
      pronunciation: match[2].trim(),
      russian: ""
    });
  }

  return dialog;
}

// Extract vocabulary
function extractVocabulary(html) {
  const vocab = [];
  const vocabSection = extractText(html, '<h3 class="text-sm font-semibold text-zinc-100">Лексика урока</h3>', '</div></div><div class="rounded-xl border border-zinc-800 bg-zinc-900/30 p-4"><button class="flex items-center justify-between w-full"><div class="flex items-center gap-2"><svg');

  if (!vocabSection) return vocab;

  const itemRegex = /<span class="text-sm text-zinc-200 font-medium">([^<]+)<\/span>\s*<span class="text-xs text-zinc-500">([^<]+)<\/span>/g;
  let match;

  while ((match = itemRegex.exec(vocabSection)) !== null) {
    vocab.push({
      german: match[1].trim(),
      russian: match[2].trim()
    });
  }

  return vocab;
}

// Extract grammar note
function extractGrammar(html) {
  const grammarContentTag = '<p class="mt-3 text-sm text-zinc-300 leading-relaxed">';
  const grammarText = extractText(html, '<h3 class="text-sm font-semibold text-zinc-100">Грамматика</h3>', grammarContentTag);
  const contentEnd = html.indexOf('</p></div></div><div class="space-y-4">', html.indexOf(grammarText));
  if (contentEnd === -1) return '';

  const startIdx = html.indexOf(grammarContentTag);
  if (startIdx === -1) return '';

  // Use tag.length to skip exactly the opening HTML tag. A previous version
  // hardcoded +56 which silently ate the first 2 Cyrillic letters of every
  // grammar section (e.g. "Определённые" became "ределённые").
  let content = html.substring(startIdx + grammarContentTag.length, contentEnd).trim();
  // Decode HTML entities
  content = content.replace(/&#x27;/g, "'").replace(/&quot;/g, '"').replace(/&amp;/g, '&');

  return content;
}

// Extract lesson essence
function extractEssence(html) {
  const essenceContentTag = '<p class="text-sm text-zinc-300 leading-relaxed">';
  const essenceSection = extractText(html, '<h3 class="text-sm font-semibold text-zinc-100">Суть урока</h3>', essenceContentTag);
  const contentStart = html.indexOf(essenceContentTag, html.indexOf('Суть урока'));
  if (contentStart === -1) return '';

  const contentEnd = html.indexOf('</p></div><div class="rounded-xl border border-zinc-800 bg-zinc-900/30 p-4">', contentStart);
  if (contentEnd === -1) return '';

  // Same defense as extractGrammar: tag.length, not a hardcoded +50 (which
  // used to eat the first Cyrillic letter of every essence section).
  return html.substring(contentStart + essenceContentTag.length, contentEnd).trim();
}

// Extract list items (difficulties, mistakes, tips)
function extractListItems(html, sectionTitle) {
  const items = [];
  const sectionStart = html.indexOf(`<h3 class="text-sm font-semibold text-zinc-100">${sectionTitle}</h3>`);
  if (sectionStart === -1) return items;

  const listStart = html.indexOf('<ul class="space-y-2">', sectionStart);
  if (listStart === -1) return items;

  const listEnd = html.indexOf('</ul>', listStart);
  if (listEnd === -1) return items;

  const listHtml = html.substring(listStart, listEnd);
  const itemRegex = /<span class="text-orange-400 mt-1 flex-shrink-0">•<\/span>\s*<span>([^<]+)<\/span>/g;
  const itemRegex2 = /<span class="text-red-400 mt-1 flex-shrink-0">•<\/span>\s*<span>([^<]+)<\/span>/g;
  const itemRegex3 = /<span class="text-green-400 mt-1 flex-shrink-0">•<\/span>\s*<span>([^<]+)<\/span>/g;

  let match;
  while ((match = itemRegex.exec(listHtml)) !== null) {
    items.push(match[1].trim());
  }
  while ((match = itemRegex2.exec(listHtml)) !== null) {
    items.push(match[1].trim());
  }
  while ((match = itemRegex3.exec(listHtml)) !== null) {
    items.push(match[1].trim());
  }

  return items;
}

// Extract phase from HTML
function extractPhase(html) {
  const match = html.match(/<div class="text-xs text-zinc-500">([^<]+)<\/div>/);
  if (!match) return '';
  const phaseText = match[1].trim();
  if (phaseText.includes('Пассивная')) return 'passive';
  if (phaseText.includes('Переходная')) return 'transition';
  if (phaseText.includes('Активная')) return 'active';
  return '';
}

// Parse single lesson
function parseLesson(lessonNum) {
  const htmlPath = path.join(__dirname, 'www', 'lesson', String(lessonNum), 'index.html');

  if (!fs.existsSync(htmlPath)) {
    console.warn(`Lesson ${lessonNum} not found at ${htmlPath}`);
    return null;
  }

  const html = fs.readFileSync(htmlPath, 'utf8');
  const week = Math.ceil(lessonNum / 7);
  const phase = extractPhase(html);

  const lesson = {
    id: lessonNum,
    week: week,
    phase: phase,
    title: extractLessonTitle(html),
    topics: extractTopics(html),
    audioPath: extractAudioPath(html),
    dialog: extractDialog(html),
    vocabulary: extractVocabulary(html),
    grammar: extractGrammar(html),
    essence: extractEssence(html),
    difficulties: extractListItems(html, 'Скрытые сложности'),
    mistakes: extractListItems(html, 'Типичные ошибки'),
    tips: extractListItems(html, 'Методические советы')
  };

  console.log(`Parsed lesson ${lessonNum}`);
  return lesson;
}

// Main function
function main() {
  const lessons = [];

  for (let i = 1; i <= 100; i++) {
    const lesson = parseLesson(i);
    if (lesson) {
      lessons.push(lesson);
    }
  }

  const output = {
    lessons: lessons,
    metadata: {
      totalLessons: lessons.length,
      generatedAt: new Date().toISOString(),
      version: '1.0'
    }
  };

  const outputPath = path.join(__dirname, 'lessons.json');
  fs.writeFileSync(outputPath, JSON.stringify(output, null, 2), 'utf8');

  console.log(`\n✓ Successfully parsed ${lessons.length} lessons`);
  console.log(`✓ Output saved to: ${outputPath}`);
}

main();
