/**
 * Master audio component — places BGM + all SFX at their exact frames.
 */

import React from 'react';
import { Audio, Sequence, staticFile, interpolate } from 'remotion';
import { useSynthSfx } from './sfx/useSynthSfx';

const FPS = 30;
const TOTAL_FRAMES = 900;

/** Helper: place an SFX <Audio> at the given frame */
function Sfx({ src, frame, volume = 0.6 }) {
  if (!src) return null;
  return (
    <Sequence from={frame} layout="none">
      <Audio src={src} volume={volume} />
    </Sequence>
  );
}

export default function AudioLayer() {
  const sfx = useSynthSfx();

  return (
    <>
      {/* ── Background Music ── */}
      <Audio
        src={staticFile('bgm.wav')}
        volume={(f) =>
          interpolate(f, [0, 15, 870, TOTAL_FRAMES], [0, 0.35, 0.5, 0], {
            extrapolateLeft: 'clamp',
            extrapolateRight: 'clamp',
          })
        }
      />

      {sfx && (
        <>
          {/* ═══ INTRO (0-105) ═══ */}
          <Sfx src={sfx.logoWhoosh} frame={0} volume={0.5} />
          <Sfx src={sfx.titleImpact} frame={3} volume={0.6} />
          <Sfx src={sfx.eyeDraw} frame={5} volume={0.3} />
          <Sfx src={sfx.taglineSlide} frame={44} volume={0.4} />
          <Sfx src={sfx.badgePop} frame={62} volume={0.5} />
          <Sfx src={sfx.sceneExit} frame={90} volume={0.4} />

          {/* ═══ PHOTO (105-215) ═══ */}
          <Sfx src={sfx.phoneWhoosh} frame={105} volume={0.4} />
          <Sfx src={sfx.shutterClick} frame={155} volume={0.5} />
          <Sfx src={sfx.flash} frame={159} volume={0.4} />
          <Sfx src={sfx.scanHum} frame={167} volume={0.3} />
          <Sfx src={sfx.processBeep} frame={160} volume={0.4} />

          {/* ═══ VIDEO (215-315) ═══ */}
          <Sfx src={sfx.phoneWhoosh} frame={215} volume={0.4} />
          <Sfx src={sfx.recBeep} frame={255} volume={0.5} />

          {/* ═══ CONTINUOUS (315-415) ═══ */}
          <Sfx src={sfx.phoneWhoosh} frame={315} volume={0.4} />
          <Sfx src={sfx.activateChime} frame={350} volume={0.5} />
          <Sfx src={sfx.resultPop} frame={370} volume={0.4} />

          {/* ═══ CHAT Q&A (415-555) ═══ */}
          <Sfx src={sfx.phoneWhoosh} frame={415} volume={0.4} />
          <Sfx src={sfx.sheetSlide} frame={430} volume={0.4} />
          <Sfx src={sfx.messagePop} frame={440} volume={0.4} />
          {/* Typing clicks every 2 frames from 470-500 */}
          {Array.from({ length: 16 }, (_, i) => (
            <Sfx
              key={`type-${i}`}
              src={sfx.typingClick}
              frame={470 + i * 2}
              volume={0.3}
            />
          ))}
          <Sfx src={sfx.sendWhoosh} frame={508} volume={0.4} />
          <Sfx src={sfx.aiResponsePop} frame={555} volume={0.4} />

          {/* ═══ VOICE (555-660) ═══ */}
          <Sfx src={sfx.phoneWhoosh} frame={555} volume={0.4} />
          <Sfx src={sfx.commandChime} frame={590} volume={0.5} />
          <Sfx src={sfx.confirmTone} frame={615} volume={0.5} />

          {/* ═══ HAPTIC (660-765) ═══ */}
          <Sfx src={sfx.phoneWhoosh} frame={660} volume={0.4} />
          <Sfx src={sfx.hapticPulse} frame={685} volume={0.6} />
          <Sfx src={sfx.hapticPulse} frame={703} volume={0.6} />
          <Sfx src={sfx.hapticPulse} frame={721} volume={0.6} />
          <Sfx src={sfx.offlineBadge} frame={710} volume={0.4} />

          {/* ═══ OUTRO (765-900) ═══ */}
          <Sfx src={sfx.grandReveal} frame={765} volume={0.6} />
          {/* Pill pops: 6 badges every 4 frames starting at 785 */}
          {Array.from({ length: 6 }, (_, i) => (
            <Sfx
              key={`pill-${i}`}
              src={sfx.badgePop}
              frame={785 + i * 4}
              volume={0.4}
            />
          ))}
          <Sfx src={sfx.comingSoonHit} frame={810} volume={0.6} />
        </>
      )}
    </>
  );
}
