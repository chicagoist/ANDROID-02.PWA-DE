// gold-dump.js — locate gold bands using png-sample.js's PROVEN decoder.
// Usage: node gold-dump.js <image.png>
// Uses the exact same decode pipeline as tools/png-sample.js (zlib inflate,
// stride = width*bpp, prev from already-decoded out buffer) and then prints
// gold bands + a few sampled pixel RGBs for cross-checking.
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

// Gold band detection (per-row counts, full resolution)
const rows = [];
for (let y = 0; y < height; y++) {
  let count = 0, sumX = 0;
  for (let x = 0; x < width; x++) {
    const i = y * stride + x * bpp;
    if (isGold(out[i], out[i + 1], out[i + 2])) { count++; sumX += x; }
  }
  rows.push({ y, count, cx: count ? Math.round(sumX / count) : 0 });
}
const bands = [];
let cur = null;
for (const r of rows) {
  if (r.count > 0) {
    if (cur && r.y - cur.y1 <= 3) { cur.y1 = r.y; cur.max = Math.max(cur.max, r.count); cur.rows++; cur.cntSum += r.count; cur.cxSum += r.cx * r.count; }
    else { if (cur) bands.push(cur); cur = { y0: r.y, y1: r.y, max: r.count, rows: 1, cntSum: r.count, cxSum: r.cx * r.count }; }
  } else if (cur) { bands.push(cur); cur = null; }
}
if (cur) bands.push(cur);

console.log(`size ${width}x${height}`);
console.log('=== GOLD BANDS (max>=5 gold px/row) ===');
let found = false;
for (const b of bands) {
  if (b.max < 5) continue;
  found = true;
  const cx = Math.round(b.cxSum / b.cntSum);
  const cy = Math.round((b.y0 + b.y1) / 2);
  console.log(`y=${b.y0}-${b.y1} h=${b.y1 - b.y0 + 1} maxGoldPerRow=${b.max} centroid=(${cx},${cy}) -> tap ~(${cx}, ${cy})`);
}
if (!found) console.log('(none)');

// Cross-check: sample a few pixels around (118,1583) and (177,1326)
console.log('=== PIXEL SAMPLES ===');
for (const [px, py] of [[118, 1583], [130, 1696], [177, 1326], [118, 1650], [540, 1800]]) {
  if (px >= 0 && px < width && py >= 0 && py < height) {
    const i = py * stride + px * bpp;
    console.log(`(${px},${py}) RGB=(${out[i]},${out[i + 1]},${out[i + 2]}) gold=${isGold(out[i], out[i + 1], out[i + 2])}`);
  }
}
