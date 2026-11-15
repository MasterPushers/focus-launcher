# Tier 3 · Issue triage: which GitHub issues are bugs, and where in the code

Snapshot of 2026-11-15: 29 issues (26 open, 3 closed), read in full with their comments, and each
claimed bug checked against `main` at `ebcea98`. **Nothing here was reproduced on a device**; "confirmed in
code" means the code does what the report describes. Re-check before acting: issues move.

## Bug reports, strongest first
| # | Report | Verdict | Where / cause |
| --- | --- | --- | --- |
| 30 (comment) | Note editor loses the text on a tap outside the dialog or Back | **Confirmed in code; data loss** | `HomeScreen.kt` `TextInputDialog(title = "Note")` → `FocusDialog` uses Compose `Dialog` with default `DialogProperties` (outside tap and Back dismiss = discard). The dialog also opens with the whole note selected (`Dialogs.kt` `TextRange(0, initial.length)`), so the first key typed replaces it. The issue itself asks for autosave; a commenter proposed "save changes?" on dismiss |
| 53 | Typo-tolerant matches stop "open when one app is left" | **Confirmed in code** | `DrawerScreen.kt`: auto-launch fires only when `results.size == 1`, but `searchApps` also returns initials (rank 3) and in-order subsequence (rank 4, 3+ letters) matches. Fix direction: launch when exactly one app matches at the best rank (0–2) |
| 21 | Media prev / play-pause / next do nothing in some players (title is shown) | **Likely, not reproduced** | `HomeWidgets.kt` `MusicSection`: with notification access it always calls `transportControls.skipToNext/play/pause`, never checks `PlaybackState.actions`, never falls back to `controller.dispatchMediaButtonEvent`. Players that only handle media-button events or only `ACTION_PLAY_PAUSE` ignore it |
| 29 | Home button sometimes does nothing (Android 15, one OEM) | **Plausible, cause not in code** | Manifest is a normal `singleTask` HOME activity; likely low-memory kills on aggressive OEMs. Open PR #25 adds a "Keep Focus running" Setup row (battery-optimisation exemption) for exactly this; it has merge conflicts with `main` |
| 51 | Time in a browser Custom Tab counts for the browser, not the app that opened it | **Confirmed by design, a limitation** | `UsageRepository.kt` attributes `ACTIVITY_RESUMED` by `packageName`. Task-root attribution is a system API. Possible heuristic, not built: `UsageEvents.Event.getClassName()` is public, so Custom Tab activities could be credited to the app before them. The reporter has a filtering PR on their fork |
| 32 (comment) | "No search app found" on a phone without Google and Firefox | Unclear | `Launching.kt` `openWebSearch` tries global search, web search, then `VIEW https://www.google.com/`; failing all four means no browser handled even the last. Not reproduced |
| 14 | Pinned shortcuts from apps go nowhere; no long-press app shortcuts | Missing feature, not a defect | Manifest has no `CONFIRM_PIN_SHORTCUT` handler and the app menu never reads `LauncherApps.getShortcuts`. Apps that skip `isRequestPinShortcutSupported()` claim success anyway |
| 17 | Tapping the calendar opens the "today" view, which changes some calendar apps' saved view | Side effect, filed as a request | `HomeScreen.kt` `openCalendarApp` opens `content://com.android.calendar/time/<now>` |
| 33 | Keyboard does not focus in Firefox's search after the swipe | Third-party behaviour | Depends on how the search app handles the intent from `openWebSearch` |
| 27 (closed) | Payment / UPI apps refuse to run while Focus's accessibility service is on | Not fixable in Focus | Those apps refuse any enabled accessibility service. Must not be "fixed" by removing the service (owner's rule, Tier 1 #11) |

Everything else (14 more open issues and the wishlist #31) is feature requests or project posts.
The wishlist's comments hold requests only (translations, fast-app presets, unified search,
landscape, follow-system theme, built-in app lock, corner-shortcut menu, another store).

## How this was done
GitHub MCP `list_issues` (all states, bodies) and `issue_read get_comments` per issue; the code
read with grep and sed. No issue was commented on or relabelled.
