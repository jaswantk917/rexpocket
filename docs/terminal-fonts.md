# Terminal fonts

Open the home screen's Settings → Terminal font to choose system monospace or a downloadable family. The default remains system monospace. Only terminal output changes; the rest of the app uses its existing UI fonts.

| Family | Download | Licence (in `app/src/main/assets/licenses/`) |
|---|---|---|
| JetBrains Mono | 1.1 MB | `JetBrainsMono-OFL.txt` |
| Meslo LG NF (Menlo's open-source twin, Nerd Font icons, powerlevel10k's build) | 10 MB | `MesloLGS-Apache.txt` |
| Hack | 1.2 MB | `Hack-MIT.txt` |
| IBM Plex Mono | 0.7 MB | `IBMPlexMono-OFL.txt` |
| Source Code Pro | 0.75 MB | `SourceCodePro-OFL.txt` |
| Ubuntu Mono | 0.8 MB | `UbuntuMono-UFL.txt` |

Selecting a family downloads its regular, bold, italic and bold italic faces once. Downloads come directly from the family's own repository on GitHub at a pinned commit, with a SHA-256 check for each file (`TERMINAL_FAMILIES` in `data/TerminalFamilies.kt`). No font binaries are bundled in the APK. Each family saves into its own directory; JetBrains Mono keeps `jetbrains-mono-v1`, the directory of the single-font version, and the old `downloaded_font` preference is read as JetBrains Mono.

Menlo itself is Apple's and cannot be redistributed; Meslo is its Apache-licensed derivative. Fira Code no longer keeps TTFs in its repository, so it is not offered.

The complete verified family is saved in private application files and the selection in preferences. After downloading, it works offline, including after restarting the app or switching to system monospace and back. A failed or incomplete download keeps the previous selection; tap the family again to retry. The picker previews every family already on the phone in its own face. Network access to GitHub is needed for the first download.

On Android 10+, a custom font family supplies all four styles with the same ASCII advance (0.6 em). Android 8–9 uses the downloaded regular face with synthetic styles. None of the families include Chinese glyphs; CJK continues to use Android font fallback.

## Why offer a separate monospace font?

RexPocket's terminal view measures `M` in the chosen typeface for the cell width and draws each cell at `column * cellWidth`; the default is `Typeface.MONOSPACE`. A device font substitution that resolves to a proportional face can therefore produce large gaps after narrow glyphs, even though ordinary UI text looks normal.

Issue #3 reports this on a Samsung Galaxy S10 running Android 12. The reporter confirmed that the first test build, with bundled JetBrains Mono, fixed the spacing. The actual runtime system font mapping has not been captured, so the OEM substitution mechanism remains a hypothesis rather than a verified device-wide diagnosis. The downloadable font provides the same independent fixed-width font choice without including its binaries in each APK.

The code: `TerminalView.measureFont` (cell measurement) and `TerminalView.drawText` (cell drawing) in `app/src/main/java/me/leaver/rexpocket/terminal/`.

## Validation

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The download store's tests cover verified offline reuse, checksum rejection, interrupted-download retry, corruption repair, abandoned partial downloads, and missing family members.

Device regression checks for the settings version:

1. On the S10, compare the same terminal pane at the same font size before and after selecting JetBrains Mono. Check `Explored`, `ManagedActionBase`, Chinese, regular/bold/italic styles, selection alignment, wrapping, resizing and reconnecting.
2. Restart offline, then switch to system monospace and back. The downloaded family should remain usable without a request.
3. Start a first download without network access. It should report failure without changing the active font; reconnect and retry.

The reporter has verified the initial bundled-font workaround. These checks for the subsequent settings/download UI still need device validation.
