# Changelog 🐾

All notable changes to **KitStickerStudio** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to Semantic Versioning.

## [1.0.0-plump] - 2026-11-21

### Added ✨
- **Standalone Android Architecture**:
  - Independent repository, Gradle 8.5, Kotlin 1.9.22, targeting API 34 (OnePlus 11 5G NA).
- **Preset Bases (`BasePresetEngine.kt`)**:
  - Built-in color transformations: **Green Face 🟢**, **Purple Face 💜**, **Kawaii Tint 🌸**, and **High Contrast ⚡**.
  - Adjustable intensity slider (0–100%) and clean alpha transparency preservation.
- **Emoji Modifiers (`ModifierOverlayRenderer.kt`, `ModifierBounds.kt`)**:
  - Vector overlays: **Cat Ears 🐱**, **Devil Horns 😈**, **Kawaii Blush 🌸**, and **Sparkles ✨**.
  - Proportional bounds calculation, horizontal flip (`⇄`), and scale adjustments (0.5x–1.5x).
- **Sticker Studio Engine & Generators (`KitStickerStudioEngine.kt`)**:
  - Single sticker 512x512 rendering.
  - Export to PNG (`.png`) and WebP (`.webp`).
  - Single-Image Expression Pack Generator (6 uniform stickers).
  - 8-frame sinusoidal Emoji Animator loop.
- **High-Density Rounded Plumpitude UI (`MainActivity.kt`, `activity_main.xml`)**:
  - Live preview stage with instant chip selectors.
  - Sticky preference persistence via `StickerStudioPreferences.kt`.
- **Unit Test Suite (`KitStickerStudioTest.kt`)**:
  - 100% test coverage for presets, modifier math, horizontal flip, and export format specifications.
