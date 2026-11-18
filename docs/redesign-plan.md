# RexPocket redesign plan

Purpose: turn RexPocket from an iOS-flavoured hybrid into a native, bold Material 3 Expressive Android app, restructured as well as restyled. References: Harmonic-HN and Orion Store for look and feel; Google's `android/skills` (adaptive, navigation-3, navigation-event, edge-to-edge, compose-performance, testing-setup, agp-9-upgrade) for platform rules. Haptics follow the owner's rule, not either reference.

Status: v2, 2026-11-19, after an independent review (7 blockers, 20 should-fix, all folded in below). Branch `feat/on-upstream` (our own fork; no upstream PR).

## Overview

What is wrong today:

- `Theme.kt` is Apple's system palette (`F2F2F7`, `1C1C1E`, `0A84FF`, `30D158`, `FF453A`, `98989F`). Material is a thin wrapper over a private `Colors` object.
- iOS idioms: plated floating pills on the host list, grouped-card `ActionSheet`s for every menu, hairline cards.
- No Material type scale, dynamic colour, motion tokens or Material Symbols.
- Settings was a sheet behind the host list's pill (patched in `66ae0f1`).
- Hand-rolled split layout (`Adaptive.kt`), `configChanges` opt-out, a two-value `Route`.

Kept as is: the terminal engine (libghostty view, IME slide, hardware keyboard, shortcuts, tooltips, pointers), the connection model in `RexViewModel`, fonts, uploads, Tailscale, SSH.

## Principles

1. **Material, fully.** Colours from scheme roles, type from the scale, motion from `motionScheme`. No hex or sp literals in screens.
2. **Bold.** Expressive shapes, emphasized type, tonal colour blocks, shape-morphing loaders.
3. **The terminal is the hero.** Its grid never resizes for chrome; chrome overlays it or has a fixed height.
4. **Controls live beside what they control**, and within thumb reach on a phone.
5. **Every state designed**, including blips: a reconnect must not flash the pane.

## Screen map (where things go)

Navigation 3 back stack owned by `RexViewModel` (it already drives `route` from `enterSession`, `disconnect`, `follow`, `openAlert`, `restore`). `ListDetailSceneStrategy` for hosts/terminal; `DialogSceneStrategy` for settings on large windows.

| Destination (`NavKey`) | Phone | Large (≥840dp wide and not compact height) |
|---|---|---|
| `Hosts` | full screen | list pane, 360dp cap, collapsible by dragging the divider (`paneExpansionState`) |
| `Terminal(hostId, sessionId)` | full screen, back disk | detail pane, no back button |
| `Settings` (hosts its own nested list-detail, as Harmonic's `SettingsNavigationShell`) | full screen | dialog scene over the two panes, so the terminal stays composed |
| `HostEditor(hostId?)` | full screen | dialog scene, max 560dp |

Back stack rules: switching session replaces the `Terminal` key (never stacks terminals); an alert intent builds `[Hosts, Terminal]`; after process death a restored `Terminal` with no connection reconnects, or pops to `Hosts` if the host is gone.

### Host list (`Hosts`)

- `LargeFlexibleTopAppBar` ("Rex", emphasized) with `exitUntilCollapsedScrollBehavior`.
- **Bottom M3E `HorizontalFloatingToolbar`** (thumb reach, Orion `Root.kt`): Search · Active-only (icon toggle, filled when on) · Settings · overflow (Tailscale, Diagnostics). **Add host** `FloatingActionButton` beside it. The plated pill is gone; bottom placement stays.
- Each host is a section header: host mark with a status badge cut into its corner, name (`titleLargeEmphasized`), supporting line `user@address · status` coloured by state, and the **tailnet path line** (direct / peer relay / DERP, latency; tap re-probes). Tap header = connect / fold, as today; fold chevron kept.
- Sessions are `SegmentedListItem`s (20dp outer / 4dp inner, `SegmentedGap`): program tile, name, "3 tabs · claude working", trailing **+ new tab** (kept from `SessionHeader`).
- **Active tabs stay visible** under their session (blocked/working ones always); idle tabs fold behind "N more". Cached and "open to load" rows kept. Active-only filter applies to tabs as today. The open tab in two-pane is marked with `secondaryContainer`.
- Long-press host → `DropdownMenu`: Connect/Disconnect, New session, Edit, Delete. Long-press session → Rename, New tab, Kill. Destructive confirms are dialogs. Swipe a session left → Kill (confirm). Needs session-scoped core actions (see phase 3d).
- No pull to refresh (sessions are pushed by events).
- Medium width (600–840dp) uses an adaptive two-column grid of host sections.

### Terminal (`Terminal`)

- **Top bar** (56dp, tonal `surfaceContainer`, fixed height): back disk on phone (40dp `surfaceContainerHighest` in a 48dp target; none in two-pane) · title block (tab name `titleMediumEmphasized`; `session · ~/path` `bodySmall`) · holder chip (`primaryContainer` "Take over" / tonal "This phone") · overflow. Reconnecting shows as a `LoadingIndicator` + "Reconnecting" in the title block after the existing settle delay; no overlay on blips.
- **Pane strip** (new, bottom, above the key bar, 40dp fixed, always shown): tab chip (tab name + count; opens the tab menu: tabs with activity dots, panes with status words, New tab) · pane segments (one per pane; tap focuses; swipe on the pane still works) · Split right · Split below · show-keys (only while the key bar is away). On windows ≥600dp the tab chip becomes an inline scrollable tab row with a trailing +.
- **Overflow** (`DropdownMenu`, grouped, leading icons): Pane (Rename, Close) · Tab (Rename, Close) · Session (Sessions…, New session, Rename, Kill) · Paste · Send a file · Hide/Show keys · Settings.
- **States:** first connect = `ContainedLoadingIndicator` over the pane placeholder; failed = tonal error card in the pane area with Retry (filled) and Edit host (outlined); waiting for the pane keeps "ask again" after 12s; short failed-action notices become an M3 `SnackbarHost` (4s).
- **Upload** is a tonal card above the pane strip with `LinearWavyProgressIndicator` and its cancel / retry / dismiss / uncertain states.
- **Terminal paper** is fixed neutral (black in dark, white in light), never the tinted dynamic surface; it does not animate on theme change.

### Key bar

- An M3E floating toolbar look, centred, max 720dp, offset 0–8dp above the IME (on `WindowInsetsRulers.Ime`).
- Two groups: a **scrolling key group** (Esc, C-c with error tint while busy, Tab, Enter with long-press newline, Ctrl, Paste, Attach, `/`) and a **pinned group** that never scrolls (arrow stick, keyboard, hide ⌄). The stick's drag must never be read as a scroll.
- **Ctrl keeps `CtrlKey`** (Off / Once / Locked, hold-and-slide chord grid); only its look changes: shapes from `ToggleButtonDefaults`, Once = tonal, Locked = filled.
- Icon keys use `IconButtonDefaults.shapes()` press morph.

### Settings (`Settings`)

- Root: emphasized title; grouped list (24dp outer corners, 1dp gaps, 40dp coloured icon tiles), 16sp titles, 13sp summaries.
- **Appearance:** theme (Dynamic / seeds), pure black, app font (Google Sans Flex / system). Live preview pinned on top.
- **Terminal:** font family list with per-font preview and download state (`LinearWavyProgressIndicator`; replaces `FontSettingsDialog`), text size slider with live preview, key bar with keyboard.
- **Connection:** Tailscale (join/leave, login link opens automatically as today), alerts.
- **Updates:** channel (connected `ToggleButton` group), check now.
- **About:** version, source, licences and credits, diagnostics (with its recording state).
- Whole-row toggles with check-thumb switches; chevron rows show the current value in `primary`.

### Host editor (`HostEditor`)

- Outlined text fields; the connection type is **parsed from the address** (`HostAddressParser`) and shown as a read-only chip (LAN / Tailscale / SSH), with the address hint as today. Join Tailscale from here as today. 56dp filled Save.
- SSH password, key and passphrase are kept out of saved state (a retained holder, not `rememberSaveable`).

### Sheets and dialogs that stay

- One sheet wrapper (`ModalBottomSheet`, `sheetMaxWidth = 640dp`, `surfaceContainerLow`, title with a `MaterialShapes` badge, 56dp primary button): Sessions picker, Link actions, Update offer, Diagnostics.
- Dialogs ported onto a predictive-back dialog (port of Harmonic's `PredictiveBackDialog`): SSH host-key trust, Tailscale, update offer, confirms.

## Design system

- **Theme:** `MaterialExpressiveTheme(colorScheme, MotionScheme.expressive(), typography)`.
  - Dynamic colour by default on API 31+; 5 seed schemes (Rex green brand, blue, violet, amber, slate) via material-kolor; pure black toggle (Harmonic `PURE` values). Theme changes animate (250ms) for chrome only.
  - Semantic roles: `pageBackground`, `cardBackground`, `menuBackground`, plus `ok` / `attention` harmonised with material-kolor; `error` from the scheme.
  - Migration: `Colors.*` getters first return scheme roles (so every screen turns Material at once), then each screen moves to roles directly as it is rebuilt, then `Colors` is deleted.
- **Type:** M3 scale with emphasized roles; Google Sans Flex via downloadable fonts, falling back silently to the system font without Play services.
- **Shapes:** segmented 20/4dp, settings groups 24dp with 1dp gaps, cards and sheets 28dp, menus 20dp, chips full; `MaterialShapes` for empty states, badges, sheet titles.
- **Icons:** Material Symbols Rounded vectors, outlined with filled variants for on states, replacing the `ic_*` drawables.
- **Motion:** spatial specs for size/position/shape, effects specs for colour/alpha, from `motionScheme`. Activity-style push/pop (96dp slide, 450ms emphasized, 83ms fades) and predictive back on every destination and dialog. `animateItem()`, staggered reveal for the first 9 rows once, `AnimatedContent` caption swaps (only outside the pane's measured height). Shared press modifier (scale 0.96, `fastSpatialSpec`, bounded ripple) on non-button surfaces.
- **Loading/progress:** `LoadingIndicator`, `ContainedLoadingIndicator`, `LinearWavyProgressIndicator`, skeleton rows on first load.
- **Haptics (owner rule):**
  - `SEGMENT_FREQUENT_TICK`: arrow stick steps and the Ctrl chord grid (kept from today).
  - `TOGGLE_ON` / `TOGGLE_OFF`: Ctrl latch, switches, active-only filter, segmented choices.
  - `CONFIRM`: only a connect the person started, and take-over.
  - Nothing on auto-reconnect, background failures, taps or swipes (swipe arming gets a `SEGMENT_TICK` only).

## Platform rules (official skills)

- **Navigation 3**, `ListDetailSceneStrategy` from `currentWindowAdaptiveInfoV2()` with `horizontalPartitionSpacerSize = 0.dp`. Two panes only at ≥840dp and non-compact height (a 45-column terminal is worse than one pane).
- **No `configChanges`**, but only after the terminal survives recreation (phase 3b): `TerminalSession` cached in the ViewModel by block id and rebound to a new view; `mods` and arrow-pad pin in `rememberSaveable`; foreground state from `ProcessLifecycleOwner` so recreation never resets the tailnet; `offeredOnce` saveable; every `remember` in `ui/` audited.
- **Resize discipline:** `TerminalView` sends a resize to Rex only after its size has been still for ~150ms; no chrome animates the pane's height.
- **Predictive back:** `android:enableOnBackInvokedCallback="true"` (targetSdk stays 35); `BackHandler`s in `TermScreen` and `HomeScreen` move to `NavigationBackHandler`.
- **Edge-to-edge:** screens pad system bars + cutout; the terminal never pads `safeDrawing` (it includes the IME); the key bar rides the IME ruler.
- **Keyboard and pointer:** keep the `Configuration` check for the key bar (a folded-back folio keyboard must not hide it). MediaQuery only if it earns its keep, behind `ComposeUiFlags.isMediaQueryIntegrationEnabled` in a new `Application`.
- **Focus:** with a hardware keyboard, focus returns to the pane after any menu, sheet or dialog closes, and when the terminal entry resumes.
- **Desktop:** existing shortcuts plus Ctrl+, (Settings) and Ctrl+Shift+K (session picker; Ctrl+K stays readline's); Hosts-screen shortcuts via `onPreviewKeyEvent`; shortcut helper list updated.
- **Performance:** `imeHeight()` read inside the `offset {}` lambda, not in composition; chrome takes narrow state, not all of `PocketState`; terminal view keyed on the pane only.
- **Screenshot tests:** Compose Preview Screenshot Testing with the skill's sizes (400/610/900 × 400/500/1000dp) and a fake pane (libghostty is JNI and does not render in Layoutlib).
- **Tabletop posture** (foldables, adaptive step 8): terminal on the top half, key bar and pane strip on the bottom. Last, if time allows.

## Build order

Each phase builds, passes tests, installs as a release build, and is walked on the Pixel and the scrcpy desktop display before the next.

1. **Toolchain.** AGP 9.x (agp-9-upgrade skill; built-in Kotlin replaces `org.jetbrains.kotlin.android`), Compose BOM 2026.09.00, material3 1.5.0-rc01, adaptive + adaptive-navigation3 1.3.0, navigation3 1.2.0, activity 1.13, lifecycle 2.10, graphics-shapes, google-fonts, kotlinx-serialization, material-kolor. Screenshot test plugin and form-factor previews of today's screens as the baseline.
2. **Theme.** `MaterialExpressiveTheme`, schemes, pure black, roles, type, Symbols. `Colors.*` backed by roles; the app turns Material without layout moves.
3. **Navigation and the terminal's lifetime.**
   - 3a: Nav3 back stack in the ViewModel and scenes, `configChanges` still on.
   - 3b: `TerminalSession` cache, resize debounce, `ProcessLifecycleOwner` foreground, saveable audit, focus return.
   - 3c: remove `configChanges`; test rotate, fold, Bluetooth keyboard, desktop resize with `htop` running, counting resizes and snapshot bytes in Diagnostics.
   - 3d: core session-scoped actions (rename / kill / new tab by session id, without switching the active session).
4. **Host list**, per the screen map.
5. **Terminal chrome:** top bar, pane strip, overflow, states, snackbar, upload card, key bar.
6. **Settings** with categories and previews; **host editor**.
7. **Sheets and dialogs** on the shared wrapper and predictive-back dialog.
8. **Polish:** screenshot tests for every new screen and state, compose-performance pass, tabletop, docs.

## Review checklist (each phase)

- Phone at 360dp with text 1.0 and 1.3; the Pixel as the owner has it (1.15, bold text) without changing its settings.
- scrcpy desktop display: two panes, divider drag, hover tooltips, pointers, shortcuts, resize across 600/840dp, no state loss.
- Light, dark, pure black, dynamic on and off.
- Every state: no hosts, connecting, reconnect blip, failed, empty session, many sessions and long names, upload running, update available.
- IME up/down: the pane slides, no resize; hardware keyboard: no key bar, typing works after every menu closes.
- Count Rex resizes during each transition (should be 0, or 1 on a real size change).
- Grep: no hex colours or sp literals in screens.

## Gotchas

- Nav3 disposes entries that leave the visible scene; anything expensive about a pane lives in the ViewModel cache, not in composition.
- Recreation was hiding three problems: the emulator in `remember`, foreground toggling resetting the tailnet, and `remember`-only flags. Fix all three before dropping `configChanges`.
- The terminal palette belongs to the program; only paper, ink, cursor and selection come from the app, and paper is fixed neutral.
- Downloadable fonts need Play services; fall back silently.
- AGP 9 removes the `kotlin-android` plugin and changes some DSL; the native `buildGhostty` tasks must keep running before `preBuild`.

## Key files

| File | Role after the redesign |
|---|---|
| `ui/Theme.kt` | `RexTheme` on `MaterialExpressiveTheme`, schemes, roles, type |
| `ui/RexApp.kt` | `NavDisplay`, scene strategies, top-level dialogs |
| `ui/RexViewModel.kt` | back stack, `TerminalSession` cache, session-scoped actions |
| `ui/HomeScreen.kt` | `Hosts` destination |
| `ui/TermScreen.kt` | `Terminal` chrome: top bar, pane strip, states |
| `ui/InputBar.kt` | key bar groups |
| `ui/Settings*.kt` | settings shell and categories (new) |
| `ui/Sheets.kt` | sheet wrapper, menus, predictive-back dialog |
| `ui/Motion.kt` | press modifier, reveal, nav transitions (new) |
| `terminal/TerminalView.kt` | resize debounce, rebinding to a cached session |
| `ui/Adaptive.kt` | removed once scenes replace it |
