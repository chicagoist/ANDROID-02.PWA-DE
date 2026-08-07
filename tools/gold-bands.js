// gold-bands.js — find horizontal bands of gold pixels in a screenshot.
// Usage: node gold-bands.js <image.png>
// Prints bands (y-range, gold px count, centroid) for rows with many gold pixels,
// so we can locate full-width gold buttons like «Учебник — стр. N».
const fs = require('fs');

// Minimal PNG decoder (8-bit RGB/RGBA, non-interlaced) — enough for screenshots.
function decodePNG(buf) {
  if (buf.readUInt32BE(0) !== 0x89504e47) throw new Error('Not a PNG');
  let pos = 8;
  let width = 0, height = 0, bitDepth = 0, colorType = 0, interlace = 0;
  const idat = [];
  while (pos < buf.length) {
    const len = buf.readUInt32BE(pos);
    const type = buf.toString('ascii', pos + 4, pos + 8);
    const data = buf.slice(pos + 8, pos + 8 + len);
    if (type === 'IHDR') {
      width = data.readUInt32BE(0);
      height = data.readUInt32BE(4);
      bitDepth = data[8];
      colorType = data[9];
      interlace = data[12];
    } else if (type === 'IDAT') {
      idat.push(data);
    } else if (type === 'IEND') break;
    pos += 12 + len;
  }
  if (interlace !== 0) throw new Error('Interlaced PNG not supported');
  const raw = Buffer.concat(idat);
  const bpp = colorType === 6 ? 4 : colorType === 2 ? 3 : colorType === 0 ? 1 : colorType === 4 ? 2 : 3;
  const stride = width * bpp + 1;
  const out = Buffer.alloc(width * height * 3);
  let prev = Buffer.alloc(stride);
  let rp = 0;
  for (let y = 0; y < height; y++) {
    const filter = raw[rp];
    rp++;
    const line = Buffer.from(raw.slice(rp, rp + stride - 1));
    rp += stride - 1;
    for (let x = 0; x < line.length; x++) {
      const a = x >= bpp ? line[x - bpp] : 0;
      const b = prev[x];
      const c = x >= bpp ? prev[x - bpp] : 0;
      let v = line[x];
      if (filter === 1) v = (v + a) & 0xff;
      else if (filter === 2) v = (v + b) & 0xff;
      else if (filter === 3) v = (v + ((a + b) >> 1)) & 0xff;
      else if (filter === 4) v = (v + paeth(a, b, c)) & 0xff;
      line[x] = v;
    }
    prev = line;
    for (let x = 0; x < width; x++) {
      const o = (y * width + x) * 3;
      if (colorType === 6) {
        out[o] = line[x * 4];
        out[o + 1] = line[x * 4 + 1];
        out[o + 2] = line[x * 4 + 2];
      } else if (colorType === 2) {
        out[o] = line[x * 3];
        out[o + 1] = line[x * 3 + 1];
        out[o + 2] = line[x * 3 + 2];
      } else if (colorType === 0) {
        out[o] = out[o + 1] = out[o + 2] = line[x];
      } else if (colorType === 4) {
        out[o] = line[x * 2];
        out[o + 1] = out[o + 2] = line[x * 2 + 1];
      }
    }
  }
  return { width, height, data: out };
}

function paeth(a, b, c) {
  const p = a + b - c;
  const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
  if (pa <= pb && pa <= pc) return a;
  if (pb <= pc) return b;
  return c;
}

function isGold(r, g, b) {
  // Same rule as the proven png-sample.js tool: r>140 && g>110 && bl<100 && r-bl>60.
  return r > 140 && g > 110 && b < 100 && (r - b) > 60;
}

const file = process.argv[2];
if (!file) { console.error('usage: node gold-bands.js <png>'); process.exit(1); }
const png = decodePNG(fs.readFileSync(file));
const { width, height, data } = png;

const rows = [];
for (let y = 0; y < height; y++) {
  let count = 0;
  let sumX = 0;
  for (let x = 0; x < width; x++) {
    const o = (y * width + x) * 3;
    if (isGold(data[o], data[o + 1], data[o + 2])) { count++; sumX += x; }
  }
  rows.push({ y, count, cx: count ? Math.round(sumX / count) : 0 });
}

// merge consecutive rows with gold into bands
const bands = [];
let cur = null;
for (const r of rows) {
  if (r.count > 0) {
    if (cur && r.y - cur.y1 <= 3) { cur.y1 = r.y; cur.max = Math.max(cur.max, r.count); cur.rows++; }
    else { if (cur) bands.push(cur); cur = { y0: r.y, y1: r.y, max: r.count, rows: 1, cxSum: r.cx, cntSum: r.count }; }
  } else if (cur) { bands.push(cur); cur = null; }
}
if (cur) bands.push(cur);

console.log(`size ${width}x${height}`);
if (!bands.length) { console.log('NO GOLD PIXELS'); process.exit(0); }
for (const b of bands) {
  if (b.max < 5) continue;
  const cx = Math.round(b.cxSum / b.cntSum);
  console.log(`band y=${b.y0}-${b.y1} (h=${b.y1 - b.y0 + 1}, maxGoldPerRow=${b.max}, rows=${b.rows}) centroidX=${cx} -> tap ~(${cx}, ${Math.round((b.y0 + b.y1) / 2)})`);
}
