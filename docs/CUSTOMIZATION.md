# Optional home-screen customization

These additions preserve the existing defaults. They do not add icons, widgets,
accounts, network access, or automatic limits for communication apps.

## Swipes

Settings → Gestures assigns each direction independently:

| Action | Behavior |
| --- | --- |
| App drawer | Open the app list, respecting the automatic-keyboard preference. |
| App search | Open the app list and request search focus. |
| Web search | Use the existing web-search intent fallbacks. |
| Notifications | Use the existing accessibility/status-bar implementation. |
| Focus settings | Open the launcher's settings. |
| Lock the screen | Use the existing timer accessibility service; explain if it is off. |
| An installed app | Use the normal launch gate, including timers and bypass consent. |
| Nothing | Do not run a swipe action. |

Directions refer to finger movement. Defaults remain left → drawer, right → web
search, up → app search, down → notifications. A Restore default swipes row
remains reachable through a long press on empty home space, including when
every swipe is disabled.

The new action strings are stored in the existing settings JSON. Loading older
settings carries over disabled toggles and the app formerly assigned to the
right swipe; new action keys take precedence when both formats are present.

The drawer uses a vertical or horizontal pager according to the swipe:
up enters from below; down from above; left from the right; right from the left.
Back and Home return along that axis. The default left-swipe drawer retains its
finger-following horizontal drag. Irrelevant default gesture tips are skipped
after an action has been reassigned.

## Fast apps

Settings → Home screen adds:

- **Where the apps sit:** High / Middle / Low. Middle keeps the original spacer
  weights; High and Low redistribute the available empty height.
- **Spacing between apps:** 0, 4, 8, 12, 16, or 24 dp of extra row spacing.
  The default is zero. Space is added between apps, not before the first row.

The existing fit calculation includes the extra row spacing before choosing its
font/padding/calendar-detail fallback. Reordering and normal app-menu/launch
behavior remain in place. No new font sizes or larger app-count limit are added.

## Wallpaper

Settings → Appearance can show the phone's current system wallpaper with a
light, medium, or heavy theme-colored veil. Wallpaper is off by default; its
default veil is Medium (55%). The drawer and all settings screens remain opaque.

This uses `FLAG_SHOW_WALLPAPER`, not pixel access through
`WallpaperManager.getDrawable`. The window explicitly requests a translucent
surface and reapplies its background/format/flag on resume. A transparent
drawable alone did not reliably preserve the wallpaper on a reported device.
No INTERNET, broad-storage, or wallpaper-reading permission is introduced.
Wallpaper blur and custom-image import are not included.

## Verification

- Unit coverage: settings defaults, legacy gesture migration, action precedence,
  JSON round-trip, bounded layout/veil values, and drawer-origin axis/direction.
- Emulator regressions: swipe up opens the actual vertical pager with the
  sideways swipe disabled; returning from another app restores a deliberately
  opaque wallpaper surface without a press or hold.
- Each new emulator regression was seen failing on a deliberate source break
  and passing after the production fix was restored.
- Local Android 16/API 36 runs passed all nine journeys. They cover search,
  default Home behavior, app classification, launch-time and mid-session timers,
  and the new drawer/wallpaper regressions.

Not yet verified: Android 8.0, every gesture/action combination, repeated
long-session stability, or a physical-device retest of the reported wallpaper
issue. The wallpaper regression simulates the opaque-surface condition; it does
not claim to reproduce every OEM's compositor behavior.
