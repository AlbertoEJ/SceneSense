/**
 * React hook that generates all SFX via OfflineAudioContext
 * and returns data-URL strings for use with Remotion <Audio>.
 *
 * Uses delayRender / continueRender so Remotion waits for synthesis.
 */

import { useState, useEffect } from 'react';
import { delayRender, continueRender } from 'remotion';
import { audioBufferToDataUrl } from '@remotion/media-utils';
import * as Tones from './tones';

/** All SFX that need to be generated */
const SFX_RECIPES = {
  logoWhoosh: Tones.logoWhoosh,
  titleImpact: Tones.titleImpact,
  eyeDraw: Tones.eyeDraw,
  taglineSlide: Tones.taglineSlide,
  badgePop: Tones.badgePop,
  sceneExit: Tones.sceneExit,
  phoneWhoosh: Tones.phoneWhoosh,
  shutterClick: Tones.shutterClick,
  flash: Tones.flash,
  scanHum: Tones.scanHum,
  processBeep: Tones.processBeep,
  recBeep: Tones.recBeep,
  activateChime: Tones.activateChime,
  resultPop: Tones.resultPop,
  sheetSlide: Tones.sheetSlide,
  messagePop: Tones.messagePop,
  typingClick: Tones.typingClick,
  sendWhoosh: Tones.sendWhoosh,
  aiResponsePop: Tones.aiResponsePop,
  commandChime: Tones.commandChime,
  confirmTone: Tones.confirmTone,
  hapticPulse: Tones.hapticPulse,
  offlineBadge: Tones.offlineBadge,
  grandReveal: Tones.grandReveal,
  comingSoonHit: Tones.comingSoonHit,
};

/**
 * @returns {Record<string, string> | null} map of sfxName → data URL, or null while loading
 */
export function useSynthSfx() {
  const [sfxUrls, setSfxUrls] = useState(null);
  const [handle] = useState(() => delayRender('Synthesizing SFX...'));

  useEffect(() => {
    let cancelled = false;

    async function generate() {
      const urls = {};
      const entries = Object.entries(SFX_RECIPES);

      // Generate all SFX in parallel
      const results = await Promise.all(
        entries.map(async ([name, fn]) => {
          const audioBuffer = await fn();
          const dataUrl = await audioBufferToDataUrl({ audioBuffer });
          return [name, dataUrl];
        })
      );

      if (cancelled) return;

      for (const [name, url] of results) {
        urls[name] = url;
      }

      setSfxUrls(urls);
      continueRender(handle);
    }

    generate().catch((err) => {
      console.error('SFX generation failed:', err);
      // Continue render even on error to avoid hanging
      continueRender(handle);
    });

    return () => {
      cancelled = true;
    };
  }, [handle]);

  return sfxUrls;
}
