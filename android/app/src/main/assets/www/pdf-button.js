/**
 * Just German — v2.0 «Учебник» button
 * ------------------------------------
 * On every /lesson/N page this script injects a gold button right under the
 * audio player: «📖 Учебник (стр. X)». Clicking it opens the Assimil textbook
 * (resources/Assimil_DE.pdf) at the page that corresponds to lesson N.
 *
 * The page mapping below was extracted from the PDF itself (markers
 * "ERSTE (1.) LEKTION" … "HUNDERTSTE (100.) UND LETZTE LEKTION").
 * Page numbers are 1-based physical PDF pages (browsers honour #page=N).
 *
 * The app is a hydrated Next.js App Router site: navigation between lessons
 * happens client-side without a full page reload, so the button is re-synced
 * on every URL change (history events + MutationObserver fallback).
 */
(function () {
  'use strict';

  // lesson number -> physical PDF page
  var LESSON_PAGE = {
    1: 5, 2: 6, 3: 8, 4: 10, 5: 11, 6: 13, 7: 15, 8: 16, 9: 18, 10: 20,
    11: 22, 12: 25, 13: 26, 14: 28, 15: 30, 16: 31, 17: 33, 18: 35, 19: 37, 20: 39,
    21: 41, 22: 43, 23: 45, 24: 47, 25: 50, 26: 52, 27: 54, 28: 56, 29: 58, 30: 60,
    31: 62, 32: 64, 33: 67, 34: 69, 35: 71, 36: 72, 37: 75, 38: 77, 39: 79, 40: 81,
    41: 83, 42: 84, 43: 86, 44: 88, 45: 90, 46: 93, 47: 94, 48: 97, 49: 99, 50: 100,
    51: 103, 52: 105, 53: 107, 54: 109, 55: 111, 56: 112, 57: 113, 58: 115, 59: 117, 60: 119,
    61: 122, 62: 124, 63: 126, 64: 127, 65: 129, 66: 132, 67: 134, 68: 136, 69: 138, 70: 140,
    71: 141, 72: 143, 73: 146, 74: 148, 75: 150, 76: 152, 77: 155, 78: 156, 79: 158, 80: 161,
    81: 163, 82: 165, 83: 167, 84: 169, 85: 171, 86: 173, 87: 175, 88: 177, 89: 180, 90: 182,
    91: 184, 92: 185, 93: 188, 94: 190, 95: 192, 96: 194, 97: 197, 98: 199, 99: 201, 100: 203
  };

  var PDF_PATH = '/resources/Assimil_DE.pdf';
  var BTN_ID = 'jg-pdf-btn';

  function getLessonNumber() {
    var m = window.location.pathname.match(/^\/lesson\/(\d+)/);
    return m ? parseInt(m[1], 10) : null;
  }

  function findAnchor() {
    var audio = document.querySelector('main audio');
    if (!audio) return null;
    // The audio player container has the rounded-xl class in the current markup;
    // fall back to the nearest div if the class ever changes.
    return audio.closest('[class*="rounded-xl"]') || audio.closest('div');
  }

  function createButton(page) {
    var link = document.createElement('a');
    link.id = BTN_ID;
    // Desktop: #page=N (the browser's built-in PDF viewer jumps to the page).
    // Android WebView: ?page=N (query params are never stripped by WebView,
    // unlike URL fragments — MainActivity parses either form).
    var isAndroidWebView = /Android/i.test(navigator.userAgent);
    link.href = isAndroidWebView
      ? PDF_PATH + '?page=' + page
      : PDF_PATH + '#page=' + page;
    // In a desktop browser the PDF opens in a new tab. Inside the Android
    // WebView the click must navigate in the same frame so the native layer
    // can intercept the .pdf request and open the external viewer.
    if (!isAndroidWebView) {
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
    }
    link.setAttribute('aria-label', 'Открыть учебник на странице ' + page);
    link.className =
      'w-full py-4 bg-gold text-black font-semibold rounded-xl ' +
      'hover:bg-yellow-400 transition-colors flex items-center justify-center gap-2 ' +
      'mt-4 cursor-pointer';
    link.innerHTML =
      '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" ' +
      'fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" ' +
      'aria-hidden="true"><path d="M12 5v16"></path><path d="M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z"></path></svg>' +
      '<span>Учебник — стр. ' + page + '</span>';
    return link;
  }

  function syncButton() {
    var lesson = getLessonNumber();
    var page = lesson && LESSON_PAGE[lesson];
    var existing = document.getElementById(BTN_ID);

    if (!page) {
      if (existing && existing.parentNode) existing.parentNode.removeChild(existing);
      return;
    }

    // Already present and pointing at the right lesson: nothing to do.
    if (existing && existing.getAttribute('data-lesson') === String(lesson)) return;

    var anchor = findAnchor();
    if (!anchor || !anchor.parentNode) return;

    if (existing && existing.parentNode) existing.parentNode.removeChild(existing);
    var link = createButton(page);
    link.setAttribute('data-lesson', String(lesson));
    anchor.parentNode.insertBefore(link, anchor.nextSibling);
  }

  var syncTimer = null;
  function scheduleSync() {
    // Debounce: never mutate the DOM while React is hydrating (that would
    // trigger a hydration mismatch, e.g. Minified React error #418).
    if (syncTimer) clearTimeout(syncTimer);
    syncTimer = setTimeout(syncButton, 400);
  }

  // Track URL changes (Next.js App Router uses pushState/replaceState).
  var wrapHistory = function (type) {
    var orig = history[type];
    return function () {
      var r = orig.apply(this, arguments);
      scheduleSync();
      return r;
    };
  };
  history.pushState = wrapHistory('pushState');
  history.replaceState = wrapHistory('replaceState');
  window.addEventListener('popstate', scheduleSync);

  // Fallback: re-sync whenever the DOM changes (also covers late hydration).
  var observer = new MutationObserver(function () {
    scheduleSync();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });

  // Initial injection — delayed well past hydration (and re-checked by the
  // debounced observer / history hooks afterwards).
  setTimeout(syncButton, 1000);
})();
