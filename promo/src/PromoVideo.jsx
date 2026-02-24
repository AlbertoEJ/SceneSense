import {
  AbsoluteFill,
  Sequence,
  useCurrentFrame,
  interpolate,
  spring,
  useVideoConfig,
  staticFile,
  Img,
} from "remotion";
import AudioLayer from "./audio/AudioLayer";

const C = {
  cyan: "#0DE3F2",
  cyanDim: "rgba(13, 227, 242, 0.20)",
  cyanGlow: "rgba(13, 227, 242, 0.40)",
  darkBg: "#102122",
  glassBase: "rgba(18, 18, 24, 0.75)",
  glassBorder: "rgba(255, 255, 255, 0.15)",
  neonGreen: "#00FF88",
  errorRed: "#FF3D5A",
  white: "#FFFFFF",
  dimWhite: "rgba(255,255,255,0.6)",
  mutedWhite: "rgba(255,255,255,0.3)",
};

const ease = (f, from, to, range) =>
  interpolate(f, range, [from, to], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });

const GlassSurface = ({ children, style, borderRadius = 16 }) => (
  <div style={{ background: C.glassBase, border: `1px solid ${C.glassBorder}`, borderRadius, ...style }}>{children}</div>
);

const GridBg = () => (<>
  {Array.from({ length: 9 }).map((_, i) => <div key={`v${i}`} style={{ position: "absolute", left: `${(i+1)*10}%`, top: 0, bottom: 0, width: 1, background: "rgba(13,227,242,0.04)" }} />)}
  {Array.from({ length: 16 }).map((_, i) => <div key={`h${i}`} style={{ position: "absolute", top: `${(i+1)*5.8}%`, left: 0, right: 0, height: 1, background: "rgba(13,227,242,0.04)" }} />)}
</>);

// ─── 3D Phone ───
const PhoneMockup = ({ children, rotateY = 0, rotateX = 0, scale = 1, opacity = 1, translateX = 0, translateY = 0 }) => (
  <div style={{ perspective: 1400, display: "flex", justifyContent: "center", alignItems: "center" }}>
    <div style={{
      width: 420, height: 860, borderRadius: 48, background: "#080808",
      border: "5px solid #1a1a1a",
      boxShadow: `0 0 0 2px #111, 0 30px 100px rgba(0,0,0,0.9), 0 0 80px ${C.cyanGlow}, inset 0 0 2px rgba(255,255,255,0.08)`,
      overflow: "hidden", position: "relative",
      transform: `rotateY(${rotateY}deg) rotateX(${rotateX}deg) scale(${scale}) translate(${translateX}px, ${translateY}px)`,
      transformStyle: "preserve-3d", opacity,
    }}>
      <div style={{ position: "absolute", top: 0, left: "50%", transform: "translateX(-50%)", width: 140, height: 32, background: "#080808", borderBottomLeftRadius: 20, borderBottomRightRadius: 20, zIndex: 20 }}>
        <div style={{ position: "absolute", top: 10, left: "50%", transform: "translateX(-50%)", width: 60, height: 6, borderRadius: 3, background: "#1a1a1a" }} />
      </div>
      <div style={{ width: "100%", height: "100%", position: "relative", overflow: "hidden" }}>{children}</div>
      <div style={{ position: "absolute", inset: 0, background: "linear-gradient(135deg, rgba(255,255,255,0.03) 0%, transparent 50%)", pointerEvents: "none", zIndex: 15 }} />
    </div>
  </div>
);

// ─── App UI atoms ───
const PulsingDot = ({ color = C.neonGreen, frame }) => (
  <div style={{ width: 11, height: 11, borderRadius: 6, background: color, opacity: 0.6 + Math.sin(frame * 0.1) * 0.4 }} />
);

const HeaderPill = ({ frame }) => (
  <div style={{ position: "absolute", top: 44, left: 14, zIndex: 10 }}>
    <GlassSurface borderRadius={20}>
      <div style={{ display: "flex", alignItems: "center", padding: "9px 16px", gap: 9 }}>
        <PulsingDot color={C.neonGreen} frame={frame} />
        <span style={{ color: C.white, fontSize: 16, fontWeight: 700, letterSpacing: 2.5, fontFamily: "monospace" }}>SCENESENSE</span>
      </div>
    </GlassSurface>
  </div>
);

const CornerBrackets = () => {
  const m = 14, len = 46, sw = 3;
  const b = { position: "absolute", background: C.cyan, borderRadius: 2 };
  return (<>
    <div style={{ ...b, top: 84+m, left: m, width: len, height: sw }} />
    <div style={{ ...b, top: 84+m, left: m, width: sw, height: len }} />
    <div style={{ ...b, top: 84+m, right: m, width: len, height: sw }} />
    <div style={{ ...b, top: 84+m, right: m, width: sw, height: len }} />
    <div style={{ ...b, bottom: 200+m, left: m, width: len, height: sw }} />
    <div style={{ ...b, bottom: 200+m, left: m, width: sw, height: len }} />
    <div style={{ ...b, bottom: 200+m, right: m, width: len, height: sw }} />
    <div style={{ ...b, bottom: 200+m, right: m, width: sw, height: len }} />
  </>);
};

const ModeSelectorPill = ({ selected = "FOTO" }) => (
  <GlassSurface borderRadius={22}>
    <div style={{ display: "flex" }}>
      {["FOTO", "VIDEO", "CONTINUO"].map(t => (
        <div key={t} style={{ padding: "10px 18px", borderRadius: 22, background: t === selected ? C.cyanDim : "transparent", color: t === selected ? C.cyan : C.dimWhite, fontSize: 14, fontWeight: 700, letterSpacing: 1.5, fontFamily: "monospace" }}>{t}</div>
      ))}
    </div>
  </GlassSurface>
);

const ShutterButton = ({ frame, animatePress = false, pressFrame = 0, innerColor = C.white, pulse = false }) => {
  let s = 1;
  if (animatePress && frame >= pressFrame && frame < pressFrame + 15) {
    s = frame <= pressFrame + 5 ? ease(frame, 1, 0.6, [pressFrame, pressFrame + 5]) : ease(frame, 0.6, 1, [pressFrame + 5, pressFrame + 15]);
  }
  if (pulse) s = 0.85 + Math.sin(frame * 0.12) * 0.15;
  return (
    <div style={{ width: 82, height: 82, borderRadius: 41, border: `3px solid ${C.cyan}`, display: "flex", alignItems: "center", justifyContent: "center", boxShadow: `0 0 30px ${C.cyanGlow}` }}>
      <div style={{ width: 64, height: 64, borderRadius: 32, background: innerColor, transform: `scale(${s})` }} />
    </div>
  );
};

const ScanLine = ({ frame, startFrame, endFrame }) => {
  const p = ease(frame, 0, 100, [startFrame, endFrame]);
  if (p <= 0 || p >= 100) return null;
  return <div style={{ position: "absolute", left: "5%", right: "5%", top: `${p}%`, height: 3, background: `linear-gradient(90deg, transparent, ${C.cyan}, transparent)`, boxShadow: `0 0 20px ${C.cyan}, 0 0 40px ${C.cyanGlow}`, zIndex: 5 }} />;
};

const ChatBubble = ({ text, isAi, label, opacity = 1, scale = 1, frame = 0 }) => (
  <div style={{ background: isAi ? C.cyanDim : C.glassBase, borderRadius: 14, padding: "13px 16px", marginBottom: 9, opacity, transform: `scale(${scale})`, border: isAi ? "none" : `1px solid ${C.glassBorder}` }}>
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 5 }}>
      <span style={{ fontSize: 11, fontWeight: 800, letterSpacing: 2, color: isAi ? C.neonGreen : C.cyan, fontFamily: "monospace" }}>{label}</span>
      {isAi && <SpeakerBadge frame={frame} color={C.neonGreen} />}
    </div>
    <div style={{ fontSize: 15, color: C.white, lineHeight: 1.5 }}>{text}</div>
    {isAi && <div style={{ marginTop: 5, fontSize: 13, color: "rgba(13,227,242,0.6)", fontWeight: 500 }}>Traducir</div>}
  </div>
);

const ThinkingDots = ({ frame }) => (
  <div style={{ display: "flex", alignItems: "center", gap: 6, padding: "6px 0" }}>
    {[0,1,2].map(i => <div key={i} style={{ width: 8, height: 8, borderRadius: 4, background: C.cyan, opacity: 0.3 + Math.sin(frame*0.18+i*1.4)*0.7, transform: `scale(${0.8+Math.sin(frame*0.18+i*1.4)*0.3})` }} />)}
    <span style={{ fontSize: 14, color: "rgba(13,227,242,0.5)", marginLeft: 7, fontFamily: "monospace" }}>Pensando...</span>
  </div>
);

const QaInputRow = ({ text = "", showCursor = false, frame }) => (
  <div style={{ display: "flex", alignItems: "center", gap: 8, background: C.glassBase, borderRadius: 26, border: `1.5px solid rgba(13,227,242,0.3)`, padding: "5px 16px" }}>
    <div style={{ flex: 1, fontSize: 15, color: text ? C.white : C.mutedWhite, padding: "9px 0", fontFamily: "monospace" }}>
      {text || "Pregunta sobre la imagen..."}
      {showCursor && <span style={{ color: C.cyan, opacity: Math.floor(frame/12)%2===0 ? 1 : 0 }}>|</span>}
    </div>
    <svg width="22" height="22" viewBox="0 0 24 24"><path d="M2 21L23 12L2 3V10L17 12L2 14V21Z" fill={text ? C.cyan : "rgba(13,227,242,0.25)"} /></svg>
  </div>
);

const PoweredByFooter = () => (
  <div style={{ paddingTop: 8, fontSize: 11, fontWeight: 500, letterSpacing: 2, color: C.mutedWhite, fontFamily: "monospace" }}>POWERED BY SMOLVLM2</div>
);

const SpeakerBadge = ({ frame, color = C.neonGreen }) => (
  <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 4 }}>
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" fill={color} stroke="none" />
      <path d="M15.54 8.46a5 5 0 0 1 0 7.07" opacity={0.5 + Math.sin(frame * 0.2) * 0.5} />
      <path d="M19.07 4.93a10 10 0 0 1 0 14.14" opacity={0.3 + Math.sin(frame * 0.2 + 0.5) * 0.4} />
    </svg>
    <span style={{ fontSize: 8, fontWeight: 800, color, letterSpacing: 2, fontFamily: "monospace", opacity: 0.6 + Math.sin(frame * 0.12) * 0.3 }}>VOZ ALTA</span>
  </div>
);

// Full camera screen inside phone
const CameraScreen = ({ frame, children, showControls = true, selectedMode = "FOTO", statusLabel, shutterProps = {} }) => (
  <div style={{ width: "100%", height: "100%", position: "relative" }}>
    <Img src={staticFile("escence.jpg")} style={{ width: "100%", height: "100%", objectFit: "cover", position: "absolute" }} />
    <CornerBrackets />
    <HeaderPill frame={frame} />
    {children}
    {showControls && (
      <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, display: "flex", flexDirection: "column", alignItems: "center", gap: 12, paddingBottom: 28, zIndex: 6 }}>
        <ModeSelectorPill selected={selectedMode} />
        <ShutterButton frame={frame} {...shutterProps} />
        {statusLabel && <div style={{ fontSize: 14, fontWeight: 700, letterSpacing: 2.5, color: C.cyan, fontFamily: "monospace" }}>{statusLabel}</div>}
      </div>
    )}
  </div>
);

// ─── SVG Icons (cyberpunk line style) ───
const CyberIcon = ({ type, size = 52, color = C.cyan }) => {
  const s = size;
  const icons = {
    camera: (
      <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
        <rect x="4" y="14" width="40" height="26" rx="4" stroke={color} strokeWidth="2.5" />
        <path d="M16 14L19 6H29L32 14" stroke={color} strokeWidth="2.5" strokeLinejoin="round" />
        <circle cx="24" cy="27" r="8" stroke={color} strokeWidth="2.5" />
        <circle cx="24" cy="27" r="3" fill={color} />
      </svg>
    ),
    video: (
      <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
        <rect x="4" y="12" width="28" height="24" rx="4" stroke={color} strokeWidth="2.5" />
        <path d="M32 20L44 13V35L32 28" stroke={color} strokeWidth="2.5" strokeLinejoin="round" />
        <circle cx="18" cy="24" r="4" fill={color} opacity="0.4" />
      </svg>
    ),
    loop: (
      <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
        <path d="M36 16C36 16 32 10 24 10C15 10 8 17 8 24C8 31 15 38 24 38C30 38 35 34 37 29" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
        <path d="M32 10L38 16L32 22" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="24" cy="24" r="4" fill={color} opacity="0.5" />
      </svg>
    ),
    chat: (
      <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
        <path d="M8 10H40C42 10 44 12 44 14V30C44 32 42 34 40 34H28L18 42V34H8C6 34 4 32 4 30V14C4 12 6 10 8 10Z" stroke={color} strokeWidth="2.5" strokeLinejoin="round" />
        <circle cx="16" cy="22" r="2.5" fill={color} />
        <circle cx="24" cy="22" r="2.5" fill={color} />
        <circle cx="32" cy="22" r="2.5" fill={color} />
      </svg>
    ),
    offline: (
      <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
        <path d="M6 18C11 12 17 9 24 9C31 9 37 12 42 18" stroke={color} strokeWidth="2.5" strokeLinecap="round" opacity="0.35" />
        <path d="M12 24C15 20 19 17 24 17C29 17 33 20 36 24" stroke={color} strokeWidth="2.5" strokeLinecap="round" opacity="0.55" />
        <path d="M18 30C20 28 22 26 24 26C26 26 28 28 30 30" stroke={color} strokeWidth="2.5" strokeLinecap="round" opacity="0.8" />
        <circle cx="24" cy="36" r="3" fill={color} />
        <line x1="8" y1="8" x2="40" y2="40" stroke={C.errorRed} strokeWidth="3" strokeLinecap="round" />
      </svg>
    ),
    voice: (
      <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
        <rect x="18" y="6" width="12" height="22" rx="6" stroke={color} strokeWidth="2.5" />
        <path d="M10 24C10 32 16 38 24 38C32 38 38 32 38 24" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
        <line x1="24" y1="38" x2="24" y2="44" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
        <line x1="18" y1="44" x2="30" y2="44" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
        <path d="M22 16H26" stroke={color} strokeWidth="1.5" strokeLinecap="round" opacity="0.6" />
        <path d="M22 20H26" stroke={color} strokeWidth="1.5" strokeLinecap="round" opacity="0.6" />
      </svg>
    ),
    haptic: (
      <svg width={s} height={s} viewBox="0 0 48 48" fill="none">
        <rect x="14" y="4" width="20" height="40" rx="4" stroke={color} strokeWidth="2.5" />
        <circle cx="24" cy="38" r="2" fill={color} opacity="0.5" />
        <path d="M8 18C6 20 5 22 5 24C5 26 6 28 8 30" stroke={color} strokeWidth="2" strokeLinecap="round" opacity="0.4" />
        <path d="M3 14C0 18 -1 22 -1 24C-1 28 0 30 3 34" stroke={color} strokeWidth="2" strokeLinecap="round" opacity="0.2" />
        <path d="M40 18C42 20 43 22 43 24C43 26 42 28 40 30" stroke={color} strokeWidth="2" strokeLinecap="round" opacity="0.4" />
        <path d="M45 14C48 18 49 22 49 24C49 28 48 30 45 34" stroke={color} strokeWidth="2" strokeLinecap="round" opacity="0.2" />
        <rect x="18" y="14" width="12" height="18" rx="2" stroke={color} strokeWidth="1.5" opacity="0.5" />
      </svg>
    ),
  };
  return icons[type] || null;
};

// ─── Feature label that appears next to the phone ───
const FeatureLabel = ({ title, desc, iconType, frame, delay = 0, position = "right", color = C.cyan }) => {
  const { fps } = useVideoConfig();
  const s = spring({ frame: Math.max(0, frame - delay), fps, config: { damping: 12, stiffness: 70 } });
  const opacity = ease(frame, 0, 1, [delay, delay + 12]);
  const isRight = position === "right";
  const slideX = interpolate(s, [0, 1], [isRight ? 80 : -80, 0]);

  // Icon pulse when entering
  const iconScale = frame > delay && frame < delay + 25 ? 0.9 + Math.sin((frame - delay) * 0.25) * 0.15 : 1;

  return (
    <div style={{
      position: "absolute",
      [isRight ? "right" : "left"]: 40,
      top: "50%", transform: `translateY(-50%) translateX(${slideX}px)`,
      opacity, maxWidth: 440,
      textAlign: isRight ? "left" : "right",
      display: "flex", flexDirection: "column",
      alignItems: isRight ? "flex-start" : "flex-end",
    }}>
      <div style={{ marginBottom: 16, transform: `scale(${iconScale})` }}>
        <CyberIcon type={iconType} size={52} color={color} />
      </div>
      <div style={{
        fontSize: 38, fontWeight: 900, fontFamily: "monospace", letterSpacing: 4,
        color, textShadow: `0 0 25px ${color}`,
        marginBottom: 12, lineHeight: 1.2,
      }}>{title}</div>
      <div style={{ fontSize: 22, color: C.dimWhite, lineHeight: 1.5, fontFamily: "sans-serif" }}>{desc}</div>
    </div>
  );
};

// ═══════════════════════════════════════════
// SCENE 1: INTRO (0-3.5s = 0-105)
// ═══════════════════════════════════════════

const IntroScene = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const logoScale = spring({ frame: Math.max(0, frame-3), fps, config: { damping: 11, stiffness: 70 } });
  const logoOp = ease(frame, 0, 1, [0, 18]);
  const eyeP = ease(frame, 0, 1, [5, 40]);
  const eyeDash = 300 - eyeP * 300;
  const subOp = ease(frame, 0, 1, [44, 58]);
  const subSlide = ease(frame, 40, 0, [44, 58]);
  const badgeOp = ease(frame, 0, 1, [62, 74]);
  const badgeScale = spring({ frame: Math.max(0, frame-62), fps, config: { damping: 10, stiffness: 100 } });
  const outFade = ease(frame, 1, 0, [90, 105]);

  return (
    <AbsoluteFill style={{ background: C.darkBg, justifyContent: "center", alignItems: "center", opacity: outFade }}>
      <GridBg />
      <svg width="280" height="280" viewBox="0 0 108 108" style={{ marginBottom: 40, opacity: logoOp }}>
        <path d="M26,50 Q54,24 82,50 Q54,76 26,50Z" fill="none" stroke={C.cyan} strokeWidth="2.5" strokeDasharray="300" strokeDashoffset={eyeDash} />
        <circle cx="54" cy="50" r="12" fill="none" stroke={C.cyan} strokeWidth="2" opacity={eyeP} />
        <circle cx="54" cy="50" r="5" fill={C.cyan} opacity={eyeP} />
        <circle cx="49" cy="46" r="2" fill={C.white} opacity={eyeP} />
        {[[26,50,18,50],[82,50,90,50],[54,38,54,32],[54,62,54,68]].map(([x1,y1,x2,y2],i) =>
          <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke={C.cyan} strokeWidth="1.2" opacity={eyeP} />
        )}
        <path d="M66,74 L80,74 Q84,74 84,78 L84,86 Q84,90 80,90 L72,90 L68,94 L68,90 L66,90 Q62,90 62,86 L62,78 Q62,74 66,74Z" fill="none" stroke={C.neonGreen} strokeWidth="1.5" opacity={eyeP} />
        {[68,73,78].map(cx => <circle key={cx} cx={cx} cy="82" r="1.5" fill={C.neonGreen} opacity={eyeP} />)}
      </svg>
      <div style={{ fontSize: 100, fontWeight: 900, fontFamily: "monospace", letterSpacing: 7, color: C.cyan, textShadow: `0 0 50px ${C.cyan}, 0 0 100px rgba(13,227,242,0.2)`, opacity: logoOp, transform: `scale(${logoScale})` }}>SCENESENSE</div>
      <div style={{ fontSize: 28, color: C.dimWhite, fontFamily: "monospace", letterSpacing: 5, marginTop: 16, opacity: subOp, transform: `translateY(${subSlide}px)` }}>SEE MORE. ASK MORE.</div>
      <div style={{ marginTop: 36, padding: "12px 32px", borderRadius: 26, border: `2px solid ${C.neonGreen}60`, background: "rgba(0,255,136,0.06)", opacity: badgeOp, transform: `scale(${badgeScale})` }}>
        <span style={{ fontSize: 20, color: C.neonGreen, fontFamily: "monospace", fontWeight: 700, letterSpacing: 3, textShadow: `0 0 12px ${C.neonGreen}` }}>100% ON-DEVICE AI</span>
      </div>
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// SCENE 2: FOTO MODE (3.5s-7.5s = 105-225)
// Phone on LEFT, label on RIGHT
// ═══════════════════════════════════════════

const PhotoFeature = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ps = spring({ frame, fps, config: { damping: 13, stiffness: 50 } });
  const rotY = interpolate(ps, [0, 1], [-25, 5]);
  const sc = interpolate(ps, [0, 1], [0.55, 0.92]);
  const op = ease(frame, 0, 1, [0, 15]);
  const sway = Math.sin(frame * 0.025) * 1.5;

  const flash = frame >= 50 && frame <= 60 ? ease(frame, 0, 0.7, [50, 54]) * ease(frame, 1, 0, [54, 60]) : 0;
  const scanActive = frame >= 62 && frame < 100;
  const procOp = ease(frame, 0, 1, [55, 62]) * ease(frame, 1, 0, [95, 102]);
  const resultOp = ease(frame, 0, 1, [80, 90]) * ease(frame, 1, 0, [100, 108]);

  return (
    <AbsoluteFill style={{ background: C.darkBg }}>
      <GridBg />
      <div style={{ position: "absolute", left: 30, top: "50%", transform: "translateY(-50%)" }}>
        <PhoneMockup rotateY={rotY + sway} rotateX={-1} scale={sc} opacity={op}>
          <CameraScreen frame={frame} selectedMode="FOTO" statusLabel={procOp > 0 ? "PROCESANDO..." : null}
            shutterProps={{ animatePress: true, pressFrame: 50 }}>
            {scanActive && <ScanLine frame={frame} startFrame={62} endFrame={98} />}
            {flash > 0 && <div style={{ position: "absolute", inset: 0, background: C.white, opacity: flash, zIndex: 8 }} />}
            {resultOp > 0 && (
              <div style={{ position: "absolute", bottom: 180, left: 12, right: 12, zIndex: 6, opacity: resultOp }}>
                <GlassSurface borderRadius={20}>
                  <div style={{ padding: 14 }}>
                    <SpeakerBadge frame={frame} color={C.neonGreen} />
                    <div style={{ fontSize: 14, color: C.white, lineHeight: 1.5 }}>A corner building with ornate balconies and warm restaurant lights at dusk.</div>
                    <PoweredByFooter />
                  </div>
                </GlassSurface>
              </div>
            )}
          </CameraScreen>
        </PhoneMockup>
      </div>
      <FeatureLabel frame={frame} delay={10} position="right" iconType="camera" title="MODO IMAGEN" desc="Toma una foto y obtén una descripción detallada de la escena al instante" color={C.cyan} />
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// SCENE 3: VIDEO MODE (7.5s-11s = 225-330)
// Phone on RIGHT, label on LEFT
// ═══════════════════════════════════════════

const VideoFeature = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ps = spring({ frame, fps, config: { damping: 13, stiffness: 50 } });
  const rotY = interpolate(ps, [0, 1], [25, -5]);
  const sc = interpolate(ps, [0, 1], [0.55, 0.92]);
  const op = ease(frame, 0, 1, [0, 15]);
  const sway = Math.sin(frame * 0.025) * -1.5;

  const isRec = frame >= 40;
  const recDot = isRec ? 0.5 + Math.sin(frame * 0.15) * 0.5 : 0;

  return (
    <AbsoluteFill style={{ background: C.darkBg }}>
      <GridBg />
      <div style={{ position: "absolute", right: 30, top: "50%", transform: "translateY(-50%)" }}>
        <PhoneMockup rotateY={rotY + sway} rotateX={1} scale={sc} opacity={op}>
          <CameraScreen frame={frame} selectedMode="VIDEO"
            statusLabel={isRec ? "GRABANDO..." : null}
            shutterProps={{ innerColor: isRec ? C.errorRed : C.white, pulse: isRec }}>
            {/* REC indicator */}
            {isRec && (
              <div style={{ position: "absolute", top: 100, right: 18, display: "flex", alignItems: "center", gap: 6, zIndex: 10 }}>
                <div style={{ width: 10, height: 10, borderRadius: 5, background: C.errorRed, opacity: recDot }} />
                <span style={{ fontSize: 13, color: C.errorRed, fontWeight: 700, fontFamily: "monospace", letterSpacing: 1 }}>REC 0:{String(Math.floor((frame - 40) / 30)).padStart(2, '0')}</span>
              </div>
            )}
          </CameraScreen>
        </PhoneMockup>
      </div>
      <FeatureLabel frame={frame} delay={10} position="left" iconType="video" title="MODO VIDEO" desc="Graba un clip y analiza acciones, movimiento y cambios entre frames" color={C.cyan} />
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// SCENE 4: CONTINUOUS MODE (11s-14.5s = 330-435)
// Phone on LEFT, label on RIGHT
// ═══════════════════════════════════════════

const ContinuousFeature = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ps = spring({ frame, fps, config: { damping: 13, stiffness: 50 } });
  const rotY = interpolate(ps, [0, 1], [-25, 5]);
  const sc = interpolate(ps, [0, 1], [0.55, 0.92]);
  const op = ease(frame, 0, 1, [0, 15]);
  const sway = Math.sin(frame * 0.03) * 2;

  const isActive = frame >= 35;
  const count = isActive ? Math.min(Math.floor((frame - 35) / 25) + 1, 4) : 0;
  const resultOp = ease(frame, 0, 1, [55, 65]);

  return (
    <AbsoluteFill style={{ background: C.darkBg }}>
      <GridBg />
      <div style={{ position: "absolute", left: 30, top: "50%", transform: "translateY(-50%)" }}>
        <PhoneMockup rotateY={rotY + sway} rotateX={-1} scale={sc} opacity={op}>
          <CameraScreen frame={frame} selectedMode="CONTINUO"
            statusLabel={isActive ? `CONTINUO... (${count})` : null}
            shutterProps={{ innerColor: isActive ? C.cyan : C.white, pulse: isActive }}>
            {/* Pulsing scan lines in continuous mode */}
            {isActive && (
              <div style={{ position: "absolute", inset: 0, zIndex: 4, opacity: 0.3 + Math.sin(frame * 0.1) * 0.2 }}>
                <div style={{ position: "absolute", top: `${30 + Math.sin(frame * 0.08) * 15}%`, left: "5%", right: "5%", height: 2, background: `linear-gradient(90deg, transparent, ${C.cyan}, transparent)` }} />
              </div>
            )}
            {/* Result sheet */}
            {resultOp > 0 && (
              <div style={{ position: "absolute", bottom: 180, left: 12, right: 12, zIndex: 6, opacity: resultOp }}>
                <GlassSurface borderRadius={20}>
                  <div style={{ padding: 14 }}>
                    <SpeakerBadge frame={frame} color={C.neonGreen} />
                    <div style={{ fontSize: 14, color: C.white, lineHeight: 1.5 }}>[Frame {count}] A street corner at dusk with warm restaurant lights and pedestrians.</div>
                    <PoweredByFooter />
                  </div>
                </GlassSurface>
              </div>
            )}
          </CameraScreen>
        </PhoneMockup>
      </div>
      <FeatureLabel frame={frame} delay={10} position="right" iconType="loop" title="MODO CONTINUO" desc="Deja la cámara activa y recibe descripciones continuas en tiempo real" color={C.cyan} />
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// SCENE 5: Q&A CHAT (14.5s-19.5s = 435-585)
// Phone on RIGHT, label on LEFT
// ═══════════════════════════════════════════

const ChatFeature = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ps = spring({ frame, fps, config: { damping: 13, stiffness: 50 } });
  const rotY = interpolate(ps, [0, 1], [25, -5]);
  const sc = interpolate(ps, [0, 1], [0.55, 0.92]);
  const op = ease(frame, 0, 1, [0, 15]);
  const sway = Math.sin(frame * 0.025) * -1.5;

  const sheetSpring = spring({ frame: Math.max(0, frame-15), fps, config: { damping: 13, stiffness: 60 } });
  const sheetSlideY = interpolate(sheetSpring, [0, 1], [200, 0]);

  const msg1Op = ease(frame, 0, 1, [25, 38]);
  const msg1S = spring({ frame: Math.max(0, frame-25), fps, config: { damping: 11, stiffness: 75 } });

  const qText = "What restaurant is on the corner?";
  const tStart = 55, tEnd = 85;
  const tProg = ease(frame, 0, 1, [tStart, tEnd]);
  const typed = qText.slice(0, Math.floor(tProg * qText.length));
  const isTyp = frame >= tStart && frame < tEnd + 5;
  const sent = frame >= tEnd + 8;
  const msg2Op = ease(frame, 0, 1, [tEnd+8, tEnd+18]);
  const msg2S = spring({ frame: Math.max(0, frame-tEnd-8), fps, config: { damping: 11, stiffness: 75 } });

  const thS = tEnd + 20, thE = thS + 40;
  const thOp = (frame >= thS && frame < thE) ? ease(frame, 0, 1, [thS, thS+8]) * ease(frame, 1, 0, [thE-8, thE]) : 0;

  const aF = thE;
  const msg3Op = ease(frame, 0, 1, [aF, aF+12]);
  const msg3S = spring({ frame: Math.max(0, frame-aF), fps, config: { damping: 11, stiffness: 75 } });

  return (
    <AbsoluteFill style={{ background: C.darkBg }}>
      <GridBg />
      <div style={{ position: "absolute", right: 30, top: "50%", transform: "translateY(-50%)" }}>
        <PhoneMockup rotateY={rotY + sway} rotateX={1} scale={sc} opacity={op}>
          <div style={{ width: "100%", height: "100%", position: "relative" }}>
            <Img src={staticFile("escence.jpg")} style={{ width: "100%", height: "100%", objectFit: "cover", position: "absolute" }} />
            <CornerBrackets />
            <HeaderPill frame={frame} />
            <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, padding: "0 12px", transform: `translateY(${sheetSlideY}px)`, zIndex: 6 }}>
              <GlassSurface borderRadius={22} style={{ borderBottomLeftRadius: 0, borderBottomRightRadius: 0, maxHeight: 520 }}>
                <div style={{ padding: 16 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
                    <span style={{ fontSize: 14, fontWeight: 800, color: C.cyan, letterSpacing: 2.5, fontFamily: "monospace" }}>CHAT Q&A</span>
                    <span style={{ fontSize: 12, fontWeight: 600, color: C.mutedWhite, letterSpacing: 1.5, fontFamily: "monospace" }}>{sent ? 1 : 0}/3 PREGUNTAS</span>
                  </div>
                  <ChatBubble isAi label="SCENESENSE" text="A corner building with ornate balconies and a restaurant. Warm lights at dusk." opacity={msg1Op} scale={interpolate(msg1S, [0,1], [0.9,1])} frame={frame} />
                  {sent && <ChatBubble isAi={false} label="TU" text={qText} opacity={msg2Op} scale={interpolate(msg2S, [0,1], [0.9,1])} />}
                  {thOp > 0 && <div style={{ opacity: thOp }}><ThinkingDots frame={frame} /></div>}
                  {msg3Op > 0 && <ChatBubble isAi label="SCENESENSE" text={'The restaurant is "Martin" — yellow awning, pizza and pasta.'} opacity={msg3Op} scale={interpolate(msg3S, [0,1], [0.9,1])} frame={frame} />}
                  <div style={{ marginTop: 10 }}>
                    {!sent ? <QaInputRow text={typed} showCursor={isTyp} frame={frame} /> : <QaInputRow text="" frame={frame} />}
                  </div>
                  <PoweredByFooter />
                </div>
              </GlassSurface>
            </div>
          </div>
        </PhoneMockup>
      </div>
      <FeatureLabel frame={frame} delay={10} position="left" iconType="chat" title="CHAT Q&A" desc="Pregunta lo que quieras sobre cualquier imagen o video que captures" color={C.neonGreen} />
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// SCENE 6: VOICE CONTROL (19.5s-22.5s = 585-675)
// Phone LEFT, voice waveform inside phone + label RIGHT
// ═══════════════════════════════════════════

const VoiceFeature = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ps = spring({ frame, fps, config: { damping: 13, stiffness: 50 } });
  const rotY = interpolate(ps, [0, 1], [-25, 5]);
  const sc = interpolate(ps, [0, 1], [0.55, 0.92]);
  const op = ease(frame, 0, 1, [0, 15]);
  const sway = Math.sin(frame * 0.025) * 1.5;

  // Voice command appears
  const voiceActive = frame >= 30;
  const commandOp = ease(frame, 0, 1, [35, 48]);
  const responseOp = ease(frame, 0, 1, [60, 73]);

  return (
    <AbsoluteFill style={{ background: C.darkBg }}>
      <GridBg />
      <div style={{ position: "absolute", left: 30, top: "50%", transform: "translateY(-50%)" }}>
        <PhoneMockup rotateY={rotY + sway} rotateX={-1} scale={sc} opacity={op}>
          <CameraScreen frame={frame} selectedMode="FOTO" showControls={!voiceActive}>
            {/* Voice overlay on phone */}
            {voiceActive && (
              <div style={{ position: "absolute", bottom: 0, left: 0, right: 0, zIndex: 10 }}>
                {/* Voice waveform bar */}
                <div style={{ background: "rgba(0,0,0,0.7)", padding: "14px 16px 10px" }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 3, marginBottom: 10 }}>
                    {Array.from({ length: 20 }).map((_, i) => (
                      <div key={i} style={{
                        width: 3, borderRadius: 2,
                        height: voiceActive ? 6 + Math.sin(frame * 0.2 + i * 0.7) * 16 : 4,
                        background: C.neonGreen,
                        opacity: 0.3 + Math.sin(frame * 0.15 + i * 0.5) * 0.5,
                      }} />
                    ))}
                  </div>
                  <div style={{ textAlign: "center", fontSize: 11, color: C.neonGreen, fontFamily: "monospace", fontWeight: 700, letterSpacing: 2, opacity: 0.5 + Math.sin(frame * 0.1) * 0.3 }}>
                    LISTENING...
                  </div>
                </div>

                {/* Voice command bubble */}
                {commandOp > 0 && (
                  <div style={{ background: C.glassBase, padding: "12px 16px", opacity: commandOp }}>
                    <div style={{ fontSize: 10, fontWeight: 800, color: C.neonGreen, letterSpacing: 2, fontFamily: "monospace", marginBottom: 4 }}>VOZ DETECTADA</div>
                    <div style={{ fontSize: 14, color: C.white, fontFamily: "monospace" }}>"Describe lo que ves"</div>
                  </div>
                )}

                {/* Response — spoken aloud */}
                {responseOp > 0 && (
                  <GlassSurface borderRadius={0} style={{ padding: "12px 16px", opacity: responseOp, borderTop: `1px solid ${C.cyan}30` }}>
                    <SpeakerBadge frame={frame} color={C.neonGreen} />
                    <div style={{ fontSize: 13, color: C.white, lineHeight: 1.5 }}>A corner building with ornate balconies and warm restaurant lights at dusk.</div>
                    <PoweredByFooter />
                  </GlassSurface>
                )}
              </div>
            )}
          </CameraScreen>
        </PhoneMockup>
      </div>
      <FeatureLabel frame={frame} delay={10} position="right" iconType="voice" title="CONTROL POR VOZ" desc="Manos libres: di lo que necesitas y SceneSense lo hace por ti" color={C.neonGreen} />
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// SCENE 7: HAPTIC + OFFLINE (22.5s-25.5s = 675-765)
// Phone RIGHT, haptic visualization + label LEFT
// ═══════════════════════════════════════════

const HapticOfflineFeature = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const ps = spring({ frame, fps, config: { damping: 13, stiffness: 50 } });
  const rotY = interpolate(ps, [0, 1], [25, -5]);
  const sc = interpolate(ps, [0, 1], [0.55, 0.92]);
  const op = ease(frame, 0, 1, [0, 15]);
  const sway = Math.sin(frame * 0.025) * -1.5;

  // Haptic pulses radiating from phone
  const pulseActive = frame >= 25;
  const pulseCount = pulseActive ? Math.floor((frame - 25) / 18) : 0;

  // Offline badge
  const offlineOp = ease(frame, 0, 1, [50, 63]);
  const offlineS = spring({ frame: Math.max(0, frame-50), fps, config: { damping: 10, stiffness: 80 } });

  return (
    <AbsoluteFill style={{ background: C.darkBg }}>
      <GridBg />
      <div style={{ position: "absolute", right: 30, top: "50%", transform: "translateY(-50%)" }}>
        {/* Haptic ripple rings around phone */}
        {pulseActive && [0, 1, 2].map(i => {
          const pFrame = frame - 25 - i * 18;
          if (pFrame < 0 || pFrame > 40) return null;
          const ringScale = 1 + pFrame * 0.025;
          const ringOp = 1 - pFrame / 40;
          return (
            <div key={i} style={{
              position: "absolute", inset: 0,
              display: "flex", justifyContent: "center", alignItems: "center",
              pointerEvents: "none", zIndex: 1,
            }}>
              <div style={{
                width: 420 * sc, height: 860 * sc, borderRadius: 48 * sc,
                border: `2px solid ${C.cyan}`,
                opacity: ringOp * 0.4,
                transform: `scale(${ringScale})`,
              }} />
            </div>
          );
        })}

        <PhoneMockup rotateY={rotY + sway} rotateX={1} scale={sc} opacity={op}>
          <CameraScreen frame={frame} selectedMode="FOTO">
            {/* Vibration indicator on screen */}
            {pulseActive && (
              <div style={{
                position: "absolute", bottom: 200, left: 0, right: 0,
                display: "flex", justifyContent: "center", zIndex: 8,
              }}>
                <GlassSurface borderRadius={20} style={{ padding: "10px 20px", display: "flex", alignItems: "center", gap: 10 }}>
                  <CyberIcon type="haptic" size={28} color={C.cyan} />
                  <span style={{
                    fontSize: 13, fontWeight: 700, color: C.cyan,
                    fontFamily: "monospace", letterSpacing: 2,
                    opacity: 0.5 + Math.sin(frame * 0.15) * 0.4,
                  }}>
                    {["BOTON DETECTADO", "VIBRACION ENVIADA", "RESULTADO LISTO"][pulseCount % 3]}
                  </span>
                </GlassSurface>
              </div>
            )}
          </CameraScreen>
        </PhoneMockup>
      </div>

      {/* Left labels: both haptic and offline stacked */}
      <div style={{ position: "absolute", left: 40, top: "50%", transform: "translateY(-60%)", maxWidth: 420 }}>
        <FeatureLabel frame={frame} delay={10} position="left" iconType="haptic" title="VIBRACIONES" desc="Siente cada botón con vibración háptica y navega sin mirar la pantalla" color={C.cyan} />
      </div>

      {/* Offline badge bottom-left */}
      <div style={{
        position: "absolute", left: 50, bottom: 120,
        opacity: offlineOp, transform: `scale(${interpolate(offlineS, [0, 1], [0.85, 1])})`,
        display: "flex", alignItems: "center", gap: 14,
      }}>
        <CyberIcon type="offline" size={40} color={C.cyan} />
        <div>
          <div style={{ fontSize: 22, fontWeight: 900, fontFamily: "monospace", letterSpacing: 3, color: C.cyan, textShadow: `0 0 15px ${C.cyan}` }}>100% OFFLINE</div>
          <div style={{ fontSize: 16, color: C.dimWhite, marginTop: 4 }}>Úsalo sin internet después de instalarlo</div>
        </div>
      </div>
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// SCENE 7: OUTRO CTA (24s-30s = 720-900)
// ═══════════════════════════════════════════

const OutroScene = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const inFade = ease(frame, 0, 1, [0, 15]);
  const logoScale = spring({ frame: Math.max(0, frame-3), fps, config: { damping: 10, stiffness: 60 } });

  const pills = ["ON-DEVICE AI", "FOTO + VIDEO", "Q&A CHAT", "CONTROL POR VOZ", "VIBRACIONES", "100% OFFLINE"];
  const pillsOp = ease(frame, 0, 1, [20, 38]);
  const ctaOp = ease(frame, 0, 1, [45, 58]);
  const ctaScale = spring({ frame: Math.max(0, frame-45), fps, config: { damping: 10, stiffness: 80 } });
  const footerOp = ease(frame, 0, 1, [65, 80]);
  const devOp = ease(frame, 0, 1, [80, 95]);

  return (
    <AbsoluteFill style={{ background: C.darkBg, justifyContent: "center", alignItems: "center", opacity: inFade }}>
      <GridBg />
      <div style={{ position: "absolute", width: 700, height: 700, borderRadius: "50%", background: `radial-gradient(circle, rgba(13,227,242,0.06), transparent 70%)` }} />

      <div style={{ fontSize: 90, fontWeight: 900, fontFamily: "monospace", letterSpacing: 6, color: C.cyan, textShadow: `0 0 50px ${C.cyan}, 0 0 100px rgba(13,227,242,0.2)`, transform: `scale(${logoScale})` }}>SCENESENSE</div>

      <div style={{ display: "flex", gap: 12, marginTop: 44, opacity: pillsOp, flexWrap: "wrap", justifyContent: "center", padding: "0 60px" }}>
        {pills.map((p, i) => {
          const pS = spring({ frame: Math.max(0, frame-20-i*4), fps, config: { damping: 10, stiffness: 90 } });
          return (
            <div key={p} style={{ transform: `scale(${pS})` }}>
              <GlassSurface borderRadius={22}>
                <div style={{ padding: "10px 20px", fontSize: 14, color: C.cyan, fontFamily: "monospace", fontWeight: 700, letterSpacing: 1.5 }}>{p}</div>
              </GlassSurface>
            </div>
          );
        })}
      </div>

      <div style={{ fontSize: 28, fontWeight: 800, fontFamily: "monospace", marginTop: 50, letterSpacing: 3, color: C.neonGreen, textShadow: `0 0 20px ${C.neonGreen}`, opacity: ctaOp, transform: `scale(${ctaScale})` }}>
        COMING SOON
      </div>

      <div style={{ marginTop: 40, fontSize: 13, color: C.mutedWhite, fontFamily: "monospace", letterSpacing: 2.5, fontWeight: 500, opacity: footerOp }}>
        POWERED BY SMOLVLM2 + LLAMA.CPP
      </div>

      <div style={{ marginTop: 30, opacity: devOp, display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
        <div style={{ fontSize: 14, color: C.dimWhite, fontFamily: "sans-serif", fontWeight: 500 }}>Desarrollado por <span style={{ color: C.white, fontWeight: 700 }}>Alberto Espinosa</span></div>
        <div style={{ fontSize: 18, fontWeight: 800, fontFamily: "monospace", letterSpacing: 3, color: C.cyan, textShadow: `0 0 12px ${C.cyan}` }}>SinergIA</div>
      </div>
    </AbsoluteFill>
  );
};

// ═══════════════════════════════════════════
// MAIN COMPOSITION (30s = 900 frames @ 30fps)
// ═══════════════════════════════════════════

export const PromoVideo = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    <AudioLayer />
    <Sequence from={0} durationInFrames={105}><IntroScene /></Sequence>
    <Sequence from={105} durationInFrames={110}><PhotoFeature /></Sequence>
    <Sequence from={215} durationInFrames={100}><VideoFeature /></Sequence>
    <Sequence from={315} durationInFrames={100}><ContinuousFeature /></Sequence>
    <Sequence from={415} durationInFrames={140}><ChatFeature /></Sequence>
    <Sequence from={555} durationInFrames={105}><VoiceFeature /></Sequence>
    <Sequence from={660} durationInFrames={105}><HapticOfflineFeature /></Sequence>
    <Sequence from={765} durationInFrames={135}><OutroScene /></Sequence>
  </AbsoluteFill>
);
