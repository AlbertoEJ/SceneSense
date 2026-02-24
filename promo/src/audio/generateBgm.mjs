#!/usr/bin/env node
/**
 * Generates public/bgm.wav — run before Remotion render.
 * Usage: node src/audio/generateBgm.mjs
 */

import { writeFileSync, mkdirSync, existsSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { generateSynthwave, SAMPLE_RATE } from './music/synthwave.js';
import { encodeWav } from './music/wavEncoder.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = join(__dirname, '..', '..', 'public');
const outPath = join(outDir, 'bgm.wav');

if (!existsSync(outDir)) {
  mkdirSync(outDir, { recursive: true });
}

console.log('Generating synthwave BGM (30s, 44.1kHz, mono)...');
const samples = generateSynthwave();

console.log('Encoding WAV...');
const wav = encodeWav(samples, SAMPLE_RATE, 1);

writeFileSync(outPath, wav);
console.log(`Written ${(wav.length / 1024).toFixed(0)} KB → ${outPath}`);
