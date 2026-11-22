```
   /\_/\  
  ( o.o )  KIT-STICKER-STUDIO 🐾 [Plumpitude Engine v3.1]
   > ^ <   Standalone Android / Kotlin • OnePlus 11 5G NA Build
```

# KitStickerStudio 🐾🎨

**KitStickerStudio** is a standalone, power-user Android application and media/emoji picker engine designed for creating custom stickers, expression packs, and animated loops with pixel-perfect transformations and proportional vector overlays.

Targeting **Android 14 (API 34) / OnePlus 11 5G (NA build)** with **Gradle 8.5** and **Kotlin 1.9.22**, it runs 100% on-device with zero external dependencies, zero cloud roundtrips, and zero telemetry.

---

## 🌸 Key Pillars & Core Engines

### 1. Base Preset Engine (`BasePresetEngine.kt`)
Performs real-time, alpha-preserving color space transformations:
- **Original (`✨`)**: Clean baseline preservation with full dynamic range.
- **Green Face (`🟢`)**: Hue shift targeting standard yellow/warm face ranges (~20°–75° HSV) transitioning to vibrant green (110° HSV) while retaining mouth/eye features and transparent margins.
- **Purple Face (`💜`)**: Kawaiian Plum/Lavender palette shift (`#2E1A47` / `#9C27B0`) with boosted saturation.
- **Kawaii Tint (`🌸`)**: Elevated pastel warmth and brightness blending soft pink/lavender tones.
- **High Contrast (`⚡`)**: Contrast stretch inflection with punchy dark and light separation.
- **Adjustable Intensity Slider**: Continuous scaling from `0.0f` to `1.0f` with instant live preview.

### 2. Proportional Modifier Overlay Renderer (`ModifierOverlayRenderer.kt`)
Lightweight, anti-slop vector overlays rendered directly on canvas without bitmap artifacts:
- **Cat Ears (`🐱`)**: Plush Kawaiian cat ears with Dark Slate (`#1E1E2E`) contouring, Deep Indigo (`#2E1A47`) outer fill, and soft pink (`#FFB7C5`) inner shading.
- **Devil Horns (`😈`)**: Ruby-red curved demon horns with deep crimson borders.
- **Kawaii Blush (`🌸`)**: Semi-transparent cheek highlights (`#80FF69B4`).
- **Sparkles (`✨`)**: Amber Gold (`#FFFBEB`) 4-pointed ambient sparkle stars.
- **Scaling, Offsetting & Horizontal Flipping**: Proportional bounds calculation (`ModifierBounds.kt`) with horizontal flip (`canvas.scale(-1f, 1f, cx, cy)`) and custom scale factors.

### 3. Output Formats & Generators (`KitStickerStudioEngine.kt`)
- **Single Sticker Export**: Standard square `512x512` formatted for WhatsApp/Telegram/Signal sticker protocols in lossless WebP and PNG.
- **Single-Image Emoji Pack Generator**: Applies chosen preset and modifiers uniformly across 6 core expressions (`😊`, `😸`, `😻`, `🤪`, `🥺`, `🔥`) in a single tap.
- **Emoji Animator Loop**: 8-frame sinusoidal bounce and ear-twitch loop rendered to memory for GIF/WebP preview.
- **Persistence (`StickerStudioPreferences.kt`)**: Remembers last-used preset, modifier, scale, flip, and intensity across app restarts.

---

## 🐾 High-Density Rounded Plumpitude UI Standards

- **Theme Palette**: Deep Indigo (`#2E1A47`), Soft Lavender (`#E6E6FA`), Crisp Dark Slate (`#1E1E2E`), and Amber Gold (`#FFFBEB`).
- **Rounding & Shadows**: 24px and 12px pill rounding, zero blurred shadows, crisp 1px/2px borders.
- **Status Indicators**:
  - `✨` `[status: ready]` — Sticker compiled and ready for export
  - `🐾` `[status: processing]` — Real-time frame loop rendering
  - `💜` `[#zettelkasten]` — Persisted preference state in SharedPreferences

---

## 🛠️ Build & Verification

```powershell
# Run unit test suite
./gradlew.bat testDebugUnitTest

# Assemble APK for OnePlus 11 5G
./gradlew.bat assembleDebug
```

---

## ⚖️ Open Source & License

MIT License. Designed with reusability, modularity, and 100% on-device safety.
