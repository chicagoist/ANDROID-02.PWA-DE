// Minimal PNG decoder using only Node built-ins (zlib) to sample pixel colors.
// Usage: node png-sample.js <file.png>
const fs = require('fs');
const zlib = require('zlib');

const buf = fs.readFileSync(process.argv[2]);
let off = 8;
let width = 0, height = 0, bitDepth = 0, colorType = 0;
const idat = [];

while (off < buf.length) {
  const len = buf.readUInt32BE(off);
  const type = buf.toString('ascii', off + 4, off + 8);
  const data = buf.slice(off + 8, off + 8 + len);
  if (type === 'IHDR') {
    width = data.readUInt32BE(0);
    height = data.readUInt32BE(4);
    bitDepth = data[8];
    colorType = data[9];
  } else if (type === 'IDAT') {
    idat.push(data);
  }
  off += 12 + len;
}

const bpp = colorType === 6 ? 4 : colorType === 2 ? 3 : 1; // RGBA or RGB
const raw = zlib.inflateSync(Buffer.concat(idat));
const stride = width * bpp;
const out = Buffer.alloc(height * stride);

// Undo PNG scanline filters
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

// Sample colors + find gold button location (centroid of gold pixels)
let total = 0, dark = 0, bright = 0, gold = 0, colored = 0;
let minLum = 255, maxLum = 0, sumLum = 0;
let gx = 0, gy = 0;
const step = 8;
for (let y = 0; y < height; y += step) {
  for (let x = 0; x < width; x += step) {
    const i = y * stride + x * bpp;
    const r = out[i], g = out[i + 1], bl = out[i + 2];
    const lum = 0.299 * r + 0.587 * g + 0.114 * bl;
    total++;
    sumLum += lum;
    if (lum < 40) dark++;
    if (lum > 200) bright++;
    if (r > 140 && g > 110 && bl < 100 && r - bl > 60) { gold++; gx += x; gy += y; } // gold accents
    if (Math.max(r, g, bl) - Math.min(r, g, bl) > 40) colored++; // any saturated color
    if (lum < minLum) minLum = lum;
    if (lum > maxLum) maxLum = lum;
  }
}
const avg = sumLum / total;
const result = {
  size: width + 'x' + height,
  colorType, bitDepth,
  darkPct: (dark / total * 100).toFixed(1),
  brightPct: (bright / total * 100).toFixed(1),
  goldPct: (gold / total * 100).toFixed(1),
  coloredPct: (colored / total * 100).toFixed(1),
  avgLum: avg.toFixed(1),
  minLum: minLum.toFixed(0),
  maxLum: maxLum.toFixed(0),
  design: dark / total > 0.5 ? 'DARK THEME RENDERED' : (maxLum > 240 && dark / total < 0.2 ? 'LIGHT/UNSTYLED (white bg)' : 'MIXED')
};
if (gold > 0) { result.goldCentroidX = Math.round(gx / gold); result.goldCentroidY = Math.round(gy / gold); }
console.log(JSON.stringify(result));
