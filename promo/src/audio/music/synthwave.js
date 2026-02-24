/**
 * Synthwave music generator — 120 BPM, E minor, 30 seconds.
 * Pure mathematical PCM synthesis. No external dependencies.
 */

const SAMPLE_RATE = 44100;
const BPM = 120;
const DURATION = 30; // seconds
const TOTAL_SAMPLES = SAMPLE_RATE * DURATION;
const BEAT_SEC = 60 / BPM; // 0.5s per beat
const SIXTEENTH = BEAT_SEC / 4;

// ── Note frequencies ──
const NOTE = {
  E2: 82.41, A2: 110.0, B2: 123.47,
  E3: 164.81, G3: 196.0, A3: 220.0, B3: 246.94, C4: 261.63,
  E4: 329.63, G4: 392.0, A4: 440.0, B4: 493.88,
  E5: 659.26, G5: 783.99, B5: 987.77,
};

// Chord voicings (pads)
const CHORDS = [
  [NOTE.E3, NOTE.G3, NOTE.B3],    // Em
  [NOTE.A3, NOTE.C4, NOTE.E4],    // Am
  [NOTE.C4, NOTE.E4, NOTE.G4],    // C
  [NOTE.B3, NOTE.E4, NOTE.G4],    // B (simplified)
];

// Bass pattern (per bar: 4 beats)
const BASS_PATTERN = [NOTE.E2, NOTE.E2, NOTE.A2, NOTE.B2];

// Arp notes cycling (16th notes)
const ARP_NOTES = [NOTE.E4, NOTE.G4, NOTE.B4, NOTE.E5, NOTE.B4, NOTE.G4];

// ── Oscillators ──
function saw(phase) {
  return 2.0 * (phase - Math.floor(phase + 0.5));
}

function square(phase) {
  return (phase % 1.0) < 0.5 ? 1.0 : -1.0;
}

function noise() {
  return Math.random() * 2 - 1;
}

// ── Envelope helpers ──
function adsr(t, a, d, s, r, dur) {
  if (t < 0) return 0;
  if (t < a) return t / a;
  if (t < a + d) return 1.0 - (1.0 - s) * ((t - a) / d);
  if (t < dur - r) return s;
  if (t < dur) return s * (1.0 - (t - (dur - r)) / r);
  return 0;
}

function expDecay(t, decay) {
  return Math.exp(-t * decay);
}

// ── Layer generators ──

/** Detuned sawtooth bass */
function renderBass(out) {
  const barDur = BEAT_SEC * 4;
  let phase1 = 0, phase2 = 0;

  for (let i = 0; i < TOTAL_SAMPLES; i++) {
    const t = i / SAMPLE_RATE;
    const barIndex = Math.floor(t / barDur);
    const beatInBar = Math.floor((t % barDur) / BEAT_SEC);
    const noteFreq = BASS_PATTERN[beatInBar % BASS_PATTERN.length];

    const tInBeat = t % BEAT_SEC;
    const env = adsr(tInBeat, 0.01, 0.1, 0.7, 0.05, BEAT_SEC);

    phase1 += noteFreq / SAMPLE_RATE;
    phase2 += (noteFreq * 1.005) / SAMPLE_RATE; // slight detune

    const val = (saw(phase1) + saw(phase2)) * 0.5 * env * 0.35;
    out[i] += val;
  }
}

/** Pad — two detuned saws per voice with LFO */
function renderPad(out) {
  const barDur = BEAT_SEC * 4;
  const phases = new Float64Array(6); // 3 voices × 2 oscillators

  for (let i = 0; i < TOTAL_SAMPLES; i++) {
    const t = i / SAMPLE_RATE;
    const chordIdx = Math.floor(t / barDur) % CHORDS.length;
    const chord = CHORDS[chordIdx];

    const lfo = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.3 * t); // slow tremolo
    const tInBar = t % barDur;
    const env = adsr(tInBar, 0.4, 0.3, 0.6, 0.5, barDur);

    let sum = 0;
    for (let v = 0; v < chord.length; v++) {
      const freq = chord[v];
      phases[v * 2] += freq / SAMPLE_RATE;
      phases[v * 2 + 1] += (freq * 1.008) / SAMPLE_RATE;
      sum += (saw(phases[v * 2]) + saw(phases[v * 2 + 1])) * 0.5;
    }

    out[i] += (sum / chord.length) * env * lfo * 0.15;
  }
}

/** Arpeggio — square wave with simple bandpass feel */
function renderArp(out) {
  let phase = 0;
  const stepDur = SIXTEENTH;

  for (let i = 0; i < TOTAL_SAMPLES; i++) {
    const t = i / SAMPLE_RATE;
    const stepIdx = Math.floor(t / stepDur);
    const noteFreq = ARP_NOTES[stepIdx % ARP_NOTES.length];

    const tInStep = t - stepIdx * stepDur;
    const env = expDecay(tInStep, 12);

    phase += noteFreq / SAMPLE_RATE;
    const raw = square(phase);

    // Simple resonant-ish filter: attenuate low freqs by mixing with derivative
    const val = raw * env * 0.12;
    out[i] += val;
  }
}

/** Kick drum — sine with pitch drop */
function renderKick(out) {
  const barDur = BEAT_SEC * 4;

  for (let i = 0; i < TOTAL_SAMPLES; i++) {
    const t = i / SAMPLE_RATE;
    const beatT = t % BEAT_SEC;

    // Kick on every beat
    if (beatT < 0.15) {
      const freq = 40 + 110 * expDecay(beatT, 30); // 150Hz → 40Hz
      const env = expDecay(beatT, 18);
      const phase = freq * beatT; // simplified phase accumulation
      out[i] += Math.sin(2 * Math.PI * phase) * env * 0.4;
    }
  }
}

/** Hi-hat — noise bursts on off-beats */
function renderHiHat(out) {
  const halfBeat = BEAT_SEC / 2;

  for (let i = 0; i < TOTAL_SAMPLES; i++) {
    const t = i / SAMPLE_RATE;
    const halfIdx = Math.floor(t / halfBeat);
    const tInHalf = t - halfIdx * halfBeat;

    // Only on upbeats (odd half-beats)
    if (halfIdx % 2 === 1 && tInHalf < 0.05) {
      const env = expDecay(tInHalf, 60);
      // High-pass approximation: use noise and rapid decay
      out[i] += noise() * env * 0.12;
    }
  }
}

/**
 * Generate the full synthwave track.
 * @returns {Float32Array} mono PCM samples [-1, 1]
 */
export function generateSynthwave() {
  const out = new Float32Array(TOTAL_SAMPLES);

  renderBass(out);
  renderPad(out);
  renderArp(out);
  renderKick(out);
  renderHiHat(out);

  // ── Master volume envelope ──
  // Fade in: frames 0-15 (0 → 0.5s), sustain, swell at outro, fade out 870-900
  const FPS = 30;
  const fadeInEnd = 15 / FPS;       // 0.5s
  const fadeOutStart = 870 / FPS;   // 29.0s
  const fadeOutEnd = 900 / FPS;     // 30.0s
  const swellStart = 765 / FPS;     // 25.5s (outro starts)

  for (let i = 0; i < TOTAL_SAMPLES; i++) {
    const t = i / SAMPLE_RATE;
    let vol = 0.35;

    // Fade in
    if (t < fadeInEnd) {
      vol = 0.35 * (t / fadeInEnd);
    }
    // Swell in outro
    else if (t >= swellStart && t < fadeOutStart) {
      const progress = (t - swellStart) / (fadeOutStart - swellStart);
      vol = 0.35 + 0.15 * progress; // 0.35 → 0.50
    }
    // Fade out
    else if (t >= fadeOutStart) {
      const progress = (t - fadeOutStart) / (fadeOutEnd - fadeOutStart);
      vol = 0.5 * (1 - Math.min(1, progress));
    }

    out[i] *= vol;
  }

  // Soft clamp
  for (let i = 0; i < TOTAL_SAMPLES; i++) {
    out[i] = Math.tanh(out[i]);
  }

  return out;
}

export { SAMPLE_RATE, DURATION };
