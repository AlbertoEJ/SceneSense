/**
 * SFX synthesis recipes using OfflineAudioContext.
 * Each function returns a rendered AudioBuffer.
 */

const SR = 44100;

/** Create an OfflineAudioContext for a given duration */
function ctx(duration, channels = 1) {
  return new OfflineAudioContext(channels, Math.ceil(SR * duration), SR);
}

// ── Utility: connect a chain of nodes ──
function chain(...nodes) {
  for (let i = 0; i < nodes.length - 1; i++) {
    nodes[i].connect(nodes[i + 1]);
  }
}

// ── Noise buffer helper ──
function noiseBuffer(actx, duration) {
  const len = Math.ceil(SR * duration);
  const buf = actx.createBuffer(1, len, SR);
  const data = buf.getChannelData(0);
  for (let i = 0; i < len; i++) data[i] = Math.random() * 2 - 1;
  return buf;
}

// ═══════════════════════════════════════════════════
// INTRO SFX
// ═══════════════════════════════════════════════════

/** Logo whoosh — ascending noise sweep */
export async function logoWhoosh() {
  const dur = 0.4;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);

  const bp = ac.createBiquadFilter();
  bp.type = 'bandpass';
  bp.Q.value = 5;
  bp.frequency.setValueAtTime(200, 0);
  bp.frequency.exponentialRampToValueAtTime(6000, dur * 0.7);
  bp.frequency.exponentialRampToValueAtTime(2000, dur);

  const gain = ac.createGain();
  gain.gain.setValueAtTime(0, 0);
  gain.gain.linearRampToValueAtTime(0.5, dur * 0.3);
  gain.gain.linearRampToValueAtTime(0, dur);

  chain(src, bp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

/** Title impact — low thump + noise transient */
export async function titleImpact() {
  const dur = 0.3;
  const ac = ctx(dur);

  // Sub thump
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(60, 0);
  osc.frequency.exponentialRampToValueAtTime(30, dur);
  const oscGain = ac.createGain();
  oscGain.gain.setValueAtTime(0.6, 0);
  oscGain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(osc, oscGain, ac.destination);

  // Noise transient
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const nGain = ac.createGain();
  nGain.gain.setValueAtTime(0.4, 0);
  nGain.gain.exponentialRampToValueAtTime(0.001, 0.05);
  chain(src, nGain, ac.destination);

  osc.start(0);
  src.start(0);
  return ac.startRendering();
}

/** Eye draw — descending saw tone */
export async function eyeDraw() {
  const dur = 0.5;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sawtooth';
  osc.frequency.setValueAtTime(2000, 0);
  osc.frequency.exponentialRampToValueAtTime(1000, dur);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.08, 0);
  gain.gain.linearRampToValueAtTime(0, dur);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

/** Tagline slide — soft noise whoosh */
export async function taglineSlide() {
  const dur = 0.3;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const lp = ac.createBiquadFilter();
  lp.type = 'lowpass';
  lp.frequency.setValueAtTime(2000, 0);
  lp.frequency.linearRampToValueAtTime(4000, dur * 0.5);
  lp.frequency.linearRampToValueAtTime(1000, dur);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0, 0);
  gain.gain.linearRampToValueAtTime(0.2, dur * 0.3);
  gain.gain.linearRampToValueAtTime(0, dur);
  chain(src, lp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

/** Badge pop — sine with pitch bend up */
export async function badgePop() {
  const dur = 0.15;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(400, 0);
  osc.frequency.exponentialRampToValueAtTime(800, 0.04);
  osc.frequency.exponentialRampToValueAtTime(600, dur);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.3, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

/** Scene exit — descending noise sweep */
export async function sceneExit() {
  const dur = 0.3;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const bp = ac.createBiquadFilter();
  bp.type = 'bandpass';
  bp.Q.value = 4;
  bp.frequency.setValueAtTime(4000, 0);
  bp.frequency.exponentialRampToValueAtTime(200, dur);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.3, 0);
  gain.gain.linearRampToValueAtTime(0, dur);
  chain(src, bp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

// ═══════════════════════════════════════════════════
// PHOTO SFX
// ═══════════════════════════════════════════════════

/** Phone whoosh — bandpass noise sweep (reusable across scenes) */
export async function phoneWhoosh() {
  const dur = 0.35;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const bp = ac.createBiquadFilter();
  bp.type = 'bandpass';
  bp.Q.value = 3;
  bp.frequency.setValueAtTime(300, 0);
  bp.frequency.exponentialRampToValueAtTime(3000, dur * 0.5);
  bp.frequency.exponentialRampToValueAtTime(800, dur);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0, 0);
  gain.gain.linearRampToValueAtTime(0.3, dur * 0.3);
  gain.gain.linearRampToValueAtTime(0, dur);
  chain(src, bp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

/** Shutter click — double noise burst */
export async function shutterClick() {
  const dur = 0.12;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const hp = ac.createBiquadFilter();
  hp.type = 'highpass';
  hp.frequency.value = 3000;
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.5, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, 0.02);
  gain.gain.setValueAtTime(0.4, 0.04);
  gain.gain.exponentialRampToValueAtTime(0.001, 0.07);
  chain(src, hp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

/** Flash — bright noise burst */
export async function flash() {
  const dur = 0.08;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const hp = ac.createBiquadFilter();
  hp.type = 'highpass';
  hp.frequency.value = 6000;
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.4, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(src, hp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

/** Scan hum — sine 4kHz with vibrato */
export async function scanHum() {
  const dur = 0.8;
  const ac = ctx(dur);
  const lfo = ac.createOscillator();
  lfo.type = 'sine';
  lfo.frequency.value = 8;
  const lfoGain = ac.createGain();
  lfoGain.gain.value = 40;
  chain(lfo, lfoGain);

  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.value = 4000;
  lfoGain.connect(osc.frequency);

  const gain = ac.createGain();
  gain.gain.setValueAtTime(0, 0);
  gain.gain.linearRampToValueAtTime(0.1, 0.1);
  gain.gain.setValueAtTime(0.1, dur - 0.15);
  gain.gain.linearRampToValueAtTime(0, dur);

  chain(osc, gain, ac.destination);
  lfo.start(0);
  osc.start(0);
  return ac.startRendering();
}

/** Process beep — two tones C6-E6 */
export async function processBeep() {
  const dur = 0.2;
  const ac = ctx(dur);
  // C6
  const osc1 = ac.createOscillator();
  osc1.type = 'sine';
  osc1.frequency.value = 1047;
  const g1 = ac.createGain();
  g1.gain.setValueAtTime(0.25, 0);
  g1.gain.exponentialRampToValueAtTime(0.001, 0.08);
  chain(osc1, g1, ac.destination);

  // E6
  const osc2 = ac.createOscillator();
  osc2.type = 'sine';
  osc2.frequency.value = 1319;
  const g2 = ac.createGain();
  g2.gain.setValueAtTime(0, 0);
  g2.gain.setValueAtTime(0.25, 0.1);
  g2.gain.exponentialRampToValueAtTime(0.001, 0.18);
  chain(osc2, g2, ac.destination);

  osc1.start(0);
  osc2.start(0);
  return ac.startRendering();
}

// ═══════════════════════════════════════════════════
// VIDEO SFX
// ═══════════════════════════════════════════════════

/** REC beep — double pulse sine 1kHz */
export async function recBeep() {
  const dur = 0.25;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.value = 1000;
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.3, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, 0.06);
  gain.gain.setValueAtTime(0.3, 0.12);
  gain.gain.exponentialRampToValueAtTime(0.001, 0.2);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

// ═══════════════════════════════════════════════════
// CONTINUOUS SFX
// ═══════════════════════════════════════════════════

/** Activate chime — arpeggio E5-G5-B5 */
export async function activateChime() {
  const dur = 0.4;
  const ac = ctx(dur);
  const notes = [659.26, 783.99, 987.77]; // E5, G5, B5
  notes.forEach((freq, idx) => {
    const osc = ac.createOscillator();
    osc.type = 'sine';
    osc.frequency.value = freq;
    const gain = ac.createGain();
    const start = idx * 0.08;
    gain.gain.setValueAtTime(0, 0);
    gain.gain.setValueAtTime(0.25, start);
    gain.gain.exponentialRampToValueAtTime(0.001, start + 0.2);
    chain(osc, gain, ac.destination);
    osc.start(0);
  });
  return ac.startRendering();
}

/** Result pop — sine burst 660Hz */
export async function resultPop() {
  const dur = 0.15;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.value = 660;
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.3, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

// ═══════════════════════════════════════════════════
// CHAT SFX
// ═══════════════════════════════════════════════════

/** Sheet slide — low-pass noise */
export async function sheetSlide() {
  const dur = 0.25;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const lp = ac.createBiquadFilter();
  lp.type = 'lowpass';
  lp.frequency.setValueAtTime(500, 0);
  lp.frequency.linearRampToValueAtTime(2000, dur * 0.4);
  lp.frequency.linearRampToValueAtTime(300, dur);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0, 0);
  gain.gain.linearRampToValueAtTime(0.25, dur * 0.2);
  gain.gain.linearRampToValueAtTime(0, dur);
  chain(src, lp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

/** Message pop — sine 523Hz with pitch bend */
export async function messagePop() {
  const dur = 0.12;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(400, 0);
  osc.frequency.exponentialRampToValueAtTime(523, 0.03);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.3, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

/** Typing click — single short noise burst */
export async function typingClick() {
  const dur = 0.03;
  const ac = ctx(dur);
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const hp = ac.createBiquadFilter();
  hp.type = 'highpass';
  hp.frequency.value = 4000;
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.2, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(src, hp, gain, ac.destination);
  src.start(0);
  return ac.startRendering();
}

/** Send whoosh — sweep 400→800Hz */
export async function sendWhoosh() {
  const dur = 0.25;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(400, 0);
  osc.frequency.exponentialRampToValueAtTime(800, dur * 0.6);
  osc.frequency.exponentialRampToValueAtTime(600, dur);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0, 0);
  gain.gain.linearRampToValueAtTime(0.2, dur * 0.2);
  gain.gain.linearRampToValueAtTime(0, dur);

  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const bp = ac.createBiquadFilter();
  bp.type = 'bandpass';
  bp.Q.value = 5;
  bp.frequency.setValueAtTime(400, 0);
  bp.frequency.exponentialRampToValueAtTime(800, dur);
  const nGain = ac.createGain();
  nGain.gain.setValueAtTime(0, 0);
  nGain.gain.linearRampToValueAtTime(0.15, dur * 0.2);
  nGain.gain.linearRampToValueAtTime(0, dur);

  chain(osc, gain, ac.destination);
  chain(src, bp, nGain, ac.destination);
  osc.start(0);
  src.start(0);
  return ac.startRendering();
}

/** AI response pop — sine 659Hz */
export async function aiResponsePop() {
  const dur = 0.15;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(500, 0);
  osc.frequency.exponentialRampToValueAtTime(659, 0.03);
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.3, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

// ═══════════════════════════════════════════════════
// VOICE SFX
// ═══════════════════════════════════════════════════

/** Command chime — two tones G5-B5 */
export async function commandChime() {
  const dur = 0.3;
  const ac = ctx(dur);
  const notes = [783.99, 987.77]; // G5, B5
  notes.forEach((freq, idx) => {
    const osc = ac.createOscillator();
    osc.type = 'sine';
    osc.frequency.value = freq;
    const gain = ac.createGain();
    const start = idx * 0.1;
    gain.gain.setValueAtTime(0, 0);
    gain.gain.setValueAtTime(0.25, start);
    gain.gain.exponentialRampToValueAtTime(0.001, start + 0.15);
    chain(osc, gain, ac.destination);
    osc.start(0);
  });
  return ac.startRendering();
}

/** Confirm tone — three descending notes B5-G5-E5 */
export async function confirmTone() {
  const dur = 0.4;
  const ac = ctx(dur);
  const notes = [987.77, 783.99, 659.26]; // B5, G5, E5
  notes.forEach((freq, idx) => {
    const osc = ac.createOscillator();
    osc.type = 'sine';
    osc.frequency.value = freq;
    const gain = ac.createGain();
    const start = idx * 0.1;
    gain.gain.setValueAtTime(0, 0);
    gain.gain.setValueAtTime(0.2, start);
    gain.gain.exponentialRampToValueAtTime(0.001, start + 0.18);
    chain(osc, gain, ac.destination);
    osc.start(0);
  });
  return ac.startRendering();
}

// ═══════════════════════════════════════════════════
// HAPTIC SFX
// ═══════════════════════════════════════════════════

/** Haptic pulse — very low sine burst */
export async function hapticPulse() {
  const dur = 0.1;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.value = 32;
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.5, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

/** Offline badge — short square 440Hz */
export async function offlineBadge() {
  const dur = 0.1;
  const ac = ctx(dur);
  const osc = ac.createOscillator();
  osc.type = 'square';
  osc.frequency.value = 440;
  const gain = ac.createGain();
  gain.gain.setValueAtTime(0.2, 0);
  gain.gain.exponentialRampToValueAtTime(0.001, dur);
  chain(osc, gain, ac.destination);
  osc.start(0);
  return ac.startRendering();
}

// ═══════════════════════════════════════════════════
// OUTRO SFX
// ═══════════════════════════════════════════════════

/** Grand reveal — ascending noise + sine sweep */
export async function grandReveal() {
  const dur = 0.6;
  const ac = ctx(dur);

  // Ascending sine sweep
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(200, 0);
  osc.frequency.exponentialRampToValueAtTime(1200, dur * 0.7);
  osc.frequency.exponentialRampToValueAtTime(800, dur);
  const oscGain = ac.createGain();
  oscGain.gain.setValueAtTime(0, 0);
  oscGain.gain.linearRampToValueAtTime(0.3, dur * 0.5);
  oscGain.gain.linearRampToValueAtTime(0, dur);
  chain(osc, oscGain, ac.destination);

  // Noise sweep
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const bp = ac.createBiquadFilter();
  bp.type = 'bandpass';
  bp.Q.value = 3;
  bp.frequency.setValueAtTime(200, 0);
  bp.frequency.exponentialRampToValueAtTime(5000, dur * 0.7);
  bp.frequency.exponentialRampToValueAtTime(1000, dur);
  const nGain = ac.createGain();
  nGain.gain.setValueAtTime(0, 0);
  nGain.gain.linearRampToValueAtTime(0.25, dur * 0.4);
  nGain.gain.linearRampToValueAtTime(0, dur);
  chain(src, bp, nGain, ac.destination);

  osc.start(0);
  src.start(0);
  return ac.startRendering();
}

/** Coming Soon hit — low thump + noise transient */
export async function comingSoonHit() {
  const dur = 0.35;
  const ac = ctx(dur);

  // Low thump
  const osc = ac.createOscillator();
  osc.type = 'sine';
  osc.frequency.setValueAtTime(80, 0);
  osc.frequency.exponentialRampToValueAtTime(30, dur);
  const oscGain = ac.createGain();
  oscGain.gain.setValueAtTime(0.5, 0);
  oscGain.gain.exponentialRampToValueAtTime(0.001, dur * 0.7);
  chain(osc, oscGain, ac.destination);

  // Noise transient
  const src = ac.createBufferSource();
  src.buffer = noiseBuffer(ac, dur);
  const hp = ac.createBiquadFilter();
  hp.type = 'highpass';
  hp.frequency.value = 2000;
  const nGain = ac.createGain();
  nGain.gain.setValueAtTime(0.35, 0);
  nGain.gain.exponentialRampToValueAtTime(0.001, 0.06);
  chain(src, hp, nGain, ac.destination);

  osc.start(0);
  src.start(0);
  return ac.startRendering();
}
