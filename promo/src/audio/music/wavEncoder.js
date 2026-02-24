/**
 * Encodes Float32 PCM samples into a WAV file buffer (16-bit, mono/stereo).
 * No external dependencies — pure Node.js Buffer operations.
 */

/**
 * @param {Float32Array} samples  – interleaved PCM in [-1, 1]
 * @param {number} sampleRate     – e.g. 44100
 * @param {number} numChannels    – 1 (mono) or 2 (stereo)
 * @returns {Buffer}              – complete WAV file
 */
export function encodeWav(samples, sampleRate = 44100, numChannels = 1) {
  const bitsPerSample = 16;
  const bytesPerSample = bitsPerSample / 8;
  const blockAlign = numChannels * bytesPerSample;
  const byteRate = sampleRate * blockAlign;
  const dataSize = samples.length * bytesPerSample;
  const headerSize = 44;

  const buf = Buffer.alloc(headerSize + dataSize);

  // RIFF header
  buf.write('RIFF', 0);
  buf.writeUInt32LE(headerSize + dataSize - 8, 4);
  buf.write('WAVE', 8);

  // fmt  sub-chunk
  buf.write('fmt ', 12);
  buf.writeUInt32LE(16, 16);           // sub-chunk size
  buf.writeUInt16LE(1, 20);            // PCM format
  buf.writeUInt16LE(numChannels, 22);
  buf.writeUInt32LE(sampleRate, 24);
  buf.writeUInt32LE(byteRate, 28);
  buf.writeUInt16LE(blockAlign, 32);
  buf.writeUInt16LE(bitsPerSample, 34);

  // data sub-chunk
  buf.write('data', 36);
  buf.writeUInt32LE(dataSize, 40);

  // Convert Float32 [-1,1] → Int16
  let offset = headerSize;
  for (let i = 0; i < samples.length; i++) {
    const clamped = Math.max(-1, Math.min(1, samples[i]));
    const int16 = clamped < 0 ? clamped * 0x8000 : clamped * 0x7FFF;
    buf.writeInt16LE(Math.round(int16), offset);
    offset += 2;
  }

  return buf;
}
