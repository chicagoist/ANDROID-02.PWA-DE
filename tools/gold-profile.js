// gold-profile.js — per-row gold pixel counts for a Y region, to find full-width gold buttons.
// Usage: node gold-profile.js <png> [yStart] [yEnd]
// Uses the proven zlib decoder (same as png-sample.js).
const fs = require('fs');
const zlib = require('zlib');

const buf = fs.readFileSync(process.argv[2]);
let off = 8;
let width = 0, height = 0, colorType = 0;
const idat = [];
while (off < buf.length) {
  const len = buf.readUInt32BE(off);
  const type = buf.toString('ascii', off + 4, off + 8);
  const data = buf.slice(off + 8, off + 8 + len);
  if (type === 'IHDR') { width = data.readUInt32BE(0); height = data.readUInt32BE(4); colorType = data[9]; }
  else if (type === 'IDAT') idat.push(data);
  off += 12 + len;
}
const bpp = colorType === 6 ? 4 : colorType === 2 ? 3 : 1;
const raw = zlib.inflateSync(Buffer.concat(idat));
const stride = width * bpp;
const out = Buffer.alloc(height * stride);
for (let y = 0; y < height; y++) {
  const filter = raw[y * (stride + 1)];
  const line = raw.slice(y * (stride + 1) + 1, (y + 1) * (stride + 1));
  const prev = y > 0 ? out.slice((y - 1) * stride, y * stride) : Buffer.alloc(stride);
  for (let x = 0; x < stride; x++) {
    const a = x >= bpp ? line[x - bpp] : 0;
    const b = prev[x];
    const c = x >= bpp ? prev[x - bpp] : 0;
    let val = line[x];
    if (filter === 1) val = (val + a) & 255;
    else if (filter === 2) val = (val + b) & 255;
    else if (filter === 3) val = (val + ((a + b) >> 1)) & 255;
    else if (filter === 4) {
      const p = a + b - c;
      const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
      const pr = pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
      val = (val + pr) & 255;
    }
    out[y * stride + x] = val;
  }
}
const isGold = (r, g, bl) => r > 140 && g > 110 && bl < 100 && r - bl > 60;
const yStart = parseInt(process.argv[3] || '0', 10);
const yEnd = parseInt(process.argv[4] || String(height), 10);
let prevCount = -1;
for (let y = yStart; y < Math.min(yEnd, height); y++) {
  let count = 0, minX = width, maxX = 0;
  for (let x = 0; x < width; x++) {
    const i = y * stride + x * bpp;
    if (isGold(out[i], out[i + 1], out[i + 2])) { count++; if (x < minX) minX = x; if (x > maxX) maxX = x; }
  }
  if (count !== prevCount) {
    console.log(`y=${y} gold=${count} span=${count > 0 ? minX + '-' + maxX : '-'}`);
    prevCount = count;
  }
}
