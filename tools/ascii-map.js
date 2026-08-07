// ascii-map.js — print a coarse ASCII map of a screenshot to "see" the layout.
// Usage: node ascii-map.js <image.png>
// Uses the proven zlib-based decoder from tools/png-sample.js (the hand-written
// scanline decoder drifted and produced all-black output).
// Renders: . = dark, : = dim, o = mid, # = bright, G = gold, R = red/warm.
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

const COLS = 72, ROWS = 40;
const cw = width / COLS, ch = height / ROWS;
const isGold = (r, g, bl) => r > 140 && g > 110 && bl < 100 && r - bl > 60;
const isRed = (r, g, bl) => r > 150 && g < 110 && bl < 110;

for (let row = 0; row < ROWS; row++) {
  let line = '';
  for (let col = 0; col < COLS; col++) {
    let sum = 0, n = 0, isG = false, isR = false;
    for (let y = Math.floor(row * ch); y < Math.min(height, Math.floor((row + 1) * ch)); y += 4) {
      for (let x = Math.floor(col * cw); x < Math.min(width, Math.floor((col + 1) * cw)); x += 4) {
        const i = y * stride + x * bpp;
        const r = out[i], g = out[i + 1], bl = out[i + 2];
        sum += (r + g + bl) / 3; n++;
        if (isGold(r, g, bl)) isG = true;
        if (isRed(r, g, bl)) isR = true;
      }
    }
    const lum = sum / (n || 1);
    if (isG) line += 'G';
    else if (isR) line += 'R';
    else if (lum < 12) line += '.';
    else if (lum < 40) line += ':';
    else if (lum < 100) line += 'o';
    else if (lum < 180) line += '#';
    else line += '@';
  }
  console.log(String(row * ch | 0).padStart(4) + ' ' + line);
}
