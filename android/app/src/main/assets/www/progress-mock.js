(function () {
  'use strict';
  var KEY = 'jg-done-v1';
  function read() {
    try { return JSON.parse(localStorage.getItem(KEY) || '[]'); } catch (e) { return []; }
  }
  function write(list) {
    try { localStorage.setItem(KEY, JSON.stringify(list)); } catch (e) {}
  }
  var origFetch = window.fetch;
  window.fetch = function (input, init) {
    var url = typeof input === 'string' ? input : (input && input.url) || '';
    if (url.indexOf('/api/progress') !== -1) {
      var method = (init && init.method) || (input && input.method) || 'GET';
      if (method.toUpperCase() === 'POST') {
        var body = {};
        try { body = JSON.parse(init.body || '{}'); } catch (e) {}
        var list = read();
        if (body.lessonId != null && list.indexOf(String(body.lessonId)) === -1) {
          list.push(String(body.lessonId));
          write(list);
        }
        return Promise.resolve(new Response(JSON.stringify({ ok: true }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        }));
      }
      return Promise.resolve(new Response(JSON.stringify({ completedLessons: read() }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      }));
    }
    return origFetch.apply(this, arguments);
  };
})();
